# open-server 集成测试

基于 Python 的端到端集成测试，覆盖连接器平台 V3 全部 API。

## 目录

```
python/
├── README.md              ← 本文件
└── inspect/
    ├── client.py          ← 统一入口（api / db / redis / ok / done）
    ├── all.py             ← 全量回归执行器
    ├── connector_create.py       # IT-001~004  创建连接器
    ├── connector_delete.py       # IT-005      删除连接器
    ├── connector_crud.py         # IT-006~007  连接器 CRUD
    ├── connector_detail.py       # IT-008      连接器详情
    ├── connector_update.py       # IT-009      更新连接器
    ├── connector_recover.py      # IT-012      恢复连接器
    ├── connector_version_lifecycle.py  # IT-108~115  版本全生命周期
    ├── connector_list.py         # IT-????     连接器列表
    ├── flow_create.py            # IT-???      创建连接流
    ├── flow_delete.py            # IT-???      删除连接流
    ├── flow_update.py            # IT-???      更新连接流
    ├── flow_detail.py            # IT-???      连接流详情
    ├── flow_list.py              # IT-???      连接流列表
    ├── flow_copy.py              # IT-???      复制连接流
    ├── flow_start.py             # IT-???      启动连接流
    ├── flow_stop.py              # IT-???      停止连接流
    ├── flow_stop_restart.py      # IT-???      停止并重启
    ├── flow_recover.py           # IT-???      恢复连接流
    ├── flow_deploy_start_invoke.py     # IT-???  部署并启动调用
    ├── flow_version_lifecycle.py       # IT-???  流版本生命周期
    ├── flow_approval_full_flow.py      # IT-???  审批全流程
    ├── api_delete.py             # IT-???      删除 API
    ├── event_delete.py           # IT-???      删除事件
    ├── callback_delete.py        # IT-???      删除回调
    ├── app_whitelist.py          # IT-???      应用白名单
    ├── operation_log.py          # IT-???      操作日志
    ├── approval_engine_callback.py     # IT-???  审批引擎回调
    ├── json_validation.py        # IT-???      JSON 校验
    └── flow_version_debug.py             # IT-047~048  调试运行
```

## 用法

```bash
# 全量回归
cd open-server/src/test/python
python3 inspect/all.py              # 详细输出
python3 inspect/all.py --quiet      # 只输出摘要

# 单独运行某个脚本
python3 inspect/connector_create.py
python3 inspect/flow_version_lifecycle.py
```

## 编写测试

`client.py` 提供统一入口，测试脚本只传业务参数：

```python
from client import api, db, redis, ok, fail, done

# HTTP 请求 — URL/server/auth 全部自动
resp = api("POST", "/connectors", {"nameCn": "测试", "nameEn": "Test", "connectorType": 1})
ok(resp, 200, "创建连接器")

# 覆盖默认值
resp = api("GET", "/connectors", app_id="99999")     # 换应用
resp = api("POST", "/connectors", body, user="tester")  # 换用户

# 数据库
db("INSERT INTO t VALUES (1, 'hello')")
val = db_val("SELECT COUNT(*) FROM t")

# Redis
redis("SET", "key", "value")
redis("KEYS", "connector:*")

# 收尾
done()   # 打印 PASS/FAIL 汇总，有失败则 exit 1
```

## 配置

所有基础设施配置集中在 `client.py` 顶部，一个地方改：

```python
_API_BASE       = "http://localhost:18080/open-server"
_DEFAULT_APP_ID = "202606241730488926"
_DEFAULT_USER   = "admin"
_DB  = {"host": "192.168.3.155", "user": "openapp", "passwd": "openapp", "db": "openapp"}
_REDIS = {"host": "192.168.3.201", "port": 6379, "password": "openapp"}
```
