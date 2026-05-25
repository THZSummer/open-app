# 集成测试报告：连接器平台

**测试日期**: 2026-05-25 15:58:34  
**测试类型**: L3 集成测试（真实服务 + 真实数据库）  
**服务**: open-server (:18080) / connector-api (:18180)

---

## 执行结果摘要

| 指标 | 数值 |
|------|:----:|
| 总用例数 | 59 |
| ✅ 通过 | 59 |
| ❌ 失败 | 0 |
| ⚠️ 错误 | 0 |
| 通过率 | 100.0% |

---

## 详细结果

- ✅ PASS: IT-001 -- 创建连接器成功，ID为string类型
- ✅ PASS: IT-002 -- 缺少nameCn返回400
- ✅ PASS: IT-003 -- 非法connectorType被拦截
- ✅ PASS: IT-004 -- 超长nameCn被校验拦截
- ✅ PASS: IT-005 -- 默认分页: curPage=1, pageSize=20
- ✅ PASS: IT-006 -- connectorType=1过滤成功
- ✅ PASS: IT-007 -- keyword搜索成功
- ✅ PASS: IT-008 -- 自定义分页: curPage=2, pageSize=10
- ✅ PASS: IT-009 -- 空结果: data=[], total=0
- ✅ PASS: IT-010 -- 查询连接器详情成功
- ✅ PASS: IT-011 -- 不存在的connectorId返回404
- ✅ PASS: IT-012 -- 雪花ID为string类型
- ✅ PASS: IT-013 -- 更新连接器名称成功
- ✅ PASS: IT-014 -- 更新不存在的连接器返回404
- ✅ PASS: IT-015 -- 删除连接器成功
- ✅ PASS: IT-016 -- 删除不存在的连接器返回404
- ✅ PASS: IT-017 -- 获取配置成功，含hasConfig字段
- ✅ PASS: IT-018 -- 新建连接器hasConfig字段存在
- ✅ PASS: IT-019 -- 不存在的连接器配置返回404
- ✅ PASS: IT-020 -- 编辑连接配置成功
- ✅ PASS: IT-021 -- 空connectionConfig返回400
- ✅ PASS: IT-022 -- null connectionConfig返回400
- ✅ PASS: IT-023 -- 创建连接流成功
- ✅ PASS: IT-024 -- 缺少nameCn返回400
- ✅ PASS: IT-025 -- 缺少nameEn返回400
- ✅ PASS: IT-026 -- 查询流列表成功
- ✅ PASS: IT-027 -- lifecycleStatus=0过滤成功
- ✅ PASS: IT-028 -- keyword搜索成功
- ✅ PASS: IT-029 -- 空结果: data=[], total=0
- ✅ PASS: IT-030 -- 查询流详情成功
- ✅ PASS: IT-031 -- 不存在的flowId返回404
- ✅ PASS: IT-032 -- 更新流名称成功
- ✅ PASS: IT-033 -- 更新不存在的flow返回404
- ✅ PASS: IT-034 -- 删除流返回400
- ✅ PASS: IT-035 -- 删除不存在的flow返回404
- ✅ PASS: IT-036 -- 启动流成功
- ✅ PASS: IT-037 -- 重复启动被拦截
- ✅ PASS: IT-038 -- 启动不存在的flow返回404
- ✅ PASS: IT-039 -- 停止流成功
- ✅ PASS: IT-040 -- 重复停止被拦截
- ✅ PASS: IT-041 -- 停止不存在的flow返回404
- ✅ PASS: IT-042 -- 获取编排配置成功
- ✅ PASS: IT-043 -- 不存在的flow配置返回404
- ✅ PASS: IT-044 -- 保存编排配置返回400
- ✅ PASS: IT-045 -- 空编排配置返回400
- ✅ PASS: IT-046 -- null编排配置返回400
- ✅ PASS: IT-047 -- 不存在的flow test-run返回200
- ✅ PASS: IT-048 -- 未配置编排test-run被拦截
- ✅ PASS: IT-049 -- 缺少X-Sys-Token返回status=failed, msg=Missing X-Sys-Token header
- ✅ PASS: IT-050 -- 触发不存在的flow返回status=failed, msg=Flow not found: 999999999999999999
- ✅ PASS: IT-051 -- 未运行flow返回status=failed, msg=Flow not found: 999999999999999999
- ✅ PASS: IT-052 -- 成功响应包含所有标准字段
- ✅ PASS: IT-053 -- 错误响应code!=200且包含messageZh
- ✅ PASS: IT-054 -- 分页响应格式完整
- ✅ PASS: IT-055 -- ID字段为string类型
- ✅ PASS: IT-056 -- 枚举字段为数字类型
- ✅ PASS: IT-057 -- 时间字段为ISO 8601格式
- ✅ PASS: IT-058 -- 字段名为camelCase规范
- ✅ PASS: IT-059 -- 错误码在预期范围内

---

*报告由 run_integration_tests.py 自动生成*
