#!/usr/bin/env python3
"""公共 Mock HTTP Server — 所有需要模拟下游服务的脚本共享

用法:
    from mock_server import MockServer

    # 创建 mock server
    server = MockServer(port=18999)

    @server.handler("GET", "/api/health")
    def health():
        return 200, {"status": "ok"}

    @server.handler("POST", "/api/search")
    def search(body):
        return 200, {"code": 0, "data": {"keyword": body.get("keyword", "")}}

    # 启动并等待就绪
    server.start()
    
    # ... 测试代码 ...
    
    # 关闭
    server.stop()

设计原则:
- 一行代码启动 mock server，无需关心线程/健康检查等细节
- handler 返回 (status_code, body_dict) 即可
- 自动处理 JSON 序列化
"""
import json
import threading
import time
import urllib.request
import urllib.parse
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler


class MockServer:
    """轻量级 Mock HTTP Server，用于模拟下游服务"""

    def __init__(self, port=18999, host="localhost"):
        self.host = host
        self.port = port
        self._handlers = {}  # {(method, path_prefix): handler_fn}
        self._server = None
        self._thread = None

    def handler(self, method, path):
        """装饰器：注册一个 handler 函数

        handler 签名为: fn(body_dict=None, path=None, headers=None) -> (status_code, response_dict)
        """
        def decorator(fn):
            self._handlers[(method.upper(), path)] = fn
            return fn
        return decorator

    @property
    def base_url(self):
        return f"http://{self.host}:{self.port}"

    def _make_handler_class(self):
        handlers = self._handlers
        port = self.port

        class _Handler(BaseHTTPRequestHandler):
            def log_message(self, format, *args):
                pass  # 抑制访问日志

            def _send_json(self, status_code, body):
                self.send_response(status_code)
                self.send_header("Content-Type", "application/json")
                self.end_headers()
                self.wfile.write(json.dumps(body).encode("utf-8"))

            def _find_handler(self, method):
                parsed = urllib.parse.urlparse(self.path)
                path = parsed.path
                # 精确匹配优先，再前缀匹配
                for (m, p), fn in handlers.items():
                    if m == method and p == path:
                        return fn
                for (m, p), fn in handlers.items():
                    if m == method and path.startswith(p):
                        return fn
                return None

            def _parse_body(self):
                content_len = int(self.headers.get("Content-Length", 0))
                if content_len > 0:
                    raw = self.rfile.read(content_len)
                    try:
                        return json.loads(raw.decode("utf-8"))
                    except Exception:
                        return {}
                return {}

            def do_GET(self):
                fn = self._find_handler("GET")
                if fn:
                    try:
                        status, body = fn()
                    except TypeError:
                        status, body = fn(body=None, path=self.path, headers=dict(self.headers))
                    self._send_json(status, body)
                else:
                    self._send_json(404, {"error": "not_found"})

            def do_POST(self):
                fn = self._find_handler("POST")
                if fn:
                    body_data = self._parse_body()
                    try:
                        status, body = fn(body_data)
                    except TypeError:
                        status, body = fn(body=body_data, path=self.path, headers=dict(self.headers))
                    self._send_json(status, body)
                else:
                    self._send_json(404, {"error": "not_found"})

        return _Handler

    def start(self, wait=True, timeout=10):
        """启动 mock server 并等待就绪"""
        handler_cls = self._make_handler_class()
        self._server = ThreadingHTTPServer((self.host, self.port), handler_cls)
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._thread.start()

        if wait:
            for _ in range(timeout * 2):
                try:
                    resp = urllib.request.urlopen(
                        f"{self.base_url}/api/health", timeout=1
                    )
                    if resp.status == 200:
                        return True
                except Exception:
                    pass
                time.sleep(0.5)
            print(f"WARNING: Mock server on port {self.port} did not become ready")
            return False
        return True

    def stop(self):
        """关闭 mock server"""
        if self._server:
            self._server.shutdown()
        print(f"Mock server on port {self.port} shut down.")
