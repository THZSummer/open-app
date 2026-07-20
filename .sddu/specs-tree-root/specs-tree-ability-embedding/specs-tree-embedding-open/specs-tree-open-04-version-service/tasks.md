# 任务：VersionServiceImpl 改造

> 父 Feature: 嵌入能力开放面（EMBED-OPEN-001）  
> 对应任务: TASK-004 | 复杂度: S | ADR: ADR-004  
> 前置依赖: 无（version 模块独立，与 TASK-001~003 可并行）

## 描述

将 `VersionServiceImpl.createVersion()` 中的硬编码排除逻辑改为按数据库字段过滤。此变更属于独立 `version` 模块，与 `ability` 模块的 TASK-001~003 无代码依赖。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| MODIFY | `open-server/.../version/service/impl/VersionServiceImpl.java` |
| NEW | `open-server/src/test/java/.../version/service/VersionServiceRequireReleaseTest.java` |

## 验收标准

- [ ] 旧逻辑：`filter(r -> !Objects.equals(r.getAbilityType(), AbilityTypeEnum.GROUP_JOIN_NOTIFICATION.getCode()))`
- [ ] 新逻辑：`filter(r -> Boolean.TRUE.equals(r.getRequireRelease()))`
- [ ] `requireRelease=1` 的能力被纳入版本发布检查
- [ ] `requireRelease=0` 的能力被跳过（即时生效，无需版本发布）
- [ ] type=6（应用入群通知）的 `require_release` 默认值为 0，行为与改造前一致
- [ ] 其他逻辑不变
- [ ] Java 单元测试: VersionServiceRequireReleaseTest 通过（requireRelease=1 纳入 / =0 跳过 / type=6 行为不变）

## 验证

```bash
# Java 单元测试
mvn -f open-server/pom.xml test -Dtest="VersionServiceRequireReleaseTest"
```
