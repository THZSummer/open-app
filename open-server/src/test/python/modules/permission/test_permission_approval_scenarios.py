#!/usr/bin/env python3
"""权限申请审批场景 — 覆盖 §2.3 _permission_apply 组合"""
import json
import time
import pytest
from common import api, db, INTERNAL_APP_ID
from conftest import _approve_capability_resource


def _uid():
    return int(time.time() * 1000) % 1000000


def _ensure_global_flow():
    from common import db_val, db as _db
    exists = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'global' AND app_id IS NULL AND status = 1")
    if exists:
        return
    fid = int(time.time_ns() / 1000) % 100000000000000000 + _uid()
    nodes = json.dumps([{"userId": "admin", "userName": "全场景全应用"}])
    _db(f"INSERT INTO openplatform_v2_approval_flow_t (id, name_cn, name_en, code, app_id, nodes, status, create_time, last_update_time, create_by, last_update_by) VALUES ({fid}, '全场景审批', 'global', 'global', NULL, '{nodes}', 1, NOW(), NOW(), 'admin', 'admin')")


def _create_category():
    uid = _uid()
    r = api("POST", "/categories", {
        "nameCn": f"cat_approval_{uid}", "nameEn": f"cat_approval_{uid}",
        "categoryAlias": "api_business_app_soa",
    })
    assert r.status_code == 200, f"category creation failed: {r.json()}"
    return r.json()["data"]["id"]


def _create_and_publish(category_id):
    """创建已上架 API，返回 permissionId"""
    uid = _uid()
    r = api("POST", "/apis", {
        "nameCn": f"pytest_perm_{uid}", "nameEn": f"pytest_perm_{uid}",
        "categoryId": str(category_id), "method": "GET",
        "path": f"/api/pytest_perm/{uid}",
        "permission": {"nameCn": f"p_{uid}", "nameEn": f"p_{uid}",
                       "scope": f"api:test:v{uid}"},
    })
    assert r.status_code == 200, f"API creation failed: {r.json()}"
    aid = r.json()["data"]["id"]
    pid = r.json()["data"]["permission"]["id"]
    _approve_capability_resource(aid, "api_register")
    return pid


def _find_approval(business_id, business_type):
    r = api("GET", f"/approvals/pending?businessType={business_type}&page=1&size=50")
    items = r.json().get("data", [])
    for item in (items if isinstance(items, list) else []):
        if str(item.get("businessId")) == str(business_id):
            return int(item["id"])
    return None


def _subscribe(permission_id):
    resp = api("POST", f"/apps/{INTERNAL_APP_ID}/apis/subscribe", {"permissionIds": [permission_id]})
    assert resp.status_code in (200, 201), f"Subscribe failed: {resp.json()}"
    return resp.json()["data"]["records"][0]["id"]


class TestPermissionApplyApproval:
    @pytest.mark.L4
    def test_with_resource_nodes(self):
        """⓪ 有资源审批 + ③ scene(null) + ④ global(null): 三级审批"""
        cid = _create_category()
        pid = _create_and_publish(cid)
        db(f"UPDATE openplatform_v2_permission_t SET need_approval = 1, resource_nodes = '[{{\"userId\":\"admin\",\"userName\":\"资源审批人\"}}]' WHERE id = {pid}")
        _ensure_global_flow()
        try:
            sub_id = _subscribe(pid)
            approval_id = _find_approval(sub_id, "api_permission_apply")
            assert approval_id, "should create approval record"

            r = api("GET", f"/approvals/{approval_id}").json()
            nodes_str = str(r.get("data", {}))
            assert "资源审批" in nodes_str or "resource" in nodes_str.lower(), f"should contain resource approver: {nodes_str}"

            api("POST", f"/approvals/{approval_id}/approve", {"comment": "⓪ resource"})
            api("POST", f"/approvals/{approval_id}/approve", {"comment": "③ scene(null)"})
            api("POST", f"/approvals/{approval_id}/approve", {"comment": "④ global(null)"})

            r2 = api("GET", f"/apps/{INTERNAL_APP_ID}/apis/subscriptions/{sub_id}")
            if r2.status_code == 200:
                detail = r2.json().get("data", {})
                assert detail.get("status") in (1, "1"), f"expected authorized, got {detail}"
        finally:
            db(f"UPDATE openplatform_v2_permission_t SET need_approval = 1, resource_nodes = NULL WHERE id = {pid}")

    @pytest.mark.L4
    def test_auto_approve_no_approvers(self):
        """全空免审: 无模板无资源审批 → 直接 APPROVED"""
        cid = _create_category()
        pid = _create_and_publish(cid)
        db(f"UPDATE openplatform_v2_permission_t SET need_approval = 0 WHERE id = {pid}")
        db("UPDATE openplatform_v2_approval_flow_t SET status = 0 WHERE code = 'global' AND app_id IS NULL")
        try:
            sub_id = _subscribe(pid)
            r = api("GET", f"/apps/{INTERNAL_APP_ID}/apis/subscriptions/{sub_id}")
            if r.status_code == 200:
                detail = r.json().get("data", {})
                assert detail.get("status") in (1, "1"), f"expected auto-approved, got {detail}"
        finally:
            db("UPDATE openplatform_v2_approval_flow_t SET status = 1 WHERE code = 'global' AND app_id IS NULL")
