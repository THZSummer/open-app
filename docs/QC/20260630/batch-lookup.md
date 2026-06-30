# 批 lookup：通用查询

> 1 文件。LookupController 在 common 批逐行读过（51 行）。

## 文件覆盖表（1/1）
| 文件 | 逐行读 | 问题数 |
|------|:---:|:---:|
| common/controller/LookupController.java(51) | ✅(common批) | 0 |

## 意见：无。

## 结论
✅ 通过。LookupController 仅 1 个只读接口（GET /lookup/whitelist，查 APP_UI_WHITELIST 灰度白名单），无鉴权注解（低风险，只读配置查询），规范。
