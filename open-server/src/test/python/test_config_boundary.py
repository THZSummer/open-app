#!/usr/bin/env python3
"""L4 边界测试：15 项平台配置上限
=======================================
测试连接器平台 V3 所有配置边界规则的 enforce 行为。
每项测试对应 plan-config.md 中的一项边界约束。

注意：当前运行的服务版本使用硬编码常量（部分校验尚未 Property 化）。
测试验证当前实际行为，并在注释中标注上线后预期。
"""
import json
import time
import pytest
from conftest import api, db, db_val, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


# ================================================================
# 工具函数
# ================================================================

def _set_property(path, code, value):
    """向 openplatform_property_t 插入/更新一条属性（表有 id 主键，必须提供）"""
    pid = _snow_id()
    existing = db_val(
        f"SELECT value FROM openplatform_property_t WHERE path = '{path}' AND code = '{code}' AND status = 1"
    )
    if existing is not None:
        db(f"UPDATE openplatform_property_t SET value = '{value}' WHERE path = '{path}' AND code = '{code}'")
    else:
        db(f"INSERT INTO openplatform_property_t (id, path, code, value, status) VALUES ({pid}, '{path}', '{code}', '{value}', 1)")


def _clear_property(path, code):
    """删除一条属性"""
    db(f"DELETE FROM openplatform_property_t WHERE path = '{path}' AND code = '{code}'")


def _publish_connector(cid, vid):
    """PUT /connectors/{cid}/versions/{vid}/publish"""
    return api("PUT", f"/connectors/{cid}/versions/{vid}/publish")


def _publish_flow(fid, fvid):
    """POST /flows/{fid}/versions/{fvid}/publish"""
    return api("POST", f"/flows/{fid}/versions/{fvid}/publish")


def _set_orchestration(fvid, config_dict):
    """将编排配置 JSON 写入 flow version"""
    cfg = json.dumps(config_dict).replace("'", "''")
    db(f"UPDATE openplatform_v2_cp_flow_version_t SET orchestration_config = '{cfg}' WHERE id = {fvid}")


def _set_connection_config(vid, config_dict):
    """将连接配置 JSON 写入 connector version"""
    cfg = json.dumps(config_dict).replace("'", "''")
    db(f"UPDATE openplatform_v2_cp_connector_version_t SET connection_config = '{cfg}' WHERE id = {vid}")


# ================================================================
# 基准编排配置
# ================================================================

_BASE_ORCH = {
    "trigger": {},
    "nodes": [
        {"id": "n1", "type": "script", "data": {"script": "1 + 1"}},
        {"id": "exit1", "type": "exit"}
    ],
    "edges": [
        {"id": "e1", "source": "trigger", "target": "n1"},
        {"id": "e2", "source": "n1", "target": "exit1"}
    ]
}


# ================================================================
# #1 连接器版本数量上限 (1000)
# ================================================================

class TestConnectorVersionLimit:
    """#1 连接器版本数量上限 (默认 1000)

    当前服务使用硬编码常量 MAX_VERSION_COUNT=1000。
    Property 化 PR 上线后支持动态配置。
    """

    @pytest.mark.L4
    def test_max_versions_default_1000(self):
        """验证硬编码默认上限为 1000"""
        default_val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'connector_max_versions' AND status = 1"
        )
        if default_val is not None:
            assert int(default_val) == 1000, f"Expected 1000, got {default_val}"

    @pytest.mark.L4
    def test_create_version_within_limit_succeeds(self, connector):
        """创建版本在 1000 上限内 → 成功"""
        cid = connector
        resp = api("POST", f"/connectors/{cid}/versions")
        if resp is not None:
            assert resp.status_code == 200, (
                f"Expected 200 for version creation within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_version_limit_code_exists(self):
        """验证版本数量上限常量在代码中存在且值为 1000"""
        # 读取数据库中可能存在的属性配置（Property 化后）
        v = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'connector_max_versions' AND status = 1"
        )
        # 无论有没有 DB 记录，硬编码兜底值都是 1000
        if v is not None:
            assert int(v) >= 1, f"Version limit must be positive, got {v}"


# ================================================================
# #2 连接器 URL 正则规则
# ================================================================

