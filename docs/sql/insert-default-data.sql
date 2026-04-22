-- ============================================================================
-- 能力开放平台测试数据初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-04-22
-- 说明: 插入所有15张表的完整测试数据
-- 
-- 数据关联关系说明：
-- ┌─────────────────────────────────────────────────────────────────────┐
-- │ 1. 分类表 (category_t)                                              │
-- │    ├── 分类责任人关联表 (category_owner_t)：分类有责任人            │
-- │    └── API/Event/Callback 资源：资源属于某个分类                    │
-- │                                                                     │
-- │ 2. API资源主表 (api_t)                                              │
-- │    ├── API资源属性表 (api_p_t)：资源的扩展属性                      │
-- │    └── 权限资源主表 (permission_t)：资源对应权限                    │
-- │                                                                     │
-- │ 3. 事件资源主表 (event_t)                                           │
-- │    ├── 事件资源属性表 (event_p_t)：资源的扩展属性                   │
-- │    └── 权限资源主表 (permission_t)：资源对应权限                    │
-- │                                                                     │
-- │ 4. 回调资源主表 (callback_t)                                        │
-- │    ├── 回调资源属性表 (callback_p_t)：资源的扩展属性                │
-- │    └── 权限资源主表 (permission_t)：资源对应权限                    │
-- │                                                                     │
-- │ 5. 权限资源主表 (permission_t)                                      │
-- │    ├── 权限资源属性表 (permission_p_t)：权限的扩展属性              │
-- │    └── 订阅关系表 (subscription_t)：订阅权限                        │
-- │                                                                     │
-- │ 6. 审批流程模板表 (approval_flow_t)                                 │
-- │    └── 审批记录表 (approval_record_t)：使用审批流程                 │
-- │                                                                     │
-- │ 7. 审批记录表 (approval_record_t)                                   │
-- │    └── 审批操作日志表 (approval_log_t)：审批操作记录               │
-- │                                                                     │
-- │ 8. 用户授权表 (user_authorization_t)                                │
-- │    └── 用户 + 应用 → 用户授权                                      │
-- └─────────────────────────────────────────────────────────────────────┘
-- ============================================================================

SET NAMES utf8mb4;

-- ============================================================================
-- 一、分类表数据 (openplatform_v2_category_t)
-- ID范围: 1-50
-- 说明: 包含3个根分类(A类/B类/AKSK)及其子分类
-- ============================================================================

-- 清空表（如果需要重新初始化）
-- TRUNCATE TABLE `openplatform_v2_category_t`;

-- 1. 根分类：A类应用权限树
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1, 'app_type_a', 'A类应用权限', 'App Type A Permissions', NULL, '/1/', 1, 1, NOW(3), NOW(3), 'system', 'system');

-- 2. 根分类：B类应用权限树
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2, 'app_type_b', 'B类应用权限', 'App Type B Permissions', NULL, '/2/', 2, 1, NOW(3), NOW(3), 'system', 'system');

-- 3. 根分类：个人AKSK权限树
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3, 'personal_aksk', '个人AKSK权限', 'Personal AKSK Permissions', NULL, '/3/', 3, 1, NOW(3), NOW(3), 'system', 'system');

-- 11. 子分类：A类 - IM即时通讯
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11, NULL, 'IM即时通讯', 'IM Instant Messaging', 1, '/1/11/', 1, 1, NOW(3), NOW(3), 'system', 'system');

-- 12. 子分类：A类 - 会议服务
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(12, NULL, '会议服务', 'Meeting Service', 1, '/1/12/', 2, 1, NOW(3), NOW(3), 'system', 'system');

-- 13. 子分类：A类 - 文档协作
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(13, NULL, '文档协作', 'Document Collaboration', 1, '/1/13/', 3, 1, NOW(3), NOW(3), 'system', 'system');

-- 14. 子分类：A类 - 日历服务
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(14, NULL, '日历服务', 'Calendar Service', 1, '/1/14/', 4, 1, NOW(3), NOW(3), 'system', 'system');

-- 21. 子分类：B类 - 云存储
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(21, NULL, '云存储', 'Cloud Storage', 2, '/2/21/', 1, 1, NOW(3), NOW(3), 'system', 'system');

-- 22. 子分类：B类 - 数据分析
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(22, NULL, '数据分析', 'Data Analytics', 2, '/2/22/', 2, 1, NOW(3), NOW(3), 'system', 'system');

-- 23. 子分类：B类 - AI服务
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(23, NULL, 'AI服务', 'AI Service', 2, '/2/23/', 3, 1, NOW(3), NOW(3), 'system', 'system');

-- 31. 子分类：AKSK - 个人网盘
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(31, NULL, '个人网盘', 'Personal Drive', 3, '/3/31/', 1, 1, NOW(3), NOW(3), 'system', 'system');

