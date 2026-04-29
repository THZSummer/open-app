# api-server 代码覆盖率测试结果

测试时间：2026-04-28 20:53:34

测试命令：`mvn -DskipTests=false org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report`

报告目录：`jacoco/index.html`

原始汇总：`coverage-summary.json`

## 1. 测试执行结果

| 指标 | 结果 |
| --- | ---: |
| 测试数 | 91 |
| 失败数 | 0 |
| 错误数 | 0 |
| 跳过数 | 0 |

## 2. 总覆盖率

| 覆盖维度 | 覆盖率 | 已覆盖 | 未覆盖 |
| --- | ---: | ---: | ---: |
| 指令 Instruction | 87.19% | 1423 | 209 |
| 分支 Branch | 78.00% | 78 | 22 |
| 行 Line | 86.23% | 382 | 61 |
| 方法 Method | 83.33% | 60 | 12 |
| 圈复杂度 Complexity | 78.86% | 97 | 26 |

## 3. ApiGatewayService 覆盖率

| 类 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 | 方法覆盖率 | 未覆盖行数 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `com.xxx.api.gateway.service.ApiGatewayService` | 100.00% | 100.00% | 100.00% | 100.00% | 0 |

`findScopeByPathAndMethod(String, String)` 已直接覆盖：指令 33/33、分支 4/4、行 5/5、方法 1/1。

## 4. 包级覆盖率

| 包 | 类数量 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 |
| --- | ---: | ---: | ---: | ---: |
| `com.xxx.api` | 1 | 0.00% | N/A | 0.00% |
| `com.xxx.api.common.config` | 2 | 0.00% | N/A | 0.00% |
| `com.xxx.api.common.controller` | 1 | 100.00% | N/A | 100.00% |
| `com.xxx.api.common.exception` | 2 | 100.00% | N/A | 100.00% |
| `com.xxx.api.common.model` | 1 | 100.00% | N/A | 100.00% |
| `com.xxx.api.common.service.impl` | 1 | 0.00% | 0.00% | 0.00% |
| `com.xxx.api.common.util` | 1 | 91.63% | 90.00% | 86.54% |
| `com.xxx.api.data.controller` | 1 | 100.00% | 100.00% | 100.00% |
| `com.xxx.api.data.service` | 1 | 100.00% | 100.00% | 100.00% |
| `com.xxx.api.gateway.controller` | 1 | 86.53% | 70.00% | 81.40% |
| `com.xxx.api.gateway.service` | 1 | 100.00% | 100.00% | 100.00% |
| `com.xxx.api.scope.controller` | 1 | 100.00% | N/A | 100.00% |
| `com.xxx.api.scope.entity` | 1 | 98.15% | 90.00% | 92.86% |
| `com.xxx.api.scope.service` | 1 | 94.66% | 83.33% | 93.51% |

## 5. 类级覆盖率

| 类 | 指令覆盖率 | 分支覆盖率 | 行覆盖率 | 方法覆盖率 | 未覆盖行数 |
| --- | ---: | ---: | ---: | ---: | ---: |
| `com.xxx.api.common.config.RedisConfig` | 0.00% | N/A | 0.00% | 0.00% | 9 |
| `com.xxx.api.common.service.impl.ApplicationServiceMockImpl` | 0.00% | 0.00% | 0.00% | 0.00% | 19 |
| `com.xxx.api.common.config.JacksonConfig` | 0.00% | N/A | 0.00% | 0.00% | 9 |
| `com.xxx.api.ApiServerApplication` | 0.00% | N/A | 0.00% | 0.00% | 3 |
| `com.xxx.api.gateway.controller.ApiGatewayController` | 86.53% | 70.00% | 81.40% | 80.00% | 8 |
| `com.xxx.api.common.util.SignatureUtil` | 91.63% | 90.00% | 86.54% | 90.91% | 7 |
| `com.xxx.api.scope.entity.UserAuthorization` | 98.15% | 90.00% | 92.86% | 100.00% | 1 |
| `com.xxx.api.scope.service.ScopeService` | 94.66% | 83.33% | 93.51% | 100.00% | 5 |
| `com.xxx.api.data.service.DataQueryService` | 100.00% | 100.00% | 100.00% | 100.00% | 0 |
| `com.xxx.api.scope.controller.ScopeController` | 100.00% | N/A | 100.00% | 100.00% | 0 |
| `com.xxx.api.common.controller.HealthController` | 100.00% | N/A | 100.00% | 100.00% | 0 |
| `com.xxx.api.common.exception.BusinessException` | 100.00% | N/A | 100.00% | 100.00% | 0 |
| `com.xxx.api.data.controller.DataQueryController` | 100.00% | 100.00% | 100.00% | 100.00% | 0 |
| `com.xxx.api.common.exception.GlobalExceptionHandler` | 100.00% | N/A | 100.00% | 100.00% | 0 |
| `com.xxx.api.common.model.ApiResponse` | 100.00% | N/A | 100.00% | 100.00% | 0 |
| `com.xxx.api.gateway.service.ApiGatewayService` | 100.00% | 100.00% | 100.00% | 100.00% | 0 |

## 6. 低覆盖重点

| 类 | 行覆盖率 | 说明 |
| --- | ---: | --- |
| `com.xxx.api.common.config.RedisConfig` | 0.00% | 当前单元测试完全未覆盖 |
| `com.xxx.api.common.service.impl.ApplicationServiceMockImpl` | 0.00% | 当前单元测试完全未覆盖 |
| `com.xxx.api.common.config.JacksonConfig` | 0.00% | 当前单元测试完全未覆盖 |
| `com.xxx.api.ApiServerApplication` | 0.00% | 当前单元测试完全未覆盖 |

## 7. 结论

- 本次使用 JaCoCo 对生产代码进行覆盖率统计，测试全部通过。
- `ApiGatewayService` 当前行覆盖率、分支覆盖率、方法覆盖率均为 `100.00%`。
- 当前整体行覆盖率为 `86.23%`，分支覆盖率为 `78.00%`。
- 项目整体未达到 100%，剩余缺口主要集中在启动类、配置类和 Mock 应用服务实现。