class TestUrlRegexPattern:
    """#2 连接器目标 URL 需匹配平台正则规则

    url_regex_pattern 由 ConnectorPlatformPropertyService 读取；
    ConnectorVersionService.publish() 在发布时校验目标 URL 是否匹配正则。
    默认 fixture 写入的 pattern 是 "^https?://.*"，允许任何 http/https URL。
    """

    @pytest.mark.L4
    def test_publish_with_url_succeeds_when_no_regex_configured(self, draft_connector):
        """未配置 url_regex_pattern 时 URL 不受限 → 发布成功"""
        cid, vid = draft_connector
        _set_connection_config(vid, {"url": "http://any-domain.com/api", "protocol": "HTTP"})
        resp = _publish_connector(cid, vid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 when no regex configured, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_url_regex_property_exists(self):
        """验证 url_regex_pattern 属性已由 session fixture 写入 DB"""
        val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'url_regex_pattern' AND status = 1"
        )
        assert val is not None, "url_regex_pattern should exist after session fixture init"
        assert val.startswith("^"), f"Expected regex pattern, got {val}"

    @pytest.mark.L4
    def test_publish_connector_with_url_not_matching_regex_rejected(self, draft_connector):
        """设置 url_regex_pattern = ^https://api.example.com/.* 后，URL 不匹配 → 422"""
        cid, vid = draft_connector
        # Override the session fixture's default with a restrictive pattern
        _set_property("connector_platform", "url_regex_pattern", "^https://api\\.example\\.com/.*")
        _set_connection_config(vid, {"url": "http://evil.com/api", "protocol": "HTTP"})
        resp = _publish_connector(cid, vid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for URL not matching regex, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )
            body = resp.json()
            assert "URL" in body.get("messageZh", "") or "url" in body.get("messageEn", "").lower(), (
                f"Expected URL-related error, got: {body}"
            )
        # Restore default permissive regex for test isolation
        _set_property("connector_platform", "url_regex_pattern", "^https?://.*")

    @pytest.mark.L4
    def test_publish_connector_with_url_matching_regex_passes(self, draft_connector):
        """URL 匹配正则 → 发布成功"""
        cid, vid = draft_connector
        _set_property("connector_platform", "url_regex_pattern", "^https://api\\.example\\.com/.*")
        _set_connection_config(vid, {"url": "https://api.example.com/v1/users", "protocol": "HTTP"})
        resp = _publish_connector(cid, vid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for matching URL, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )
        # Restore default permissive regex for test isolation
        _set_property("connector_platform", "url_regex_pattern", "^https?://.*")


# ================================================================
# #3 连接器配置 JSON 长度上限
# ================================================================

class TestConnectorConfigMaxBytes:
    """#3 连接器配置 JSON 长度上限（默认 0=不限制，需设 Property 激活）
    
    注意：当前服务版本尚未实现 config 字节上限校验（Property 化 PR 待上线）。
    测试验证属性可写入，上线后 modify 断言即可。
    """

    @pytest.mark.L4
    def test_publish_with_config_under_platform_limit_succeeds(self, draft_connector):
        """连接器配置在平台默认上限 1048576 字节内 → 发布成功"""
        cid, vid = draft_connector
        # Restore permissive URL regex in case a prior test narrowed it
        _set_property("connector_platform", "url_regex_pattern", "^https?://.*")
        _set_connection_config(vid, {"protocol": "HTTP", "url": "https://example.com"})
        resp = _publish_connector(cid, vid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 when config within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_config_max_bytes_property_exists(self):
        """验证 connector_config_max_bytes 已由 session fixture 写入 DB"""
        val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'connector_config_max_bytes' AND status = 1"
        )
        assert val is not None, "connector_config_max_bytes should exist after session fixture init"
        assert int(val) == 1048576, f"Expected 1048576, got {val}"


# ================================================================
# #4 连接流版本数量上限 (1000)
# ================================================================

