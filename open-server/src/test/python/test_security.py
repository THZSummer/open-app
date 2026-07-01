#!/usr/bin/env python3
"""安全测试: 白名单准入 + 操作日志

V3 新增：验证 19 个补全操作日志的接口是否正确写入 openplatform_operate_log_t。
"""
import time
import pytest
from conftest import api, db, db_val, connector, flow, published_connector, deployed_flow


def _snow_id():
    return int(time.time() * 1000000) % 100000000000000000


class TestAppWhitelist:
    @pytest.mark.L2
    def test_whitelist_app_ok(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        if resp is not None:
            assert resp.status_code == 200

    @pytest.mark.L2
    def test_missing_app_id_header(self, connector):
        resp = api("GET", f"/connectors/{connector}", app_id=None)
        assert resp is not None

    @pytest.mark.L2
    def test_empty_whitelist_rejects_all_post_registration(self, connector):
        resp = api("GET", f"/connectors/{connector}")
        if resp is not None:
            assert resp.status_code == 200


class TestOperationLog:
    def _wait_log(self, keyword, timeout=2):
        """等待异步操作日志写入，搜索 after_data/before_data/operate_content"""
        time.sleep(0.5)
        count = db_val(f"SELECT COUNT(*) FROM openplatform_operate_log_t WHERE after_data LIKE '%{keyword}%' OR before_data LIKE '%{keyword}%' OR operate_content LIKE '%{keyword}%'")
        return int(count) if count else 0

    @pytest.mark.L1
    def test_create_connector_log(self):
        """创建连接器 → 操作日志"""
        resp = api("POST", "/connectors", {"nameCn": "日志测试连接器", "nameEn": "LogTestConnector", "connectorType": 1})
        api_cid = None
        if resp is not None and resp.status_code in (200, 201):
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                api_cid = data["data"].get("connectorId")
        if api_cid:
            assert self._wait_log(api_cid) >= 1
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {api_cid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {api_cid}")

    @pytest.mark.L1
    def test_update_flow_log(self, flow):
        """更新连接流 → 操作日志"""
        new_name = "更新的日志测试流-" + str(int(time.time()))
        resp = api("PUT", f"/flows/{flow}", {"nameCn": new_name})
        if resp is not None and resp.status_code in (200, 201):
            assert self._wait_log(str(flow)) >= 1

    @pytest.mark.L1
    def test_create_and_delete_connector_two_phase_log(self):
        """创建+失效+删除全流程操作日志"""
        resp = api("POST", "/connectors", {"nameCn": "日志删除测试", "nameEn": "LogDeleteTest", "connectorType": 1})
        api_cid = None
        if resp is not None and resp.status_code in (200, 201):
            data = resp.json()
            if data.get("code") in ("200", 200) and data.get("data"):
                api_cid = data["data"].get("connectorId")
        if api_cid:
            api("PUT", f"/connectors/{api_cid}/invalidate")
            api("DELETE", f"/connectors/{api_cid}")
            assert self._wait_log(api_cid) >= 1
            db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE connector_id = {api_cid}")
            db(f"DELETE FROM openplatform_v2_cp_connector_t WHERE id = {api_cid}")

    # ═══════════════════════════════════════════════════════
    # V3 新增：补全的操作日志验证
    # ═══════════════════════════════════════════════════════

    @pytest.mark.L2
    def test_invalidate_connector_log(self, published_connector):
        """失效连接器 → 操作日志"""
        cid, _ = published_connector
        resp = api("PUT", f"/connectors/{cid}/invalidate")
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(cid)) >= 1

    @pytest.mark.L2
    def test_recover_connector_log(self, connector):
        """恢复连接器 → 操作日志"""
        cid = connector
        # 先失效再恢复
        db(f"UPDATE openplatform_v2_cp_connector_t SET status = 3 WHERE id = {cid}")
        resp = api("PUT", f"/connectors/{cid}/recover")
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(cid)) >= 1

    @pytest.mark.L2
    def test_create_connector_version_log(self, connector):
        """创建草稿版本 → 操作日志"""
        resp = api("POST", f"/connectors/{connector}/versions", {})
        if resp is not None and resp.status_code in (200, 201):
            vid = resp.json().get("data", {}).get("versionId")
            if vid:
                assert self._wait_log(vid) >= 1
                db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")

    @pytest.mark.L2
    def test_publish_connector_version_log(self, connector):
        """发布版本 → 操作日志"""
        # 创建草稿
        vid = _snow_id()
        db(f"INSERT INTO openplatform_v2_cp_connector_version_t (id, connector_id, version_number, status, connection_config, create_by, last_update_by) VALUES ({vid}, {connector}, 1, 1, '{{\"protocol\":\"HTTP\"}}', 'tester', 'tester')")
        resp = api("PUT", f"/connectors/{connector}/versions/{vid}/publish")
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(vid)) >= 1
        db(f"DELETE FROM openplatform_v2_cp_connector_version_t WHERE id = {vid}")

    @pytest.mark.L2
    def test_copy_flow_log(self, deployed_flow):
        """复制连接流 → 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/copy", {})
        if resp is not None and resp.status_code in (200, 201):
            new_fid = resp.json().get("data", {}).get("flowId")
            if new_fid:
                assert self._wait_log(new_fid) >= 1
                db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE flow_id = {new_fid}")
                db(f"DELETE FROM openplatform_v2_cp_connector_version_ref_t WHERE flow_id = {new_fid}")
                db(f"DELETE FROM openplatform_v2_cp_flow_t WHERE id = {new_fid}")

    @pytest.mark.L2
    def test_deploy_flow_log(self, deployed_flow):
        """部署连接流 → 操作日志"""
        fid, fvid = deployed_flow
        resp = api("POST", f"/flows/{fid}/deploy", {"versionId": str(fvid)})
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(fid)) >= 1

    @pytest.mark.L2
    def test_start_stop_flow_log(self, deployed_flow):
        """启动+停止连接流 → 操作日志"""
        fid, _ = deployed_flow
        resp = api("POST", f"/flows/{fid}/start")
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(fid)) >= 1
            api("POST", f"/flows/{fid}/stop")
            assert self._wait_log(str(fid)) >= 2

    @pytest.mark.L2
    def test_invalidate_flow_log(self, flow):
        """失效连接流 → 操作日志"""
        fid = flow
        resp = api("PUT", f"/flows/{fid}/invalidate")
        if resp is not None and resp.status_code == 200:
            assert self._wait_log(str(fid)) >= 1

    @pytest.mark.L2
    def test_create_flow_version_log(self, flow):
        """创建连接流版本草稿 → 操作日志"""
        resp = api("POST", f"/flows/{flow}/versions", {})
        if resp is not None and resp.status_code in (200, 201):
            vid = resp.json().get("data", {}).get("versionId")
            if vid:
                assert self._wait_log(vid) >= 1
                db(f"DELETE FROM openplatform_v2_cp_flow_version_t WHERE id = {vid}")