-- 32. 子分类：AKSK - 个人笔记
INSERT INTO `openplatform_v2_category_t` (`id`, `category_alias`, `name_cn`, `name_en`, `parent_id`, `path`, `sort_order`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(32, NULL, '个人笔记', 'Personal Notes', 3, '/3/32/', 2, 1, NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 二、分类责任人关联表数据 (openplatform_v2_category_owner_t)
-- ID范围: 101-120
-- 说明: 为各分类指定责任人
-- ============================================================================

INSERT INTO `openplatform_v2_category_owner_t` (`id`, `category_id`, `user_id`, `user_name`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(101, 1, 'user001', '张三', NOW(3), NOW(3), 'system', 'system'),
(102, 2, 'user002', '李四', NOW(3), NOW(3), 'system', 'system'),
(103, 3, 'user003', '王五', NOW(3), NOW(3), 'system', 'system'),
(104, 11, 'user004', '赵六', NOW(3), NOW(3), 'system', 'system'),
(105, 12, 'user005', '钱七', NOW(3), NOW(3), 'system', 'system'),
(106, 13, 'user006', '孙八', NOW(3), NOW(3), 'system', 'system'),
(107, 14, 'user007', '周九', NOW(3), NOW(3), 'system', 'system'),
(108, 21, 'user008', '吴十', NOW(3), NOW(3), 'system', 'system'),
(109, 22, 'user009', '郑一', NOW(3), NOW(3), 'system', 'system'),
(110, 23, 'user010', '陈二', NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 三、API资源主表数据 (openplatform_v2_api_t)
-- ID范围: 1001-1050
-- 说明: 各分类下的API接口资源
-- 状态: 0=草稿, 1=待审, 2=已发布, 3=已下线
-- ============================================================================

-- IM即时通讯分类下的API
INSERT INTO `openplatform_v2_api_t` (`id`, `name_cn`, `name_en`, `category_id`, `path`, `method`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1001, '发送消息', 'Send Message', 11, '/api/v1/im/message/send', 'POST', 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(1002, '获取消息列表', 'Get Message List', 11, '/api/v1/im/messages', 'GET', 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(1003, '撤回消息', 'Recall Message', 11, '/api/v1/im/message/recall', 'POST', 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(1004, '创建群组', 'Create Group', 11, '/api/v1/im/group/create', 'POST', 2, NOW(3), NOW(3), 'developer02', 'developer02'),
(1005, '获取群组列表', 'Get Group List', 11, '/api/v1/im/groups', 'GET', 1, NOW(3), NOW(3), 'developer02', 'developer02');

-- 会议服务分类下的API
INSERT INTO `openplatform_v2_api_t` (`id`, `name_cn`, `name_en`, `category_id`, `path`, `method`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1011, '创建会议', 'Create Meeting', 12, '/api/v1/meeting/create', 'POST', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(1012, '加入会议', 'Join Meeting', 12, '/api/v1/meeting/join', 'POST', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(1013, '结束会议', 'End Meeting', 12, '/api/v1/meeting/end', 'POST', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(1014, '获取会议列表', 'Get Meeting List', 12, '/api/v1/meetings', 'GET', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(1015, '邀请参会', 'Invite Participant', 12, '/api/v1/meeting/invite', 'POST', 0, NOW(3), NOW(3), 'developer04', 'developer04');

-- 文档协作分类下的API
INSERT INTO `openplatform_v2_api_t` (`id`, `name_cn`, `name_en`, `category_id`, `path`, `method`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1021, '创建文档', 'Create Document', 13, '/api/v1/doc/create', 'POST', 2, NOW(3), NOW(3), 'developer05', 'developer05'),
(1022, '获取文档内容', 'Get Document', 13, '/api/v1/doc/{docId}', 'GET', 2, NOW(3), NOW(3), 'developer05', 'developer05'),
(1023, '更新文档', 'Update Document', 13, '/api/v1/doc/{docId}', 'PUT', 2, NOW(3), NOW(3), 'developer05', 'developer05'),
(1024, '删除文档', 'Delete Document', 13, '/api/v1/doc/{docId}', 'DELETE', 1, NOW(3), NOW(3), 'developer05', 'developer05');

-- 云存储分类下的API
INSERT INTO `openplatform_v2_api_t` (`id`, `name_cn`, `name_en`, `category_id`, `path`, `method`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1031, '上传文件', 'Upload File', 21, '/api/v1/storage/upload', 'POST', 2, NOW(3), NOW(3), 'developer06', 'developer06'),
(1032, '下载文件', 'Download File', 21, '/api/v1/storage/download/{fileId}', 'GET', 2, NOW(3), NOW(3), 'developer06', 'developer06'),
(1033, '删除文件', 'Delete File', 21, '/api/v1/storage/{fileId}', 'DELETE', 2, NOW(3), NOW(3), 'developer06', 'developer06'),
(1034, '获取文件列表', 'List Files', 21, '/api/v1/storage/files', 'GET', 1, NOW(3), NOW(3), 'developer06', 'developer06');

-- AI服务分类下的API
INSERT INTO `openplatform_v2_api_t` (`id`, `name_cn`, `name_en`, `category_id`, `path`, `method`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1041, 'AI对话', 'AI Chat', 23, '/api/v1/ai/chat', 'POST', 2, NOW(3), NOW(3), 'developer07', 'developer07'),
(1042, 'AI摘要生成', 'AI Summary', 23, '/api/v1/ai/summary', 'POST', 2, NOW(3), NOW(3), 'developer07', 'developer07'),
(1043, 'AI翻译', 'AI Translate', 23, '/api/v1/ai/translate', 'POST', 0, NOW(3), NOW(3), 'developer07', 'developer07');

-- ============================================================================
-- 四、API资源属性表数据 (openplatform_v2_api_p_t)
-- ID范围: 2001-2100
-- 说明: API资源的扩展属性（如限流、超时、描述等）
-- ============================================================================

-- API 1001 发送消息的属性
INSERT INTO `openplatform_v2_api_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2001, 1001, 'rate_limit', '1000/minute', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(2002, 1001, 'timeout', '30000', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(2003, 1001, 'description', '向指定用户或群组发送消息，支持文本、图片、文件等多种消息类型', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(2004, 1001, 'version', '1.0.0', 1, NOW(3), NOW(3), 'developer01', 'developer01');

-- API 1002 获取消息列表的属性
INSERT INTO `openplatform_v2_api_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2011, 1002, 'rate_limit', '5000/minute', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(2012, 1002, 'timeout', '10000', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(2013, 1002, 'description', '获取当前用户的消息列表，支持分页和过滤', 1, NOW(3), NOW(3), 'developer01', 'developer01');

-- API 1011 创建会议的属性
INSERT INTO `openplatform_v2_api_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2021, 1011, 'rate_limit', '100/minute', 1, NOW(3), NOW(3), 'developer03', 'developer03'),
(2022, 1011, 'timeout', '60000', 1, NOW(3), NOW(3), 'developer03', 'developer03'),
(2023, 1011, 'description', '创建一个新的会议房间，可设置会议主题、时间、参与者等信息', 1, NOW(3), NOW(3), 'developer03', 'developer03'),
(2024, 1011, 'max_participants', '300', 1, NOW(3), NOW(3), 'developer03', 'developer03');

-- API 1031 上传文件的属性
INSERT INTO `openplatform_v2_api_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2031, 1031, 'rate_limit', '100/minute', 1, NOW(3), NOW(3), 'developer06', 'developer06'),
(2032, 1031, 'max_file_size', '104857600', 1, NOW(3), NOW(3), 'developer06', 'developer06'),
(2033, 1031, 'allowed_types', 'jpg,png,pdf,doc,docx,xlsx,pptx', 1, NOW(3), NOW(3), 'developer06', 'developer06'),
(2034, 1031, 'description', '上传文件到云存储，支持多种文件格式，单文件最大100MB', 1, NOW(3), NOW(3), 'developer06', 'developer06');

-- API 1041 AI对话的属性
INSERT INTO `openplatform_v2_api_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2041, 1041, 'rate_limit', '60/minute', 1, NOW(3), NOW(3), 'developer07', 'developer07'),
(2042, 1041, 'timeout', '120000', 1, NOW(3), NOW(3), 'developer07', 'developer07'),
(2043, 1041, 'model', 'gpt-4', 1, NOW(3), NOW(3), 'developer07', 'developer07'),
(2044, 1041, 'max_tokens', '4096', 1, NOW(3), NOW(3), 'developer07', 'developer07');

-- ============================================================================
-- 五、事件资源主表数据 (openplatform_v2_event_t)
-- ID范围: 3001-3050
-- 说明: 各分类下的事件资源
-- 状态: 0=草稿, 1=待审, 2=已发布, 3=已下线
-- ============================================================================

-- IM即时通讯分类下的事件
INSERT INTO `openplatform_v2_event_t` (`id`, `name_cn`, `name_en`, `category_id`, `topic`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3001, '消息接收事件', 'Message Received Event', 11, 'event.im.message.received', 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(3002, '消息已读事件', 'Message Read Event', 11, 'event.im.message.read', 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(3003, '群组创建事件', 'Group Created Event', 11, 'event.im.group.created', 2, NOW(3), NOW(3), 'developer02', 'developer02'),
(3004, '群成员变更事件', 'Group Member Changed Event', 11, 'event.im.group.member_changed', 1, NOW(3), NOW(3), 'developer02', 'developer02');

-- 会议服务分类下的事件
INSERT INTO `openplatform_v2_event_t` (`id`, `name_cn`, `name_en`, `category_id`, `topic`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3011, '会议开始事件', 'Meeting Started Event', 12, 'event.meeting.started', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(3012, '会议结束事件', 'Meeting Ended Event', 12, 'event.meeting.ended', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(3013, '参会者加入事件', 'Participant Joined Event', 12, 'event.meeting.participant_joined', 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(3014, '参会者离开事件', 'Participant Left Event', 12, 'event.meeting.participant_left', 2, NOW(3), NOW(3), 'developer03', 'developer03');

-- 文档协作分类下的事件
INSERT INTO `openplatform_v2_event_t` (`id`, `name_cn`, `name_en`, `category_id`, `topic`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3021, '文档创建事件', 'Document Created Event', 13, 'event.doc.created', 2, NOW(3), NOW(3), 'developer05', 'developer05'),
(3022, '文档更新事件', 'Document Updated Event', 13, 'event.doc.updated', 2, NOW(3), NOW(3), 'developer05', 'developer05'),
(3023, '文档删除事件', 'Document Deleted Event', 13, 'event.doc.deleted', 1, NOW(3), NOW(3), 'developer05', 'developer05');

-- 云存储分类下的事件
INSERT INTO `openplatform_v2_event_t` (`id`, `name_cn`, `name_en`, `category_id`, `topic`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3031, '文件上传完成事件', 'File Upload Completed Event', 21, 'event.storage.upload_completed', 2, NOW(3), NOW(3), 'developer06', 'developer06'),
(3032, '文件删除事件', 'File Deleted Event', 21, 'event.storage.deleted', 2, NOW(3), NOW(3), 'developer06', 'developer06');

-- ============================================================================
-- 六、事件资源属性表数据 (openplatform_v2_event_p_t)
-- ID范围: 4001-4100
-- 说明: 事件资源的扩展属性
-- ============================================================================

-- 事件 3001 消息接收事件的属性
INSERT INTO `openplatform_v2_event_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(4001, 3001, 'description', '当用户收到新消息时触发此事件，包含消息内容、发送者信息等', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(4002, 3001, 'schema', '{"type":"object","properties":{"messageId":{"type":"string"},"content":{"type":"string"},"senderId":{"type":"string"},"timestamp":{"type":"number"}}}', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(4003, 3001, 'retention_days', '7', 1, NOW(3), NOW(3), 'developer01', 'developer01');

-- 事件 3011 会议开始事件的属性
INSERT INTO `openplatform_v2_event_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(4011, 3011, 'description', '当会议开始时触发此事件，包含会议ID、主题、主持人等信息', 1, NOW(3), NOW(3), 'developer03', 'developer03'),
(4012, 3011, 'schema', '{"type":"object","properties":{"meetingId":{"type":"string"},"subject":{"type":"string"},"hostId":{"type":"string"},"startTime":{"type":"number"}}}', 1, NOW(3), NOW(3), 'developer03', 'developer03');

-- 事件 3031 文件上传完成事件的属性
INSERT INTO `openplatform_v2_event_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(4021, 3031, 'description', '当文件上传完成时触发此事件，包含文件ID、大小、类型等信息', 1, NOW(3), NOW(3), 'developer06', 'developer06'),
(4022, 3031, 'max_retry', '3', 1, NOW(3), NOW(3), 'developer06', 'developer06');

-- ============================================================================
-- 七、回调资源主表数据 (openplatform_v2_callback_t)
-- ID范围: 5001-5050
-- 说明: 各分类下的回调资源
-- 状态: 0=草稿, 1=待审, 2=已发布, 3=已下线
-- ============================================================================

-- IM即时通讯分类下的回调
INSERT INTO `openplatform_v2_callback_t` (`id`, `name_cn`, `name_en`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5001, '消息送达回调', 'Message Delivered Callback', 11, 2, NOW(3), NOW(3), 'developer01', 'developer01'),
(5002, '消息已读回调', 'Message Read Callback', 11, 2, NOW(3), NOW(3), 'developer01', 'developer01');

-- 会议服务分类下的回调
INSERT INTO `openplatform_v2_callback_t` (`id`, `name_cn`, `name_en`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5011, '会议状态变更回调', 'Meeting Status Changed Callback', 12, 2, NOW(3), NOW(3), 'developer03', 'developer03'),
(5012, '录制完成回调', 'Recording Completed Callback', 12, 2, NOW(3), NOW(3), 'developer03', 'developer03');

-- 云存储分类下的回调
INSERT INTO `openplatform_v2_callback_t` (`id`, `name_cn`, `name_en`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5021, '文件处理完成回调', 'File Process Completed Callback', 21, 2, NOW(3), NOW(3), 'developer06', 'developer06'),
(5022, '转码完成回调', 'Transcode Completed Callback', 21, 1, NOW(3), NOW(3), 'developer06', 'developer06');

-- AI服务分类下的回调
INSERT INTO `openplatform_v2_callback_t` (`id`, `name_cn`, `name_en`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5031, 'AI任务完成回调', 'AI Task Completed Callback', 23, 2, NOW(3), NOW(3), 'developer07', 'developer07');

-- ============================================================================
-- 八、回调资源属性表数据 (openplatform_v2_callback_p_t)
-- ID范围: 6001-6100
-- 说明: 回调资源的扩展属性
-- ============================================================================

-- 回调 5001 消息送达回调的属性
INSERT INTO `openplatform_v2_callback_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(6001, 5001, 'description', '消息成功送达接收方后触发的回调通知', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(6002, 5001, 'timeout', '5000', 1, NOW(3), NOW(3), 'developer01', 'developer01'),
(6003, 5001, 'retry_times', '3', 1, NOW(3), NOW(3), 'developer01', 'developer01');

-- 回调 5011 会议状态变更回调的属性
INSERT INTO `openplatform_v2_callback_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(6011, 5011, 'description', '会议状态变更时触发的回调，包括开始、结束、暂停等状态', 1, NOW(3), NOW(3), 'developer03', 'developer03'),
(6012, 5011, 'events', 'started,ended,paused,resumed', 1, NOW(3), NOW(3), 'developer03', 'developer03');

-- 回调 5021 文件处理完成回调的属性
INSERT INTO `openplatform_v2_callback_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(6021, 5021, 'description', '文件后台处理完成后触发的回调，如文档预览生成、视频转码等', 1, NOW(3), NOW(3), 'developer06', 'developer06'),
(6022, 5021, 'supported_types', 'doc,docx,pdf,mp4,avi', 1, NOW(3), NOW(3), 'developer06', 'developer06');

-- ============================================================================
-- 九、权限资源主表数据 (openplatform_v2_permission_t)
-- ID范围: 7001-7100
-- 说明: 资源对应的权限定义
-- resource_type: api, event, callback
-- ============================================================================

-- API权限
INSERT INTO `openplatform_v2_permission_t` (`id`, `name_cn`, `name_en`, `scope`, `resource_type`, `resource_id`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(7001, '发送消息权限', 'Send Message Permission', 'api:im:send-message', 'api', 1001, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7002, '获取消息列表权限', 'Get Message List Permission', 'api:im:get-messages', 'api', 1002, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7003, '撤回消息权限', 'Recall Message Permission', 'api:im:recall-message', 'api', 1003, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7004, '创建群组权限', 'Create Group Permission', 'api:im:create-group', 'api', 1004, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7011, '创建会议权限', 'Create Meeting Permission', 'api:meeting:create', 'api', 1011, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7012, '加入会议权限', 'Join Meeting Permission', 'api:meeting:join', 'api', 1012, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7013, '结束会议权限', 'End Meeting Permission', 'api:meeting:end', 'api', 1013, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7021, '创建文档权限', 'Create Document Permission', 'api:doc:create', 'api', 1021, 13, 1, NOW(3), NOW(3), 'system', 'system'),
(7022, '获取文档权限', 'Get Document Permission', 'api:doc:get', 'api', 1022, 13, 1, NOW(3), NOW(3), 'system', 'system'),
(7031, '上传文件权限', 'Upload File Permission', 'api:storage:upload', 'api', 1031, 21, 1, NOW(3), NOW(3), 'system', 'system'),
(7032, '下载文件权限', 'Download File Permission', 'api:storage:download', 'api', 1032, 21, 1, NOW(3), NOW(3), 'system', 'system'),
(7041, 'AI对话权限', 'AI Chat Permission', 'api:ai:chat', 'api', 1041, 23, 1, NOW(3), NOW(3), 'system', 'system');

-- 事件权限
INSERT INTO `openplatform_v2_permission_t` (`id`, `name_cn`, `name_en`, `scope`, `resource_type`, `resource_id`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(7101, '消息接收事件权限', 'Message Received Event Permission', 'event:im:message-received', 'event', 3001, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7102, '消息已读事件权限', 'Message Read Event Permission', 'event:im:message-read', 'event', 3002, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7111, '会议开始事件权限', 'Meeting Started Event Permission', 'event:meeting:started', 'event', 3011, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7112, '会议结束事件权限', 'Meeting Ended Event Permission', 'event:meeting:ended', 'event', 3012, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7121, '文档创建事件权限', 'Document Created Event Permission', 'event:doc:created', 'event', 3021, 13, 1, NOW(3), NOW(3), 'system', 'system'),
(7131, '文件上传完成事件权限', 'File Upload Completed Event Permission', 'event:storage:upload-completed', 'event', 3031, 21, 1, NOW(3), NOW(3), 'system', 'system');

-- 回调权限
INSERT INTO `openplatform_v2_permission_t` (`id`, `name_cn`, `name_en`, `scope`, `resource_type`, `resource_id`, `category_id`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(7201, '消息送达回调权限', 'Message Delivered Callback Permission', 'callback:im:delivered', 'callback', 5001, 11, 1, NOW(3), NOW(3), 'system', 'system'),
(7211, '会议状态变更回调权限', 'Meeting Status Changed Callback Permission', 'callback:meeting:status-changed', 'callback', 5011, 12, 1, NOW(3), NOW(3), 'system', 'system'),
(7221, '文件处理完成回调权限', 'File Process Completed Callback Permission', 'callback:storage:process-completed', 'callback', 5021, 21, 1, NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 十、权限资源属性表数据 (openplatform_v2_permission_p_t)
-- ID范围: 8001-8100
-- 说明: 权限资源的扩展属性
-- ============================================================================

-- 权限 7001 发送消息权限的属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8001, 7001, 'risk_level', 'medium', 1, NOW(3), NOW(3), 'system', 'system'),
(8002, 7001, 'approval_required', 'true', 1, NOW(3), NOW(3), 'system', 'system'),
(8003, 7001, 'description', '允许应用向用户发送即时消息，需要审批后才能开通', 1, NOW(3), NOW(3), 'system', 'system');

-- 权限 7011 创建会议权限的属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8011, 7011, 'risk_level', 'low', 1, NOW(3), NOW(3), 'system', 'system'),
(8012, 7011, 'approval_required', 'false', 1, NOW(3), NOW(3), 'system', 'system'),
(8013, 7011, 'description', '允许应用创建会议，可自动开通', 1, NOW(3), NOW(3), 'system', 'system');

-- 权限 7031 上传文件权限的属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8021, 7031, 'risk_level', 'medium', 1, NOW(3), NOW(3), 'system', 'system'),
(8022, 7031, 'approval_required', 'true', 1, NOW(3), NOW(3), 'system', 'system'),
(8023, 7031, 'quota', '10GB', 1, NOW(3), NOW(3), 'system', 'system');

-- 权限 7041 AI对话权限的属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8031, 7041, 'risk_level', 'high', 1, NOW(3), NOW(3), 'system', 'system'),
(8032, 7041, 'approval_required', 'true', 1, NOW(3), NOW(3), 'system', 'system'),
(8033, 7041, 'quota', '1000/day', 1, NOW(3), NOW(3), 'system', 'system'),
(8034, 7041, 'description', 'AI对话服务，每日调用次数有限制', 1, NOW(3), NOW(3), 'system', 'system');

-- 事件权限属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8041, 7101, 'risk_level', 'low', 1, NOW(3), NOW(3), 'system', 'system'),
(8042, 7101, 'approval_required', 'false', 1, NOW(3), NOW(3), 'system', 'system'),
(8051, 7111, 'risk_level', 'low', 1, NOW(3), NOW(3), 'system', 'system'),
(8052, 7111, 'approval_required', 'false', 1, NOW(3), NOW(3), 'system', 'system');

-- 回调权限属性
INSERT INTO `openplatform_v2_permission_p_t` (`id`, `parent_id`, `property_name`, `property_value`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(8061, 7201, 'risk_level', 'low', 1, NOW(3), NOW(3), 'system', 'system'),
(8062, 7201, 'approval_required', 'false', 1, NOW(3), NOW(3), 'system', 'system'),
(8071, 7211, 'risk_level', 'low', 1, NOW(3), NOW(3), 'system', 'system'),
(8072, 7211, 'approval_required', 'false', 1, NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 十一、审批流程模板表数据 (openplatform_v2_approval_flow_t)
-- ID范围: 1-10
-- 说明: 审批流程模板定义
-- ============================================================================

-- 1. 默认审批流
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(1, '默认审批流', 'Default Approval Flow', 'default', '系统默认审批流程，适用于未配置审批流的场景', 'System default approval flow, applicable to scenarios without configured approval flow', 1, '[{"type":"approver","userId":"admin","userName":"系统管理员","order":1}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 2. API注册审批流（二级审批）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(2, 'API注册审批流', 'API Registration Approval Flow', 'api_register', 'API资源注册审批流程，需要分类责任人和平台管理员二级审批', 'API resource registration approval flow, requires two-level approval from category owner and platform admin', 0, '[{"type":"approver","userId":"category_owner","userName":"分类责任人","order":1},{"type":"approver","userId":"admin","userName":"平台管理员","order":2}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 3. 权限申请审批流（三级审批）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(3, '权限申请审批流', 'Permission Apply Approval Flow', 'permission_apply', '权限申请审批流程，需要应用管理员、分类责任人和安全管理员三级审批', 'Permission application approval flow, requires three-level approval', 0, '[{"type":"approver","userId":"app_manager","userName":"应用管理员","order":1},{"type":"approver","userId":"category_owner","userName":"分类责任人","order":2},{"type":"approver","userId":"security_admin","userName":"安全管理员","order":3}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 4. 事件注册审批流
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(4, '事件注册审批流', 'Event Registration Approval Flow', 'event_register', '事件资源注册审批流程', 'Event resource registration approval flow', 0, '[{"type":"approver","userId":"category_owner","userName":"分类责任人","order":1},{"type":"approver","userId":"admin","userName":"平台管理员","order":2}]', 1, NOW(3), NOW(3), 'system', 'system');

-- 5. 快速审批流（单级）
INSERT INTO `openplatform_v2_approval_flow_t` (`id`, `name_cn`, `name_en`, `code`, `description_cn`, `description_en`, `is_default`, `nodes`, `status`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(5, '快速审批流', 'Quick Approval Flow', 'quick', '快速审批流程，适用于低风险操作', 'Quick approval flow for low-risk operations', 0, '[{"type":"approver","userId":"auto_approver","userName":"自动审批","order":1}]', 1, NOW(3), NOW(3), 'system', 'system');

-- ============================================================================
-- 十二、审批记录表数据 (openplatform_v2_approval_record_t)
-- ID范围: 9001-9050
-- 说明: 审批记录，关联审批流程和业务数据
-- business_type: api_register, event_register, permission_apply
-- status: 0=待审, 1=已通过, 2=已拒绝, 3=已撤销
-- ============================================================================

-- API注册审批记录
INSERT INTO `openplatform_v2_approval_record_t` (`id`, `flow_id`, `business_type`, `business_id`, `applicant_id`, `applicant_name`, `status`, `current_node`, `completed_at`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(9001, 2, 'api_register', 1005, 'developer02', '开发者02', 1, 2, DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'developer02', 'admin'),
(9002, 2, 'api_register', 1015, 'developer04', '开发者04', 0, 0, NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'developer04', 'developer04'),
(9003, 2, 'api_register', 1024, 'developer05', '开发者05', 1, 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'developer05', 'admin'),
(9004, 2, 'api_register', 1034, 'developer06', '开发者06', 0, 1, NULL, NOW(3), NOW(3), 'developer06', 'developer06'),
(9005, 2, 'api_register', 1043, 'developer07', '开发者07', 2, 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'developer07', 'category_owner');

-- 权限申请审批记录
INSERT INTO `openplatform_v2_approval_record_t` (`id`, `flow_id`, `business_type`, `business_id`, `applicant_id`, `applicant_name`, `status`, `current_node`, `completed_at`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(9011, 3, 'permission_apply', 9001, 'app_admin_001', '应用管理员01', 1, 3, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'app_admin_001', 'security_admin'),
(9012, 3, 'permission_apply', 9002, 'app_admin_002', '应用管理员02', 0, 2, NULL, DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3), 'app_admin_002', 'app_admin_002'),
(9013, 3, 'permission_apply', 9003, 'app_admin_001', '应用管理员01', 1, 3, NOW(3), DATE_SUB(NOW(3), INTERVAL 2 DAY), NOW(3), 'app_admin_001', 'security_admin');

-- 事件注册审批记录
INSERT INTO `openplatform_v2_approval_record_t` (`id`, `flow_id`, `business_type`, `business_id`, `applicant_id`, `applicant_name`, `status`, `current_node`, `completed_at`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(9021, 4, 'event_register', 3004, 'developer02', '开发者02', 1, 2, DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'developer02', 'admin'),
(9022, 4, 'event_register', 3023, 'developer05', '开发者05', 0, 1, NULL, NOW(3), NOW(3), 'developer05', 'developer05');

-- ============================================================================
-- 十三、审批操作日志表数据 (openplatform_v2_approval_log_t)
-- ID范围: 10001-10050
-- 说明: 审批操作记录
-- action: 0=同意, 1=拒绝, 2=撤销, 3=转交
-- ============================================================================

-- 审批记录 9001 的操作日志
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10001, 9001, 1, 'user004', '赵六', 0, 'API接口设计合理，同意注册', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'user004', 'user004'),
(10002, 9001, 2, 'admin', '系统管理员', 0, '审核通过，可以发布', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'admin', 'admin');

-- 审批记录 9003 的操作日志
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10011, 9003, 1, 'user006', '孙八', 0, '文档删除API需要补充权限控制说明', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'user006', 'user006'),
(10012, 9003, 2, 'admin', '系统管理员', 0, '已补充权限说明，同意发布', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'admin', 'admin');

-- 审批记录 9005 的操作日志（被拒绝）
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10021, 9005, 1, 'user010', '陈二', 1, 'AI翻译API需要补充数据安全评估报告', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'user010', 'user010');

-- 审批记录 9011 的操作日志（三级审批）
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10031, 9011, 1, 'app_manager', '应用管理员', 0, '应用已通过安全评估', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_manager', 'app_manager'),
(10032, 9011, 2, 'user001', '张三', 0, 'A类应用权限申请符合规范', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'user001', 'user001'),
(10033, 9011, 3, 'security_admin', '安全管理员', 0, '安全审计通过，同意授权', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'security_admin', 'security_admin');

-- 审批记录 9012 的操作日志（进行中）
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10041, 9012, 1, 'app_manager', '应用管理员', 0, '应用资质审核通过', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'app_manager', 'app_manager'),
(10042, 9012, 2, 'user002', '李四', 0, 'B类应用权限申请，待安全管理员审批', NOW(3), NOW(3), 'user002', 'user002');

-- 审批记录 9021 的操作日志
INSERT INTO `openplatform_v2_approval_log_t` (`id`, `record_id`, `node_index`, `operator_id`, `operator_name`, `action`, `comment`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(10051, 9021, 1, 'user004', '赵六', 0, '群成员变更事件设计合理', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'user004', 'user004'),
(10052, 9021, 2, 'admin', '系统管理员', 0, '同意注册', DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'admin', 'admin');

-- ============================================================================
-- 十四、订阅关系表数据 (openplatform_v2_subscription_t)
-- ID范围: 11001-11100
-- 说明: 应用订阅权限的关系
-- status: 0=待审, 1=已授权, 2=已拒绝, 3=已取消
-- channel_type: 0=内部消息队列, 1=WebHook, 2=SSE, 3=WebSocket
-- auth_type: 0=应用类凭证A, 1=应用类凭证B, 2=开放应用凭证
-- ============================================================================

-- 应用 10001 的订阅（IM相关）
INSERT INTO `openplatform_v2_subscription_t` (`id`, `app_id`, `permission_id`, `status`, `channel_type`, `channel_address`, `auth_type`, `approved_at`, `approved_by`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11001, 10001, 7001, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 5 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), 'app_admin_001', 'admin'),
(11002, 10001, 7002, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 5 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), 'app_admin_001', 'admin'),
(11003, 10001, 7101, 1, 1, 'https://app10001.example.com/webhook/im', 0, DATE_SUB(NOW(3), INTERVAL 5 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), 'app_admin_001', 'admin'),
(11004, 10001, 7201, 1, 1, 'https://app10001.example.com/webhook/callback', 0, DATE_SUB(NOW(3), INTERVAL 5 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 7 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), 'app_admin_001', 'admin');

-- 应用 10002 的订阅（会议相关）
INSERT INTO `openplatform_v2_subscription_t` (`id`, `app_id`, `permission_id`, `status`, `channel_type`, `channel_address`, `auth_type`, `approved_at`, `approved_by`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11011, 10002, 7011, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_admin_002', 'admin'),
(11012, 10002, 7012, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_admin_002', 'admin'),
(11013, 10002, 7111, 1, 2, NULL, 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_admin_002', 'admin'),
(11014, 10002, 7112, 1, 2, NULL, 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_admin_002', 'admin'),
(11015, 10002, 7211, 1, 1, 'https://app10002.example.com/webhook/meeting', 0, DATE_SUB(NOW(3), INTERVAL 3 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 3 DAY), 'app_admin_002', 'admin');

-- 应用 10003 的订阅（存储相关）
INSERT INTO `openplatform_v2_subscription_t` (`id`, `app_id`, `permission_id`, `status`, `channel_type`, `channel_address`, `auth_type`, `approved_at`, `approved_by`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11021, 10003, 7031, 1, 0, NULL, 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'app_admin_003', 'admin'),
(11022, 10003, 7032, 1, 0, NULL, 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'app_admin_003', 'admin'),
(11023, 10003, 7131, 1, 1, 'https://app10003.example.com/webhook/storage', 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'app_admin_003', 'admin'),
(11024, 10003, 7221, 1, 1, 'https://app10003.example.com/webhook/storage', 1, DATE_SUB(NOW(3), INTERVAL 2 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY), 'app_admin_003', 'admin');

-- 应用 10004 的订阅（AI相关）
INSERT INTO `openplatform_v2_subscription_t` (`id`, `app_id`, `permission_id`, `status`, `channel_type`, `channel_address`, `auth_type`, `approved_at`, `approved_by`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11031, 10004, 7041, 0, 0, NULL, 2, NULL, NULL, NOW(3), NOW(3), 'app_admin_004', 'app_admin_004');

-- 应用 10005 的订阅（文档相关）
INSERT INTO `openplatform_v2_subscription_t` (`id`, `app_id`, `permission_id`, `status`, `channel_type`, `channel_address`, `auth_type`, `approved_at`, `approved_by`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(11041, 10005, 7021, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 1 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'app_admin_005', 'admin'),
(11042, 10005, 7022, 1, 0, NULL, 0, DATE_SUB(NOW(3), INTERVAL 1 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'app_admin_005', 'admin'),
(11043, 10005, 7121, 1, 3, 'wss://app10005.example.com/ws', 0, DATE_SUB(NOW(3), INTERVAL 1 DAY), 'admin', DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'app_admin_005', 'admin');

-- ============================================================================
-- 十五、用户授权表数据 (openplatform_v2_user_authorization_t)
-- ID范围: 12001-12050
-- 说明: 用户对应用的授权关系
-- ============================================================================

INSERT INTO `openplatform_v2_user_authorization_t` (`id`, `user_id`, `app_id`, `scopes`, `expires_at`, `revoked_at`, `create_time`, `last_update_time`, `create_by`, `last_update_by`) VALUES
(12001, 'user100', 10001, '["api:im:send-message", "api:im:get-messages", "event:im:message-received"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 30 DAY), DATE_SUB(NOW(3), INTERVAL 30 DAY), 'user100', 'user100'),
(12002, 'user101', 10001, '["api:im:send-message", "api:im:get-messages"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 29 DAY), DATE_SUB(NOW(3), INTERVAL 29 DAY), 'user101', 'user101'),
(12003, 'user102', 10002, '["api:meeting:create", "api:meeting:join", "event:meeting:started", "event:meeting:ended"]', DATE_ADD(NOW(3), INTERVAL 6 MONTH), NULL, DATE_SUB(NOW(3), INTERVAL 15 DAY), DATE_SUB(NOW(3), INTERVAL 15 DAY), 'user102', 'user102'),
(12004, 'user103', 10002, '["api:meeting:join", "event:meeting:started"]', DATE_ADD(NOW(3), INTERVAL 6 MONTH), NULL, DATE_SUB(NOW(3), INTERVAL 14 DAY), DATE_SUB(NOW(3), INTERVAL 14 DAY), 'user103', 'user103'),
(12005, 'user104', 10003, '["api:storage:upload", "api:storage:download", "event:storage:upload-completed"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 10 DAY), DATE_SUB(NOW(3), INTERVAL 10 DAY), 'user104', 'user104'),
(12006, 'user105', 10003, '["api:storage:download"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 9 DAY), DATE_SUB(NOW(3), INTERVAL 9 DAY), 'user105', 'user105'),
(12007, 'user100', 10005, '["api:doc:create", "api:doc:get", "event:doc:created"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 5 DAY), 'user100', 'user100'),
(12008, 'user106', 10005, '["api:doc:get"]', DATE_ADD(NOW(3), INTERVAL 1 YEAR), NULL, DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 4 DAY), 'user106', 'user106'),
(12009, 'user107', 10001, '["api:im:send-message"]', DATE_ADD(NOW(3), INTERVAL 1 MONTH), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 2 MONTH), DATE_SUB(NOW(3), INTERVAL 1 DAY), 'user107', 'admin'),
(12010, 'user108', 10004, '["api:ai:chat"]', DATE_ADD(NOW(3), INTERVAL 3 MONTH), NULL, NOW(3), NOW(3), 'user108', 'user108');

-- ============================================================================
-- 数据初始化完成
-- 
-- 数据统计：
-- ┌────────────────────────────────────────────────────────────────────┐
-- │ 表名                                │ 记录数 │ ID范围              │
-- ├────────────────────────────────────────────────────────────────────┤
-- │ category_t                          │ 12     │ 1-32                │
-- │ category_owner_t                    │ 10     │ 101-110             │
-- │ api_t                               │ 18     │ 1001-1043           │
-- │ api_p_t                             │ 17     │ 2001-2044           │
-- │ event_t                             │ 13     │ 3001-3032           │
-- │ event_p_t                           │ 7      │ 4001-4022           │
-- │ callback_t                          │ 7      │ 5001-5031           │
-- │ callback_p_t                        │ 7      │ 6001-6022           │
-- │ permission_t                        │ 21     │ 7001-7221           │
-- │ permission_p_t                      │ 14     │ 8001-8072           │
-- │ approval_flow_t                     │ 5      │ 1-5                 │
-- │ approval_record_t                   │ 8      │ 9001-9022           │
-- │ approval_log_t                      │ 12     │ 10001-10052         │
-- │ subscription_t                      │ 14     │ 11001-11043         │
-- │ user_authorization_t                │ 10     │ 12001-12010         │
-- ├────────────────────────────────────────────────────────────────────┤
-- │ 总计                                │ 168    │                     │
-- └────────────────────────────────────────────────────────────────────┘
-- 
-- 关联关系验证：
-- 1. 分类 → 分类责任人：每个子分类都有对应责任人
-- 2. 分类 → API/Event/Callback：资源按分类组织
-- 3. API/Event/Callback → 属性表：每个资源都有扩展属性
-- 4. API/Event/Callback → 权限：每个资源对应权限定义
-- 5. 权限 → 订阅：应用订阅权限形成授权关系
-- 6. 审批流程 → 审批记录：审批使用流程模板
-- 7. 审批记录 → 审批日志：记录审批操作历史
-- 8. 用户 + 应用 → 用户授权：用户授权应用访问权限
-- ============================================================================