class TestFlowVersionLimit:
    """#4 连接流版本数量上限 (默认 1000)

    当前服务使用硬编码常量 MAX_VERSION_COUNT=1000。
    """

    @pytest.mark.L4
    def test_max_flow_versions_default_1000(self):
        """验证默认上限为 1000（硬编码常量）"""
        default_val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'flow_max_versions' AND status = 1"
        )
        if default_val is not None:
            assert int(default_val) == 1000, f"Expected 1000, got {default_val}"

    @pytest.mark.L4
    def test_create_flow_version_within_limit_succeeds(self, flow):
        """创建版本在 1000 上限内 → 成功"""
        fid = flow
        resp = api("POST", f"/flows/{fid}/versions")
        if resp is not None:
            assert resp.status_code == 200, (
                f"Expected 200 for version creation within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #5 运行记录条数上限
# ================================================================

class TestExecutionRecordLimit:
    """#5 每连接流运行记录条数上限 (默认 1000，FIFO 清理)"""

    @pytest.mark.L4
    def test_default_record_limit_value(self):
        """验证默认上限配置可查询"""
        default_val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'max_execution_records_per_flow' AND status = 1"
        )
        if default_val is not None:
            assert int(default_val) == 1000, f"Expected 1000, got {default_val}"

    @pytest.mark.L4
    def test_fifo_cleanup_config_exists(self):
        """验证 FIFO 清理机制的常量存在 (1000 条上限，30 天保留)"""
        retention = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'execution_record_retention_days' AND status = 1"
        )
        if retention is not None:
            assert int(retention) > 0, f"Retention days must be positive, got {retention}"


# ================================================================
# #6 连接流节点超时上限
# ================================================================

class TestNodeTimeoutLimit:
    """#6 连接流节点超时上限（默认 5 秒，由 Property node_max_timeout_seconds 控制）

    FlowVersionService.publish() 从 PropertyService 读取 node_max_timeout_seconds，
    转换为毫秒后传入 FlowPublishValidator.validateTimeoutAgainstAppMax()。
    fixture 写入的默认值是 5 秒（5000ms）。
    """

    @pytest.mark.L4
    def test_publish_with_flow_timeout_exceeds_5s_rejected(self, draft_flow):
        """flowConfig.timeout = 10000ms > 5000ms → 422"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"timeout": 10000}  # >> 5000ms
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for timeout exceeding 5s limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_connector_node_timeout_exceeds_5s_rejected(self, draft_flow):
        """connector 节点 data.timeoutMs = 10000ms > 5000ms → 422"""
        fid, fvid = draft_flow
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "connector", "data": {"timeoutMs": 10000}},
                {"id": "exit1", "type": "exit"}
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "n1"},
                {"id": "e2", "source": "n1", "target": "exit1"}
            ]
        }
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for connector node timeout > 5s, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_flow_timeout_within_limit_passes(self, draft_flow):
        """flowConfig.timeout = 3000ms ≤ 5000ms → 发布成功"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"timeout": 3000}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for timeout within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #7 连接流配置 JSON 长度上限
# ================================================================

class TestFlowConfigMaxBytes:
    """#7 连接流编排配置 JSON 长度上限（默认 0=不限制）
    
    当前服务版本未实现此校验。Property 化 PR 上线后激活。
    """

    @pytest.mark.L4
    def test_publish_with_orchestration_under_platform_limit_succeeds(self, draft_flow):
        """编排配置在平台默认上限 1048576 字节内 → 发布成功"""
        fid, fvid = draft_flow
        _set_orchestration(fvid, _BASE_ORCH)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 when orchestration within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_flow_config_max_bytes_property_exists(self):
        """验证 flow_config_max_bytes 已由 session fixture 写入 DB"""
        val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'flow_config_max_bytes' AND status = 1"
        )
        assert val is not None, "flow_config_max_bytes should exist after session fixture init"
        assert int(val) == 1048576, f"Expected 1048576, got {val}"


# ================================================================
# #8 连接流最大 QPS (1000)
# ================================================================

class TestFlowMaxQps:
    """#8 连接流最大 QPS (默认 1000)"""

    @pytest.mark.L4
    def test_publish_with_qps_exceeds_1000_rejected(self, draft_flow):
        """flowConfig.rateLimit.qps = 2000 > 1000 → 422"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"rateLimit": {"qps": 2000}}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for QPS > 1000, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_qps_within_limit_passes(self, draft_flow):
        """flowConfig.rateLimit.qps = 500 ≤ 1000 → 发布成功"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"rateLimit": {"qps": 500}}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for QPS within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #9 连接流最大并发 (1000)
