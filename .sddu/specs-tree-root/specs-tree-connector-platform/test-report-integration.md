# 集成测试报告：连接器平台

**测试日期**: 2026-05-25 14:10:38  
**测试类型**: L3 集成测试（真实服务 + 真实数据库）  
**服务**: open-server (:18080) / connector-api (:18180)

---

## 执行结果摘要

| 指标 | 数值 |
|------|:----:|
| 总用例数 | 59 |
| ✅ 通过 | 39 |
| ❌ 失败 | 20 |
| ⚠️ 错误 | 0 |
| 通过率 | 66.1% |

---

## 详细结果

  ✅ PASS: IT-001 -- 创建连接器成功，ID为string类型
  ✅ PASS: IT-002 -- 缺少nameCn返回400
  ✅ PASS: IT-005 -- 默认分页: curPage=1, pageSize=20
  ✅ PASS: IT-006 -- connectorType=1过滤成功
  ✅ PASS: IT-007 -- keyword搜索成功
  ✅ PASS: IT-008 -- 自定义分页: curPage=2, pageSize=10
  ✅ PASS: IT-010 -- 查询连接器详情成功
  ✅ PASS: IT-012 -- 雪花ID为string类型
  ✅ PASS: IT-013 -- 更新连接器名称成功
  ✅ PASS: IT-015 -- 删除连接器成功
  ✅ PASS: IT-017 -- 获取配置成功，含hasConfig字段
  ✅ PASS: IT-018 -- 新建连接器hasConfig字段存在
  ✅ PASS: IT-020 -- 编辑连接配置成功
  ✅ PASS: IT-021 -- 空connectionConfig返回400
  ✅ PASS: IT-022 -- null connectionConfig返回400
  ✅ PASS: IT-023 -- 创建连接流成功
  ✅ PASS: IT-024 -- 缺少nameCn返回400
  ✅ PASS: IT-025 -- 缺少nameEn返回400
  ✅ PASS: IT-026 -- 查询流列表成功
  ✅ PASS: IT-027 -- lifecycleStatus=0过滤成功
  ✅ PASS: IT-028 -- keyword搜索成功
  ✅ PASS: IT-030 -- 查询流详情成功
  ✅ PASS: IT-032 -- 更新流名称成功
  ✅ PASS: IT-036 -- 启动流成功
  ✅ PASS: IT-037 -- 重复启动被拦截
  ✅ PASS: IT-039 -- 停止流成功
  ✅ PASS: IT-042 -- 获取编排配置成功
  ✅ PASS: IT-045 -- 空编排配置返回400
  ✅ PASS: IT-046 -- null编排配置返回400
  SKIP IT-049: connector-api 未运行 (port 18180)
  SKIP IT-050: connector-api 未运行 (port 18180)
  SKIP IT-051: connector-api 未运行 (port 18180)
  ✅ PASS: IT-052 -- 成功响应包含所有标准字段
  ✅ PASS: IT-053 -- 错误响应code!=200且包含messageZh
  ✅ PASS: IT-054 -- 分页响应格式完整
  ✅ PASS: IT-056 -- 枚举字段为数字类型
  ✅ PASS: IT-057 -- 时间字段为ISO 8601格式
  ✅ PASS: IT-058 -- 字段名为camelCase规范
  ✅ PASS: IT-059 -- 错误码在预期范围内
  - FAIL test_it_003_create_invalid_connector_type: self.assertNotEqual(data["code"], "200")
  - FAIL test_it_004_create_name_cn_too_long: self.assertIn(resp.status_code, (400, 422))
  - FAIL test_it_009_list_empty_result: self.assertEqual(data["page"]["total"], 0)
  - FAIL test_it_011_detail_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_014_update_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_016_delete_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_019_config_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_029_list_flows_empty: self.assertEqual(data["page"]["total"], 0)
  - FAIL test_it_031_flow_detail_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_033_update_flow_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_034_delete_flow: self.assertEqual(data["code"], "200")
  - FAIL test_it_035_delete_flow_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_038_start_flow_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_040_stop_flow_already_stopped: self.assertIn(data["code"], ("400", "409"))
  - FAIL test_it_041_stop_flow_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_043_flow_config_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_044_save_flow_config: self.assertEqual(data["code"], "200")
  - FAIL test_it_047_test_run_flow_not_found: self.assertEqual(data["code"], "404")
  - FAIL test_it_048_test_run_no_config: self.assertIn(data["code"], ("400", "422", "404"))
  - FAIL test_it_055_bigint_id_as_string: self.assertIsInstance(item[key], str)

---

*报告由 run_integration_tests.py 自动生成*
