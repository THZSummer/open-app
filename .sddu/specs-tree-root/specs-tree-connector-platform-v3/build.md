## ✅ TASK-005: 连接流管理 API（实体 + 版本 + 编排）实现完成

### 修改的文件

#### 新创建文件 (14)
- `open-server/src/main/java/.../v2/modules/flow/FlowController.java` — 连接流实体 CRUD 控制器（#17~#27）
- `open-server/src/main/java/.../v2/modules/flow/FlowVersionController.java` — 连接流版本管理控制器（#28~#38）
- `open-server/src/main/java/.../v2/modules/flow/FlowService.java` — 连接流实体 CRUD + 生命周期管理服务
- `open-server/src/main/java/.../v2/modules/flow/FlowVersionService.java` — 连接流版本 CRUD + 发布流程 + 审批操作服务
- `open-server/src/main/java/.../v2/modules/flow/FlowDeployService.java` — 连接流部署服务（纯版本绑定）
- `open-server/src/main/java/.../v2/modules/flow/FlowCopyService.java` — 连接流复制服务（全版本历史复制）
- `open-server/src/main/java/.../v2/modules/flow/validator/FlowPublishValidator.java` — 发布校验器（9 项校验）
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowDeployRequest.java` — 部署请求 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowVersionSaveRequest.java` — 版本保存请求 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowVersionListResponse.java` — 版本列表响应 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowVersionDetailResponse.java` — 版本详情响应 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowPublishResponse.java` — 发布响应 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowCopyResponse.java` — 复制响应 DTO
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowDeployResponse.java` — 部署响应 DTO

#### 修改文件 (2)
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowListResponse.java` — 新增 V3 字段：deployedVersionId, deployedVersionNumber, appId, draftVersionNumber
- `open-server/src/main/java/.../v2/modules/flow/dto/FlowDetailResponse.java` — 新增 V3 字段：deployedVersionId, deployedVersionNumber, appId, invokeUrl, latestPublishedVersionNumber, draftVersionNumber, createBy, lastUpdateBy

### 实现的功能

#### 连接流实体 CRUD（#17~#27）
- [x] POST /flows — #17 创建连接流（lifecycleStatus=1 STOPPED，不自动生成草稿）
- [x] GET /flows — #18 列表查询（支持 lifecycleStatus/keyword 过滤 + 分页，按 appId 隔离）
- [x] GET /flows/{flowId} — #19 详情（含 invokeUrl + 已发布版本/草稿版本信息）
- [x] PUT /flows/{flowId} — #20 更新基本信息
- [x] POST /flows/{flowId}/copy — #21 复制（全版本历史，名称 + _copy_xxxxx）
- [x] POST /flows/{flowId}/deploy — #22 部署（纯版本绑定，不改变生命周期状态）
- [x] POST /flows/{flowId}/start — #23 启动（必须有已部署版本，状态 1→2）
- [x] POST /flows/{flowId}/stop — #24 停止（状态 2→1）
- [x] PUT /flows/{flowId}/invalidate — #25 失效（仅 STOPPED，状态 1→3）
- [x] PUT /flows/{flowId}/recover — #26 恢复 → STOPPED

#### 连接流版本管理（#28~#38）
- [x] POST /flows/{flowId}/versions — #28 创建空草稿（版本上限 1000，无已有草稿）
- [x] GET /flows/{flowId}/versions — #29 版本列表（含 deployed 标记，可按 status 过滤）
- [x] GET /flows/{flowId}/versions/{versionId} — #30 版本详情（含编排配置快照）
- [x] PUT /flows/{flowId}/versions/{versionId} — #31 更新草稿（DB 级校验：JSON 可解析） + 同步 connector_version_ref
- [x] POST /flows/{flowId}/versions/{versionId}/publish — #32 发布（全部 9 校验 → 提交审批 → PENDING_APPROVAL）
- [x] POST /flows/{flowId}/versions/{versionId}/copy-to-draft — #33 复制到草稿（校验无待审批/已驳回/已撤回版本）
- [x] PUT /flows/{flowId}/versions/{versionId}/invalidate — #34 失效版本（校验未部署）
- [x] PUT /flows/{flowId}/versions/{versionId}/recover — #35 恢复版本 → PUBLISHED
- [x] DELETE /flows/{flowId}/versions/{versionId} — #36 删除版本
- [x] POST /flows/{flowId}/versions/{versionId}/cancel — #37 撤回审批 → WITHDRAWN
- [x] POST /flows/{flowId}/versions/{versionId}/urge — #38 催办审批

#### 核心架构亮点
- [x] FlowPublishValidator — FR-026 全部 9 项发布校验（业务必填/编排非空/限流/超时/缓存 TTL/并行分支/连接器引用/JSON 语法/脚本语法）
- [x] connector_version_ref 中间表同步 — 保存编排时自动解析 connector 节点并维护引用关系
- [x] FlowVersionApprovalService 集成 — 发布提交审批、撤回、催办对接审批引擎
- [x] 应用数据隔离 — 所有接口通过 X-App-Id Header 校验归属
- [x] 状态流转校验 — 使用 FlowLifecycleStatus.isValidTransition() 和 FlowVersionStatus.isValidTransition()
- [x] 4 空格缩进，@Slf4j，@RequiredArgsConstructor，@Transactional

### 测试覆盖
- 单元测试：待后续任务创建（TASK-005 测试阶段）
- 编译验证：✅ BUILD SUCCESS — 241 source files compiled (15.9s)

### 下一步
- 运行 `@sddu-review connector-platform-v3 TASK-005` 审查当前实现
- 或继续 TASK-006: 安全准入拦截器