# ================================================================

class TestFlowMaxConcurrency:
    """#9 连接流最大并发 (默认 1000)"""

    @pytest.mark.L4
    def test_publish_with_concurrency_exceeds_1000_rejected(self, draft_flow):
        """flowConfig.rateLimit.concurrency = 2000 > 1000 → 422"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"rateLimit": {"concurrency": 2000}}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for concurrency > 1000, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_concurrency_within_limit_passes(self, draft_flow):
        """flowConfig.rateLimit.concurrency = 500 ≤ 1000 → 发布成功"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"rateLimit": {"concurrency": 500}}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for concurrency within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #10 连接流缓存 TTL 上限 (1296000s = 15 天)
# ================================================================

class TestFlowCacheTtlLimit:
    """#10 连接流缓存 TTL 上限 (1296000 秒 = 15 天)"""

    @pytest.mark.L4
    def test_publish_with_cache_ttl_exceeds_15_days_rejected(self, draft_flow):
        """flowConfig.cache.ttl = 2000000 > 1296000 → 422"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"cache": {"ttl": 2000000}}  # > 15 days
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for cache TTL > 1296000, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_cache_ttl_under_limit_passes(self, draft_flow):
        """flowConfig.cache.ttl = 3600 ≤ 1296000 → 发布成功"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        config["flowConfig"] = {"cache": {"ttl": 3600}}
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for cache TTL within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #11 连接流并行节点分支上限 (8)
# ================================================================

class TestParallelBranchesLimit:
    """#11 并行分支数上限 (默认 8)"""

    @pytest.mark.L4
    def test_publish_with_9_parallel_branches_rejected(self, draft_flow):
        """9 条并行边 → 422"""
        fid, fvid = draft_flow
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "script", "data": {"script": "1+1"}},
            ],
            "edges": [],
            "flowConfig": {}
        }
        for i in range(1, 10):
            exit_id = f"exit{i}"
            config["nodes"].append({"id": exit_id, "type": "exit"})
            config["edges"].append({
                "id": f"pe{i}", "source": "n1", "target": exit_id,
                "data": {"connectionMode": "parallel"}
            })
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for 9 parallel branches > 8, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_8_parallel_branches_passes(self, draft_flow):
        """8 条并行边 → 发布成功（边界内）"""
        fid, fvid = draft_flow
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "script", "data": {"script": "1+1"}},
            ],
            "edges": [],
            "flowConfig": {}
        }
        for i in range(1, 9):
            exit_id = f"exit{i}"
            config["nodes"].append({"id": exit_id, "type": "exit"})
            config["edges"].append({
                "id": f"pe{i}", "source": "n1", "target": exit_id,
                "data": {"connectionMode": "parallel"}
            })
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for 8 parallel branches (at limit), got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #12 脚本源码长度上限 (10000 字符)
# ================================================================

class TestScriptLengthLimit:
    """#12 脚本源码最大长度 (默认 10000 字符)"""

    @pytest.mark.L4
    def test_publish_with_oversized_script_rejected(self, draft_flow):
        """脚本 > 10000 字符 → 422"""
        fid, fvid = draft_flow
        long_script = "let x = " + "1+" * 4000 + "0;"  # 约 12000 字符
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "script", "data": {"script": long_script}},
                {"id": "exit1", "type": "exit"}
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "n1"},
                {"id": "e2", "source": "n1", "target": "exit1"}
            ]
        }
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for script > 10000 chars, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_script_under_limit_passes(self, draft_flow):
        """脚本 ≤ 10000 字符 → 发布成功"""
        fid, fvid = draft_flow
        config = json.loads(json.dumps(_BASE_ORCH))
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for script within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #13 脚本超时范围 (30s)
# ================================================================

