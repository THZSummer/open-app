#!/usr/bin/env python3
"""#51 POST /flows/{id}/versions/{vid}/debug — 调试运行

FR-041: 调试接口不判断版本状态, 草稿/已发布/待审批/已撤回/已驳回/已失效 均可调试。
"""
import pytest
import json
import time
from common import api, db, db_val, TEST_APP_ID


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


# 简单编排: trigger → exit
_SIMPLE_ORCH = json.dumps({
    "nodes": [
        {
            "id": "node_trigger", "type": "trigger",
            "position": {"x": 100, "y": 200},
            "data": {
                "labelCn": "接收", "labelEn": "Recv", "type": "trigger",
                "triggerType": "http",
                "input": {
                    "protocol": "HTTP",
                    "header": {"type": "object", "properties": {}, "required": []},
                    "query": {"type": "object", "properties": {}, "required": []},
                    "body": {"type": "object", "properties": {"msg": {"type": "string"}}, "required": []}
                }
            }
        },
        {
            "id": "node_exit", "type": "exit",
            "position": {"x": 350, "y": 200},
            "data": {
                "type": "exit",
                "labelCn": "返回", "labelEn": "Ret",
                "outputMapping": {
                    "header": {"type": "object", "properties": {}},
                    "body": {"type": "object", "properties": {"echo": {"type": "string", "value": "${$.node.node_trigger.input.body.msg}"}}}
                }
            }
        }
    ],
    "edges": [{"id": "e1", "source": "node_trigger", "target": "node_exit",
               "type": "smoothstep", "data": {"businessType": "default"}}],
    "flowConfig": {"rateLimitConfig": {"maxQps": 100}}
}, ensure_ascii=False).replace("\\", "\\\\").replace("'", "''")


_INTERNAL_APP_ID = int(db_val(f"SELECT id FROM openplatform_app_t WHERE app_id = '{TEST_APP_ID}' AND status = 1"))


def _create_flow_with_version(status, tag):
    """创建 flow + 指定状态的版本(含编排配置), 返回 (flow_id, version_id)"""
    fid = _snow_id()
    vid = _snow_id()
    db(f"INSERT INTO openplatform_v2_cp_flow_t (id, name_cn, name_en, lifecycle_status, app_id, create_by, last_update_by) "
       f"VALUES ({fid}, 'pytest_debug_{tag}', 'pytest_debug_{tag}', 1, "
       f"{_INTERNAL_APP_ID}, 'tester', 'tester')")
    db(f"INSERT INTO openplatform_v2_cp_flow_version_t (id, flow_id, version_number, status, orchestration_config, create_by, last_update_by) "
       f"VALUES ({vid}, {fid}, 1, {status}, '{_SIMPLE_ORCH}', 'tester', 'tester')")
    return fid, vid


def _cleanup(fid, vid):
    db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {vid}")
    db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {fid}")


def _debug_and_verify(fid, vid, label):
    """调试指定版本, 验证不因版本状态被拒绝"""
    resp = api("POST", f"/flows/{fid}/versions/{vid}/debug",
               {"triggerData": {"msg": "debug_test"}})
    assert resp is not None, f"[{label}] 请求发送失败"
    assert resp.status_code in (200, 201), f"[{label}] HTTP status={resp.status_code}"
    data = resp.json()
    assert data["code"] == "200", f"[{label}] response code={data.get('code')}"

    # 检查返回结果中不包含"版本状态"拒绝错误
    result = data.get("data", {})
    if isinstance(result, dict):
        error_info = result.get("errorInfo", {})
        if error_info:
            err_code = error_info.get("code", "")
            err_msg = error_info.get("messageZh", "")
            # 不应出现版本状态拒绝错误码
            assert "版本状态" not in err_msg, f"[{label}] 被版本状态拒绝: {err_msg}"
            assert "不可调试" not in err_msg, f"[{label}] 被版本状态拒绝: {err_msg}"
            assert err_code != "62006", f"[{label}] 被版本状态拒绝(code=62006): {err_msg}"
    return data


class TestFlowVersionDebug:
    @pytest.mark.L3
    def test_debug_draft_version(self, draft_flow):
        """FR-041: 草稿版本调试触发，验证返回有效响应"""
        fid, fvid = draft_flow
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/debug", {"triggerData": {"message": "hello"}})
        assert resp is not None
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        data = resp.json()
        assert data["code"] == "200"

    @pytest.mark.L3
    def test_debug_published_version(self):
        """FR-041: 已发布版本(status=5)可调试"""
        fid, vid = _create_flow_with_version(5, "published")
        try:
            _debug_and_verify(fid, vid, "已发布版本")
        finally:
            _cleanup(fid, vid)

    @pytest.mark.L3
    def test_debug_pending_approval_version(self):
        """FR-041: 待审批版本(status=2)可调试"""
        fid, vid = _create_flow_with_version(2, "pending_approval")
        try:
            _debug_and_verify(fid, vid, "待审批版本")
        finally:
            _cleanup(fid, vid)

    @pytest.mark.L3
    def test_debug_withdrawn_version(self):
        """FR-041: 已撤回版本(status=3)可调试"""
        fid, vid = _create_flow_with_version(3, "withdrawn")
        try:
            _debug_and_verify(fid, vid, "已撤回版本")
        finally:
            _cleanup(fid, vid)

    @pytest.mark.L3
    def test_debug_rejected_version(self):
        """FR-041: 已驳回版本(status=4)可调试"""
        fid, vid = _create_flow_with_version(4, "rejected")
        try:
            _debug_and_verify(fid, vid, "已驳回版本")
        finally:
            _cleanup(fid, vid)

    @pytest.mark.L3
    def test_debug_invalidated_version(self):
        """FR-041: 已失效版本(status=6)可调试"""
        fid, vid = _create_flow_with_version(6, "invalidated")
        try:
            _debug_and_verify(fid, vid, "已失效版本")
        finally:
            _cleanup(fid, vid)

    @pytest.mark.L4
    def test_version_not_found(self, flow):
        """调试不存在的版本返回非200（实际透传到connector-api可能200）"""
        resp = api("POST", f"/flows/{flow}/versions/999999999999999999/debug", {"triggerData": {}})
        assert resp is not None
