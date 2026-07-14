#!/usr/bin/env python3
"""#32 POST /flows/{id}/versions/{vid}/publish — 发布连接流版本"""
import json
import time
import pytest
from common import api, db, set_lookup_config, INTERNAL_APP_ID
from conftest import assert_operate_log, _find_approval, _set_orchestration


def _mk_config(**kw):
    """构建编排配置对象（返回 dict，由 requests 一次序列化）。

    避免返回 json.dumps 字符串导致 Jackson 侧被解析为 TextNode 而非 ObjectNode，
    从而引发"节点列表为空"等误报。
    """
    nodes = kw.get("nodes", [
        {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
        {"id": "exit", "type": "exit", "data": {"type": "exit"}},
    ])
    edges = kw.get("edges", [{"id": "e1", "source": "trigger", "target": "exit"}])
    flow_config = kw.get("flowConfig", {"flowMode": "single"})
    return {"nodes": nodes, "edges": edges, "flowConfig": flow_config}


def _mk_parallel_config(branch_count, **kw):
    """构建并行模式编排配置：trigger -> parallel -> [b1..bN] -> exit

    并行分支数 = parallel 网关节点出度。connector 节点不携带 connectorId，
    故不会触发连接器版本引用校验，便于聚焦测试分支数校验本身。

    返回 dict（非 json.dumps 字符串），由 requests 库的 json= 参数一次序列化，
    确保 orchestrationConfig 以 JSON object 形式发送（避免 Jackson TextNode 问题）。

    Args:
        branch_count: 并行分支数（parallel 节点的出度）
    Returns:
        dict: 编排配置对象 {nodes, edges, flowConfig}
    """
    nodes = [
        {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
        {"id": "parallel", "type": "parallel", "data": {"type": "parallel"}},
    ]
    edges = [{"id": "e0", "source": "trigger", "target": "parallel"}]
    for i in range(1, branch_count + 1):
        bid = f"b{i}"
        nodes.append({"id": bid, "type": "connector", "data": {"type": "connector"}})
        edges.append({"id": f"ep{i}", "source": "parallel", "target": bid})
        edges.append({"id": f"ex{i}", "source": bid, "target": "exit"})
    nodes.append({"id": "exit", "type": "exit", "data": {"type": "exit"}})
    flow_config = kw.get("flowConfig", {"flowMode": "parallel"})
    return {"nodes": nodes, "edges": edges, "flowConfig": flow_config}


class TestFlowVersionPublish:
    CONFIG = {"nodes": [{"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}}, {"id": "n1", "type": "connector", "data": {"type": "connector"}}, {"id": "exit", "type": "exit", "data": {"type": "exit"}}], "edges": [{"id": "e1", "source": "trigger", "target": "n1"}, {"id": "e2", "source": "n1", "target": "exit"}], "flowConfig": {"flowMode": "single"}}

    @pytest.mark.L2
    def test_publish(self, draft_flow):
        """FR-026: 草稿→待审批，验证状态变更"""
        fid, fvid = draft_flow
        # 前置：写入最小编排配置（发布时校验非空）
        db(f"UPDATE openplatform_v2_cp_flow_version_t SET orchestration_config = '{json.dumps(self.CONFIG)}' WHERE id = {fvid}")
        # 前置确认：草稿状态
        resp0 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp0.status_code == 200
        before = resp0.json()["data"]
        assert before.get("status") in (1, "1"), f"Expected status=1 (草稿), got {before.get('status')}"
        # 执行发布（提交审批）
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp.status_code in (200, 201), f"Expected 200/201, got {resp.status_code}"
        assert resp.json()["code"] == "200"
        # 验证状态变更（草稿→待审批=2）
        resp2 = api("GET", f"/flows/{fid}/versions/{fvid}")
        assert resp2.status_code == 200
        after = resp2.json()["data"]
        assert after.get("status") in (2, "2"), f"Expected status=2 (待审批), got {after.get('status')}"

    @pytest.mark.L2
    def test_publish_log(self, draft_flow):
        """发布版本 → 操作日志"""
        fid, fvid = draft_flow
        api("PUT", f"/flows/{fid}/versions/{fvid}", {
            "orchestrationConfig": json.dumps({
                "flowConfig": {"flowMode": "serial", "timeout": 3000},
                "nodes": [
                    {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                    {"id": "exit", "type": "exit", "data": {"type": "exit"}},
                ],
                "edges": [{"id": "e1", "source": "trigger", "target": "exit"}],
            })
        })
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        assert resp.status_code in (200, 201)
        assert_operate_log("提交连接流版本发布审批")

    # —— 配置限制校验 (plan-config §4.1.3) ——

    @pytest.mark.L4
    def test_node_timeout_refused(self, draft_flow):
        """#6 Node.Max.Timeout.Seconds: data.timeoutMs > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Node.Max.Timeout.Seconds", "1")
        try:
            cfg = _mk_config(nodes=[
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "conn", "type": "connector", "data": {"type": "connector", "timeoutMs": 999999}},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ])
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#6 node timeout should be refused, got {resp.json()}"
        finally:
            set_lookup_config("Node.Max.Timeout.Seconds", "5")

    @pytest.mark.L4
    def test_flow_config_bytes_refused(self, draft_flow):
        """#7 Flow.Config.Max.Bytes: orchestration JSON 字节数 > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Config.Max.Bytes", "50")
        try:
            big_cfg = _mk_config(nodes=[
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http", "desc": "x" * 2000}},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ])
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": big_cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#7 oversized config should be refused"
        finally:
            set_lookup_config("Flow.Config.Max.Bytes", "1048576")

    @pytest.mark.L4
    def test_qps_refused(self, draft_flow):
        """#8 Flow.Max.Qps: rateLimitConfig.maxQps > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Max.Qps", "10")
        try:
            cfg = _mk_config(flowConfig={
                "flowMode": "single",
                "rateLimitConfig": {"maxQps": 9999, "maxConcurrency": 1}
            })
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#8 QPS exceed should be refused"
        finally:
            set_lookup_config("Flow.Max.Qps", "1000")

    @pytest.mark.L4
    def test_concurrency_refused(self, draft_flow):
        """#9 Flow.Max.Concurrency: rateLimitConfig.maxConcurrency > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Max.Concurrency", "10")
        try:
            cfg = _mk_config(flowConfig={
                "flowMode": "single",
                "rateLimitConfig": {"maxQps": 1, "maxConcurrency": 9999}
            })
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#9 concurrency exceed should be refused"
        finally:
            set_lookup_config("Flow.Max.Concurrency", "1000")

    @pytest.mark.L4
    def test_cache_ttl_refused(self, draft_flow):
        """#10 Flow.Max.Cache.Ttl.Seconds: flowConfig.cache.ttl > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Max.Cache.Ttl.Seconds", "60")
        try:
            cfg = _mk_config(flowConfig={
                "flowMode": "single",
                "cache": {"ttl": 999999, "enabled": True}
            })
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#10 cache TTL exceed should be refused"
        finally:
            set_lookup_config("Flow.Max.Cache.Ttl.Seconds", "1296000")

    @pytest.mark.L4
    def test_parallel_branches_refused(self, draft_flow):
        """#11 Flow.Max.Parallel.Branches: parallel 网关出度 > 上限 → 拒绝（修正拓扑含 parallel 网关）"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Max.Parallel.Branches", "2")
        try:
            # parallel 出度 = 4 > 上限 2
            cfg = _mk_parallel_config(branch_count=4)
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            body = resp.json()
            assert body["code"] != "200", f"#11 4 分支应超过上限 2，实际通过: {body}"
            assert "并行分支数超过上限" in body.get("messageZh", ""), \
                f"错误信息应提示超上限，实际 messageZh={body.get('messageZh')}"
        finally:
            set_lookup_config("Flow.Max.Parallel.Branches", "8")

    @pytest.mark.L4
    def test_parallel_branches_below_min_refused(self, draft_flow):
        """并行分支数 < 下限(2) → 拒绝：单分支不构成并行语义"""
        fid, fvid = draft_flow
        # parallel 出度 = 1 < 下限 2（硬编码常量 MIN_PARALLEL_BRANCHES=2，无需改 lookup）
        cfg = _mk_parallel_config(branch_count=1)
        api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        body = resp.json()
        assert body["code"] != "200", f"单分支应被拒绝（不构成并行语义），实际: {body}"
        assert "并行分支数至少" in body.get("messageZh", ""), \
            f"错误信息应提示不足下限，实际 messageZh={body.get('messageZh')}"

    @pytest.mark.L4
    def test_parallel_branches_boundary_min_ok(self, draft_flow):
        """并行分支数边界下限 2 → 通过分支数校验"""
        fid, fvid = draft_flow
        # parallel 出度 = 2 = MIN_PARALLEL_BRANCHES，应通过
        cfg = _mk_parallel_config(branch_count=2)
        api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
        resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
        body = resp.json()
        msg = body.get("messageZh", "") + body.get("messageEn", "")
        assert "并行分支" not in msg, \
            f"2 条分支应在合法范围内（不应因分支数原因被拒），实际: {body}"

    @pytest.mark.L4
    def test_serial_nodes_refused(self, draft_flow):
        """#12 Flow.Max.Serial.Connector.Nodes: serial 模式节点数 > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Flow.Max.Serial.Connector.Nodes", "1")
        try:
            cfg = _mk_config(
                flowConfig={"flowMode": "serial"},
                nodes=[
                    {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                    {"id": "c1", "type": "connector", "data": {"type": "connector"}},
                    {"id": "c2", "type": "connector", "data": {"type": "connector"}},
                    {"id": "exit", "type": "exit", "data": {"type": "exit"}},
                ],
                edges=[
                    {"id": "e1", "source": "trigger", "target": "c1"},
                    {"id": "e2", "source": "c1", "target": "c2"},
                    {"id": "e3", "source": "c2", "target": "exit"},
                ],
            )
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#12 serial nodes exceed should be refused"
        finally:
            set_lookup_config("Flow.Max.Serial.Connector.Nodes", "3")

    @pytest.mark.L4
    def test_script_length_refused(self, draft_flow):
        """#13 Script.Max.Length.Chars: 脚本 source 长度 > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Script.Max.Length.Chars", "50")
        try:
            cfg = _mk_config(nodes=[
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "s1", "type": "script", "data": {
                    "type": "script",
                    "script": "function main(ctx) { return { result: '" + "x" * 1000 + "' }; }",
                    "language": "javascript",
                }},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ])
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#13 script length exceed should be refused"
        finally:
            set_lookup_config("Script.Max.Length.Chars", "10000")

    @pytest.mark.L4
    def test_script_timeout_refused(self, draft_flow):
        """#14 Script.Max.Timeout.Seconds: 脚本 data.timeout > 上限 → 拒绝"""
        fid, fvid = draft_flow
        set_lookup_config("Script.Max.Timeout.Seconds", "5")
        try:
            cfg = _mk_config(nodes=[
                {"id": "trigger", "type": "trigger", "data": {"type": "trigger", "triggerType": "http"}},
                {"id": "s1", "type": "script", "data": {
                    "type": "script",
                    "script": "function main(ctx) { return { result: 1 }; }",
                    "language": "javascript",
                    "timeout": 999,
                }},
                {"id": "exit", "type": "exit", "data": {"type": "exit"}},
            ])
            api("PUT", f"/flows/{fid}/versions/{fvid}", {"orchestrationConfig": cfg})
            resp = api("POST", f"/flows/{fid}/versions/{fvid}/publish")
            assert resp.json()["code"] != "200", f"#14 script timeout exceed should be refused"
        finally:
            set_lookup_config("Script.Max.Timeout.Seconds", "30")

    # —— 审批流配置场景 (plan-config §4.1.3, V3 审批模板 app 级隔离) ——

    @pytest.mark.L4
    def test_approval_app_specific_only(self, draft_flow, published_connector):
        """仅应用专属审批流模板：发布→两级审批通过→status=5"""
        fid, fvid = draft_flow
        cid, cvid = published_connector
        _delete_scene_flows()
        _ensure_global_flow()
        _upsert_scene_flow(INTERNAL_APP_ID, [{"userId": "admin", "userName": "AppApprover"}])
        try:
            _set_orchestration(fid, fvid, cid, cvid)
            aid = _find_approval(fvid)
            assert aid, "should create approval record"
            api("POST", f"/approvals/{aid}/approve", {"comment": "L1 scene"})
            api("POST", f"/approvals/{aid}/approve", {"comment": "L2 global"})
            detail = api("GET", f"/flows/{fid}/versions/{fvid}").json()["data"]
            assert detail.get("status") in (5, "5"), f"unexpected status: {detail.get('status')}"
        finally:
            _delete_scene_flows()

    @pytest.mark.L4
    def test_approval_global_only(self, draft_flow, published_connector):
        """仅全局审批流模板(app_id=NULL)：发布→两级审批通过→status=5"""
        fid, fvid = draft_flow
        cid, cvid = published_connector
        _ensure_global_flow()
        _upsert_scene_flow(None, [{"userId": "admin", "userName": "GlobalApprover"}])
        try:
            _set_orchestration(fid, fvid, cid, cvid)
            aid = _find_approval(fvid)
            assert aid, "should create approval record"
            api("POST", f"/approvals/{aid}/approve", {"comment": "L1 scene"})
            api("POST", f"/approvals/{aid}/approve", {"comment": "L2 global"})
            detail = api("GET", f"/flows/{fid}/versions/{fvid}").json()["data"]
            assert detail.get("status") in (5, "5"), f"unexpected status: {detail.get('status')}"
        finally:
            _delete_scene_flows()

    @pytest.mark.L4
    def test_approval_both_app_and_global(self, draft_flow, published_connector):
        """应用+全局共存：不抛 TooManyResultsException，应用级优先生效→两级审批通过"""
        fid, fvid = draft_flow
        cid, cvid = published_connector
        _ensure_global_flow()
        _upsert_scene_flow(INTERNAL_APP_ID, [{"userId": "admin", "userName": "AppSpecific"}])
        _upsert_scene_flow(None, [{"userId": "admin", "userName": "GlobalFallback"}])
        try:
            _set_orchestration(fid, fvid, cid, cvid)
            aid = _find_approval(fvid)
            assert aid, "should create approval record (no TooManyResultsException)"
            r = api("GET", f"/approvals/{aid}").json()
            nodes_json = str(r.get("data", {}))
            assert "AppSpecific" in nodes_json, f"should contain app-specific nodes: {nodes_json}"
            assert "GlobalFallback" in nodes_json, f"should also contain platform-wide fallback nodes (overlay): {nodes_json}"
            api("POST", f"/approvals/{aid}/approve", {"comment": "L1 scene(app)"})
            api("POST", f"/approvals/{aid}/approve", {"comment": "L1 scene(null)"})
            api("POST", f"/approvals/{aid}/approve", {"comment": "L2 global"})
            detail = api("GET", f"/flows/{fid}/versions/{fvid}").json()["data"]
            assert detail.get("status") in (5, "5"), f"unexpected status: {detail.get('status')}"
        finally:
            _delete_scene_flows()

    @pytest.mark.L4
    def test_approval_global_only_fallback(self, draft_flow, published_connector):
        """仅全场景全应用模板(④)：scene(app+null)皆空→1级审批通过"""
        fid, fvid = draft_flow
        cid, cvid = published_connector
        _ensure_global_flow()
        _delete_scene_flows()
        try:
            _set_orchestration(fid, fvid, cid, cvid)
            aid = _find_approval(fvid)
            assert aid, "should create approval record with global-only nodes"
            api("POST", f"/approvals/{aid}/approve", {"comment": "L1 global"})
            detail = api("GET", f"/flows/{fid}/versions/{fvid}").json()["data"]
            assert detail.get("status") in (5, "5"), f"unexpected status: {detail.get('status')}"
        finally:
            _delete_scene_flows()

    @pytest.mark.L4
    def test_approval_empty_auto_approve(self, draft_flow, published_connector):
        """全空免审：无任何审批模板→直接APPROVED→status=5"""
        fid, fvid = draft_flow
        cid, cvid = published_connector
        _delete_scene_flows()
        db("UPDATE openplatform_v2_approval_flow_t SET status = 0 WHERE code = 'global'")
        try:
            _set_orchestration(fid, fvid, cid, cvid)
            detail = api("GET", f"/flows/{fid}/versions/{fvid}").json()["data"]
            assert detail.get("status") in (5, "5"), f"expected auto-approve to published, got status={detail.get('status')}"
        finally:
            db("UPDATE openplatform_v2_approval_flow_t SET status = 1 WHERE code = 'global'")


# —— 审批流模板 helper（直接操作 DB，因 API 需 @PlatformAdminPermission） ——

def _snow_id():
    return int(time.time_ns() / 1000) % 100000000000000000 + hash(str(time.time())) % 1000000


def _ensure_global_flow():
    from common import db_val, db as _db
    # 查找全局审批流 (含 status=0 被其他测试临时禁用的)
    exists = db_val("SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'global' AND app_id IS NULL ORDER BY status DESC LIMIT 1")
    nodes = json.dumps([{"userId": "admin", "userName": "全场景全应用"}])
    if exists:
        _db(f"UPDATE openplatform_v2_approval_flow_t SET nodes = '{nodes}', status = 1 WHERE id = {exists}")
        return
    fid = _snow_id()
    _db(f"INSERT INTO openplatform_v2_approval_flow_t (id, name_cn, name_en, code, app_id, nodes, status, create_time, last_update_time, create_by, last_update_by) VALUES ({fid}, '全局审批', 'global', 'global', NULL, '{nodes}', 1, NOW(), NOW(), 'admin', 'admin')")


def _upsert_scene_flow(app_id, nodes):
    from common import db_val, db as _db
    app_cond = f"AND app_id = {app_id}" if app_id is not None else "AND app_id IS NULL"
    existing = db_val(f"SELECT id FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish' {app_cond} AND status = 1")
    nodes_json = json.dumps(nodes)
    app_val = str(app_id) if app_id is not None else "NULL"
    if existing:
        _db(f"UPDATE openplatform_v2_approval_flow_t SET nodes = '{nodes_json}' WHERE id = {existing}")
        return
    fid = _snow_id()
    _db(f"INSERT INTO openplatform_v2_approval_flow_t (id, name_cn, name_en, code, app_id, nodes, status, create_time, last_update_time, create_by, last_update_by) VALUES ({fid}, '测试审批', 'test', 'connector_flow_version_publish', {app_val}, '{nodes_json}', 1, NOW(), NOW(), 'admin', 'admin')")


def _delete_scene_flows():
    from common import db as _db
    _db("DELETE FROM openplatform_v2_approval_flow_t WHERE code = 'connector_flow_version_publish'")