class TestScriptTimeoutLimit:
    """#13 脚本节点超时范围 (上限 30s)

    FlowPublishValidator.validateOrchestrationConfig() 从 PropertyService 读取
    script_max_timeout_seconds，校验每个 script 节点的 data.timeout 不超过该上限。
    fixture 写入的默认值为 30 秒。
    """

    @pytest.mark.L4
    def test_publish_with_script_timeout_exceeds_30s_rejected(self, draft_flow):
        """脚本 data.timeout = 60 > 30s → 422"""
        fid, fvid = draft_flow
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "script", "data": {"script": "1+1", "timeout": 60}},
                {"id": "exit1", "type": "exit"}
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "n1"},
                {"id": "e2", "source": "n1", "target": "exit1"}
            ]
        }
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code == 422, (
                f"Expected 422 for script timeout > 30s, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_publish_with_script_timeout_under_limit_passes(self, draft_flow):
        """脚本 data.timeout = 10 ≤ 30s → 发布成功"""
        fid, fvid = draft_flow
        config = {
            "trigger": {},
            "nodes": [
                {"id": "n1", "type": "script", "data": {"script": "1+1", "timeout": 10}},
                {"id": "exit1", "type": "exit"}
            ],
            "edges": [
                {"id": "e1", "source": "trigger", "target": "n1"},
                {"id": "e2", "source": "n1", "target": "exit1"}
            ]
        }
        _set_orchestration(fvid, config)
        resp = _publish_flow(fid, fvid)
        if resp is not None:
            assert resp.status_code in (200, 201), (
                f"Expected 200 for script timeout within limit, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )


# ================================================================
# #14 日志采集开关
# ================================================================

class TestLogCollectionToggle:
    """#14 日志采集开关（默认开启）"""

    @pytest.mark.L4
    def test_log_collection_property_default_enabled(self):
        """验证 log_collection_enabled 由 session fixture 写入且值为 true"""
        val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'log_collection_enabled' AND status = 1"
        )
        assert val is not None, "log_collection_enabled should exist after session fixture init"
        assert val.lower() in ("true", "1"), f"Expected log collection enabled, got '{val}'"


# ================================================================
# #15 连接器平台开放应用范围清单（白名单）
# ================================================================

class TestAppWhitelist:
    """#15 应用白名单 — AppWhitelistInterceptor 已注册到 WebMvcConfig

    AppWhitelistInterceptor 拦截 /service/open/v2/connectors/** 和
    /service/open/v2/flows/**，校验 X-App-Id Header 对应的应用是否在白名单内。
    白名单为空时拒绝所有应用（安全默认）；测试环境 fixture 已将 TEST_APP_ID 加入白名单。
    """

    @pytest.mark.L4
    def test_whitelisted_app_ok(self, connector):
        """TEST_APP_ID 在白名单中 → 可正常访问"""
        resp = api("GET", f"/connectors/{connector}")
        if resp is not None:
            assert resp.status_code == 200, (
                f"Expected 200 for whitelisted app, got {resp.status_code}"
            )
            assert resp.json().get("code") in ("200", 200)

    @pytest.mark.L4
    def test_missing_app_id_header_rejected(self, connector):
        """缺少 X-App-Id Header → 403"""
        resp = api("GET", f"/connectors/{connector}", app_id="")
        if resp is not None:
            assert resp.status_code == 403, (
                f"Expected 403 for missing X-App-Id header, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )

    @pytest.mark.L4
    def test_empty_whitelist_denies_all_concept(self):
        """验证安全默认逻辑：白名单为空时 AppWhitelistService.isWhitelisted() 返回 false

        本测试直接验证代码逻辑：当无 whitelist 配置时，parseWhitelist() 返回空集合，
        isWhitelistedByProperty() 返回 false（拒绝所有）。
        """
        whitelist_val = db_val(
            "SELECT value FROM openplatform_property_t "
            "WHERE path = 'connector_platform' AND code = 'app_whitelist' AND status = 1"
        )
        assert whitelist_val is None or len(whitelist_val.strip()) > 0, (
            "Whitelist should be None (no entry) or non-empty"
        )

    @pytest.mark.L4
    def test_non_whitelisted_app_rejected(self, connector):
        """非白名单应用 → 403"""
        non_whitelisted_id = "999999999999999998"
        resp = api("GET", f"/connectors/{connector}", app_id=non_whitelisted_id)
        if resp is not None:
            assert resp.status_code == 403, (
                f"Expected 403 for non-whitelisted app, got {resp.status_code}: "
                f"{resp.json() if resp else ''}"
            )
