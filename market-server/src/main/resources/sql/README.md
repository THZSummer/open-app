# 测试数据管理

本目录包含能力开放平台的测试数据管理脚本，用于解决测试报告中的数据唯一性冲突和测试数据依赖问题。

## 目录结构

```
open-server/src/main/resources/sql/
├── test-data-init.sql          # 测试数据初始化脚本
└── test-data-cleanup.sql       # 测试数据清理脚本

.sddu/specs-tree-root/specs-tree-capability-open-platform/test/scripts/
├── test-all-apis-optimized.sh  # 优化版测试脚本（自动初始化和清理）
└── verify-test-data.sh         # 测试数据验证脚本
```

## 使用方法

### 方式1：自动执行（推荐）

执行优化版测试脚本，会自动初始化和清理测试数据：

```bash
cd .sddu/specs-tree-root/specs-tree-capability-open-platform/test/scripts
./test-all-apis-optimized.sh
```

### 方式2：手动执行

#### 步骤1：初始化测试数据

```bash
mysql -u root -p openapp < open-server/src/main/resources/sql/test-data-init.sql
```

#### 步骤2：执行测试

```bash
cd .sddu/specs-tree-root/specs-tree-capability-open-platform/test/scripts
./test-all-apis.sh
```

#### 步骤3：清理测试数据

```bash
mysql -u root -p openapp < open-server/src/main/resources/sql/test-data-cleanup.sql
```

## 测试数据说明

### ID范围规划

| 资源类型 | ID范围 | 说明 |
|---------|-------|------|
| 分类 | 1-99 | 一级和二级分类 |
| API | 100-199 | 包含待审、已发布状态 |
| 事件 | 200-299 | 包含待审、已发布状态 |
| 回调 | 300-399 | 包含待审、已发布状态 |
| 订阅 | 300-399 | 用于权限配置和撤回测试 |
| 权限 | 1000-1099 | API、事件、回调权限 |
| 审批流程 | 1-99 | 默认审批流程 |
| 审批记录 | 500-599 | 待审批记录 |

### 关键测试数据

#### 待审状态资源（用于撤回测试）

- API ID=102：待审API，用于 TC-API-006 撤回测试
- 事件 ID=201：待审事件，用于 TC-EVENT-006 撤回测试
- 回调 ID=301：待审回调，用于 TC-CALLBACK-006 撤回测试

#### 订阅记录（用于权限配置和撤回测试）

- 订阅 ID=300：API订阅，用于 TC-API-PERM-004
- 订阅 ID=301：事件订阅，用于 TC-EVENT-PERM-004/005
- 订阅 ID=302：回调订阅，用于 TC-CALLBACK-PERM-004/005

#### 审批记录（用于审批操作测试）

- 审批 ID=500：待审批记录，用于 TC-APPROVAL-006~011

## 测试脚本参数

优化版测试脚本支持以下命令行参数：

```bash
./test-all-apis-optimized.sh [options]

选项：
  --no-init      跳过数据初始化
  --no-cleanup   跳过数据清理
  --db-host      数据库主机（默认：localhost）
  --db-port      数据库端口（默认：3306）
  --db-name      数据库名称（默认：openapp）
  --db-user      数据库用户（默认：root）
  --db-pass      数据库密码（默认：空）
```

### 示例

#### 跳过初始化（已有测试数据）

```bash
./test-all-apis-optimized.sh --no-init
```

#### 自定义数据库参数

```bash
./test-all-apis-optimized.sh \
  --db-host 192.168.1.100 \
  --db-port 3306 \
  --db-name openapp \
  --db-user root \
  --db-pass password
```

## 验证测试数据

执行验证脚本检查测试数据是否正确初始化：

```bash
cd .sddu/specs-tree-root/specs-tree-capability-open-platform/test/scripts
./verify-test-data.sh
```

## 预期效果

### 解决的问题

1. **数据唯一性冲突（409错误）**
   - 使用随机化的测试数据（时间戳）避免Scope、Topic、流程编码冲突
   - 预期解决：TC-API-003, TC-EVENT-003, TC-CALLBACK-003, TC-APPROVAL-003

2. **测试数据依赖（404错误）**
   - 初始化待审状态资源用于撤回测试
   - 初始化订阅记录用于权限配置和撤回测试
   - 预期解决：TC-API-006, TC-EVENT-006, TC-CALLBACK-006, TC-API-PERM-004, TC-EVENT-PERM-004/005, TC-CALLBACK-PERM-004/005

### 预期测试通过率

预期测试通过率从 **58.8%** 提升至 **90%** 以上（排除接口实现问题）。

## 注意事项

1. **数据库权限**：执行脚本需要数据库的DELETE、INSERT权限
2. **数据隔离**：测试数据ID范围与生产数据隔离，避免影响生产数据
3. **回滚机制**：测试失败时可手动执行清理脚本恢复数据
4. **审批接口问题**：测试报告中显示审批管理接口存在500错误，这是接口实现问题，不在测试数据管理范围内

## 相关文档

- [测试报告](.sddu/specs-tree-root/specs-tree-capability-open-platform/test/test-report-open-server-20260422-094752.md)
- [实现报告](.sddu/specs-tree-root/specs-tree-capability-open-platform/build.md)
