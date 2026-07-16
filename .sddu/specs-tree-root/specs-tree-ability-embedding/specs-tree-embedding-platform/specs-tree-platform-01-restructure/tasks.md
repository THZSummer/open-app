# 任务：模块重组

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-001 | 复杂度: M | 前置依赖: 无

## 描述

将 market-server 中寄生在 `approval` 模块的 ability 相关代码独立为 `modules/ability/` 模块，修正历史代码结构问题，为后续 Admin CRUD 铺平道路。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../modules/ability/` (新模块目录) |
| MOVE | `approval/entity/AbilityEntity.java` → `ability/entity/AbilityEntity.java` |
| MOVE | `approval/mapper/AbilityMapper.java` → `ability/mapper/AbilityMapper.java` |
| MODIFY | `market-server/src/main/resources/mapper/AbilityMapper.xml` (namespace 更新为 `...ability.mapper.AbilityMapper`) |
| MODIFY | 全局替换 import `...approval.entity.AbilityEntity` → `...ability.entity.AbilityEntity` |
| MODIFY | 全局替换 import `...approval.mapper.AbilityMapper` → `...ability.mapper.AbilityMapper` |
| NEW | `market-server/src/test/java/.../ability/AbilityMapperSmokeTest.java` |
| NEW | `market-server/src/test/python/modules/approval/test_approval_smoke.py` |

## 验收标准

- [ ] `modules/ability/entity/AbilityEntity.java` 存在，包名正确
- [ ] `modules/ability/mapper/AbilityMapper.java` 存在，包名正确
- [ ] `AbilityMapper.xml` namespace 已更新
- [ ] 全项目无残留 `import ...approval...Ability*`
- [ ] 回归测试: AbilityMapperSmokeTest 通过（Spring 集成测试，真实 autowire + DB 查询，证明重构后已有功能不受影响）
- [ ] Python 集成测试: test_approval_smoke.py 通过（调用已有审批接口验证 AbilityMapper 重构后全链路正常）

## 验证

> ⚠️ 本 task 是纯重构，执行过程中会有大量编译错误需逐步修复。**验证点需逐个执行**，每步通过后再进入下一步，不可一次性跑脚本。

### 验证点 1：新文件就位，旧文件已移除

```bash
# 新文件存在
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/entity/AbilityEntity.java
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/ability/mapper/AbilityMapper.java
```
**预期**: 两个文件均存在。

```bash
# 旧文件已移除
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/entity/AbilityEntity.java 2>&1
ls market-server/src/main/java/com/xxx/it/works/wecode/v2/modules/approval/mapper/AbilityMapper.java 2>&1
```
**预期**: 两个文件均报 No such file or directory。

---

### 验证点 2：MyBatis namespace 已更新

```bash
grep 'namespace=' market-server/src/main/resources/mapper/AbilityMapper.xml
```
**预期**: 输出含 `"...ability.mapper.AbilityMapper"`（新包路径），不含 `approval`。

---

### 验证点 3：全项目无旧包引用

```bash
grep -rn "approval\.entity\.AbilityEntity\|approval\.mapper\.AbilityMapper" \
  market-server/src/main/java/ 2>/dev/null
```
**预期**: 无输出。如有输出，逐文件修改 import 后重新检查。

---

### 验证点 4：编译通过

```bash
mvn -f market-server/pom.xml compile
```
**预期**: BUILD SUCCESS。如有编译错误，根据报错定位未替换的 import 或包声明问题，修复后重新编译。

---

### 验证点 5：回归测试通过 — 证明已有功能不受影响

```bash
mvn -f market-server/pom.xml test \
  -Dtest="com.xxx.it.works.wecode.v2.modules.ability.AbilityMapperSmokeTest" \
  -Dspring.profiles.active=test
```
**预期**: Tests run: 1, Failures: 0。如有失败，检查：
- `@SpringBootTest` 配置是否正确
- `AbilityMapper` 能否被 Spring 扫描到
- 测试数据库连接是否可用
- MyBatis namespace 与 Mapper XML 是否匹配

---

### 验证点 6：集成测试 — 调用已有接口验证全链路

> 重构后，已有接口 `GET /service/open/v2/apps/pending` 和 `GET /service/open/v2/apps/publish` 内部经过 `ApprovalService → AbilityMapper.selectByIds()`，调用任一接口即可验证 HTTP → Controller → Service → Mapper → DB 全链路正常。

```bash
cd market-server/src/test/python

# 运行重构冒烟集成测试
pytest modules/approval/test_approval_smoke.py -m "" -v
```
**预期**: 1 passed。如有失败，检查：
- market-server 是否已启动
- 接口路径和 Cookie 配置是否正确
- AbilityMapper Bean 是否正常注入
