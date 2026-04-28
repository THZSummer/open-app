# 数据迁移方案

## 1. 方案概述
- 迁移方式：在线迁移（读写分离）
- 触发方式：手动触发（API调用）
- 状态管理：仅记录日志

## 2. 核心接口（每模块3个）

### 2.1 批量迁移接口
```
POST /api/v1/migration/{module}/migrate
请求：{ "ids": [1,2,3] } 或 { "ids": null } 表示全量
响应：{ "success": 10, "failed": 2, "details": [...] }
```

### 2.2 批量验证接口
```
POST /api/v1/migration/{module}/validate
请求：{ "ids": [1,2,3] } 或 { "ids": null } 表示全量
响应：{ "passed": 10, "failed": 2, "details": [...] }
```

### 2.3 状态查询接口
```
GET /api/v1/migration/{module}/status
响应：{ "sourceCount": 100, "targetCount": 95, "pending": 5 }
```

## 3. 模块列表
- 分类模块：category
- API模块：api
- 事件模块：event
- 权限模块：permission
- 审批模块：approval
- 订阅模块：subscription

## 4. 迁移流程
```
阶段1 → 分类迁移 → 验证
阶段2 → 资源迁移（API/事件） → 验证
阶段3 → 权限迁移 → 验证
阶段4 → 审批迁移 → 验证
阶段5 → 订阅迁移 → 验证
```

## 5. Java代码示例

### 5.1 MigrationController（精简版）
```java
@RestController
@RequestMapping("/api/v1/migration")
public class MigrationController {

    @Autowired
    private MigrationService migrationService;

    @PostMapping("/{module}/migrate")
    public Result migrate(@PathVariable String module, @RequestBody MigrationRequest request) {
        return migrationService.migrate(module, request.getIds());
    }

    @PostMapping("/{module}/validate")
    public Result validate(@PathVariable String module, @RequestBody MigrationRequest request) {
        return migrationService.validate(module, request.getIds());
    }

    @GetMapping("/{module}/status")
    public Result status(@PathVariable String module) {
        return migrationService.status(module);
    }
}
```

### 5.2 MigrationService（核心逻辑）
```java
@Service
public class MigrationService {

    @Autowired
    private MigrationLogger migrationLogger;

    public Result migrate(String module, List<Long> ids) {
        List<Long> targetIds = (ids == null) ? getAllSourceIds(module) : ids;
        int success = 0, failed = 0;
        List<MigrationDetail> details = new ArrayList<>();

        for (Long id : targetIds) {
            try {
                Object sourceData = readFromSource(module, id);
                Long targetId = writeToTarget(module, sourceData);
                migrationLogger.log(module, id, targetId, 1, null);
                success++;
            } catch (Exception e) {
                migrationLogger.log(module, id, null, 0, e.getMessage());
                details.add(new MigrationDetail(id, e.getMessage()));
                failed++;
            }
        }

        return Result.ok(new MigrationResponse(success, failed, details));
    }

    public Result validate(String module, List<Long> ids) {
        List<Long> targetIds = (ids == null) ? getAllSourceIds(module) : ids;
        int passed = 0, failed = 0;
        List<MigrationDetail> details = new ArrayList<>();

        for (Long id : targetIds) {
            try {
                Object sourceData = readFromSource(module, id);
                Object targetData = readFromTarget(module, id);
                if (compareData(sourceData, targetData)) {
                    passed++;
                } else {
                    details.add(new MigrationDetail(id, "数据不一致"));
                    failed++;
                }
            } catch (Exception e) {
                details.add(new MigrationDetail(id, e.getMessage()));
                failed++;
            }
        }

        return Result.ok(new ValidationResponse(passed, failed, details));
    }

    public Result status(String module) {
        long sourceCount = countSource(module);
        long targetCount = countTarget(module);
        return Result.ok(new StatusResponse(sourceCount, targetCount, sourceCount - targetCount));
    }
}
```

### 5.3 MigrationLogger（日志记录）
```java
@Component
public class MigrationLogger {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void log(String module, Long sourceId, Long targetId, int status, String error) {
        String sql = "INSERT INTO openplatform_migration_log_t (id, module, source_id, target_id, status, error, create_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, IdGenerator.nextId(), module, sourceId, targetId, status, error, new Date());
    }
}
```

## 6. 迁移日志表
```sql
CREATE TABLE `openplatform_migration_log_t` (
    `id` BIGINT(20) PRIMARY KEY,
    `module` VARCHAR(50) COMMENT '模块名称',
    `source_id` BIGINT(20) COMMENT '源数据ID',
    `target_id` BIGINT(20) COMMENT '目标数据ID',
    `status` TINYINT(10) COMMENT '状态：0失败，1成功',
    `error` TEXT COMMENT '错误信息',
    `create_time` DATETIME(3) COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据迁移日志表';
```
