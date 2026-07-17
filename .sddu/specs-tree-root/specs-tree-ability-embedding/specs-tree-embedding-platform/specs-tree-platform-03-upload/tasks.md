# 任务：通用文件上传（后端）

> 父 Feature: 嵌入能力平台面（EMBED-PLATFORM-001）  
> 对应任务: TASK-003 | 复杂度: M | FR: FR-005  
> 前置依赖: TASK-002

## 描述

实现通用文件上传接口。新建 AdminAbilityFileService 接口及双模式实现（storage-mode=standard/dev），在 CommonFileController 中暴露 POST /file/upload。Controller 仅做参数绑定+权限校验，格式/尺寸/大小校验全部在 Service 层。

## 涉及文件

| 操作 | 文件路径 |
|:--:|------|
| NEW | `market-server/.../ability/service/AdminAbilityFileService.java` |
| NEW | `market-server/.../ability/service/impl/AdminAbilityFileServiceImpl.java` |
| NEW | `market-server/.../ability/dto/admin/AdminAbilityUploadRequest.java` |
| NEW | `market-server/.../file/controller/CommonFileController.java` |
| NEW | `open-server/src/main/resources/db/migration/V5__create_common_file.sql` |
| NEW | `market-server/src/test/java/.../ability/controller/AdminAbilityUploadControllerTest.java` |
| NEW | `market-server/src/test/java/.../ability/service/AdminAbilityFileServiceTest.java` |
| NEW | `market-server/src/test/python/modules/ability/test_admin_upload.py` |

## 验收标准

- [x] AdminAbilityFileService 接口定义两个方法：`upload(file, bizType) → {batchId, showUrl}`、`getShowUrl(batchId) → showUrl`
- [x] 开发环境实现（storage-mode=dev）：文件存本地临时目录，写入 openplatform_common_file_t，showUrl 本地静态映射拼接
- [x] 标准环境实现（storage-mode=standard，默认）：预留 OSS/CDN 上传+地址拼接逻辑
- [x] 文件校验（bizType=1 能力图标）：PNG/SVG，40×40PX，≤200KB，不满足返回 400
- [x] 文件校验（bizType=2 能力示意图）：PNG/JPG，520×288PX，≤500KB，不满足返回 400
- [x] Controller 仅参数绑定 + 权限校验，不包含业务校验逻辑
- [x] 接口路径: POST /service/open/v2/file/upload
- [x] V5 迁移脚本：CREATE TABLE openplatform_common_file_t（幂等设计，safe_add_column 风格）
- [x] Java 单元测试: AdminAbilityUploadControllerTest 通过
- [x] Java 单元测试: AdminAbilityFileServiceTest 通过（dev/standard 双模式）
- [x] Python 集成测试: test_admin_upload.py L1/L2/L4 全部通过

## 验证

```bash
# Java 单元测试
mvn -f market-server/pom.xml test -Dtest="AdminAbilityUploadControllerTest,AdminAbilityFileServiceTest"

# Python 集成测试
cd market-server/src/test/python
pytest modules/ability/test_admin_upload.py -m "" -v
```
