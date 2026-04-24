/**
 * 审批管理 Mock 数据
 * 
 * v2.8.0 设计变更：
 * - 移除 isDefault、flowId、applicationNo 等旧字段
 * - 新增 type（审批类型）、businessType、businessId、businessName 字段
 * - 新增 combinedNodes（组合审批节点）、currentNode（当前节点索引）字段
 * - 新增 level（审批级别）字段：resource=资源审批, scene=场景审批, global=全局审批
 */

export const mockApprovals = [
  {
    id: '1',
    // v2.8.0: 新增审批类型字段
    type: 'api_permission_apply',  // 审批类型：api_permission_apply, event_permission_apply, callback_permission_apply, resource_register
    businessType: 'api_permission_apply',  // 业务类型
    businessId: '100',  // 业务对象ID（订阅记录ID或资源ID）
    businessName: '发送消息API',  // 业务名称（用于显示）
    applicantId: 'user001',
    applicantName: '张三',
    status: 0,  // 审批状态：0=待审, 1=已通过, 2=已拒绝, 3=已撤销
    currentNode: 0,  // 当前审批节点索引
    // v2.8.0: 组合审批节点（包含 level 字段）
    combinedNodes: [
      { type: 'approver', userId: 'payment_leader', userName: '支付团队负责人', order: 1, level: 'resource', status: null },
      { type: 'approver', userId: 'perm_admin', userName: '权限管理员', order: 2, level: 'scene', status: null },
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 3, level: 'global', status: null }
    ],
    logs: [],
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '2',
    type: 'event_permission_apply',
    businessType: 'event_permission_apply',
    businessId: '101',
    businessName: '用户状态变更事件',
    applicantId: 'user002',
    applicantName: '李四',
    status: 0,
    currentNode: 1,
    combinedNodes: [
      { type: 'approver', userId: 'event_admin', userName: '事件管理员', order: 1, level: 'scene', status: 1, approveTime: '2026-04-02 11:30:00', comment: '同意申请，事件权限符合规范' },
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 2, level: 'global', status: null }
    ],
    logs: [
      { 
        nodeIndex: 0, 
        level: 'scene', 
        operatorId: 'event_admin', 
        operatorName: '事件管理员', 
        action: 0,
        comment: '同意申请，事件权限符合规范',
        createTime: '2026-04-02 11:30:00'
      }
    ],
    createTime: '2026-04-02 10:00:00',
  },
  {
    id: '3',
    type: 'api_permission_apply',
    businessType: 'api_permission_apply',
    businessId: '102',
    businessName: '用户信息API',
    applicantId: 'user003',
    applicantName: '王五',
    status: 1,  // 已通过
    currentNode: 2,
    combinedNodes: [
      { type: 'approver', userId: 'api_admin', userName: 'API管理员', order: 1, level: 'scene', status: 1, approveTime: '2026-04-03 11:00:00', comment: '同意申请，API权限需求合理' },
      { type: 'approver', userId: 'admin002', userName: '平台管理员', order: 2, level: 'global', status: 1, approveTime: '2026-04-03 14:00:00', comment: '同意，平台权限已配置' }
    ],
    logs: [
      { 
        nodeIndex: 0, 
        level: 'scene', 
        operatorId: 'api_admin', 
        operatorName: 'API管理员', 
        action: 0,
        comment: '同意申请，API权限需求合理',
        createTime: '2026-04-03 11:00:00'
      },
      { 
        nodeIndex: 1, 
        level: 'global', 
        operatorId: 'admin002', 
        operatorName: '平台管理员', 
        action: 0,
        comment: '同意，平台权限已配置',
        createTime: '2026-04-03 14:00:00'
      }
    ],
    createTime: '2026-04-03 10:00:00',
  },
  {
    id: '4',
    type: 'resource_register',
    businessType: 'api_register',
    businessId: '200',
    businessName: '支付API注册申请',
    applicantId: 'provider001',
    applicantName: '资源提供方',
    status: 0,
    currentNode: 0,
    combinedNodes: [
      { type: 'approver', userId: 'api_reviewer', userName: 'API审核员', order: 1, level: 'scene', status: null },
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 2, level: 'global', status: null }
    ],
    logs: [],
    createTime: '2026-04-04 10:00:00',
  },
];

export const mockMyApprovals = [
  {
    id: '5',
    type: 'api_permission_apply',
    businessType: 'api_permission_apply',
    businessId: '103',
    businessName: '我的申请-订单查询API',
    applicantId: 'user001',
    applicantName: '张三',
    status: 1,  // 已通过
    currentNode: 3,
    combinedNodes: [
      { type: 'approver', userId: 'order_admin', userName: '订单管理员', order: 1, level: 'resource', status: 1, approveTime: '2026-04-01 11:00:00', comment: '同意申请，订单查询权限合理' },
      { type: 'approver', userId: 'perm_admin', userName: '权限管理员', order: 2, level: 'scene', status: 1, approveTime: '2026-04-01 14:00:00', comment: '同意，权限范围合理' },
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 3, level: 'global', status: 1, approveTime: '2026-04-01 16:00:00', comment: '同意，所有审批流程已完成' }
    ],
    logs: [
      { 
        nodeIndex: 0, 
        level: 'resource', 
        operatorId: 'order_admin', 
        operatorName: '订单管理员', 
        action: 0,
        comment: '同意申请，订单查询权限合理',
        createTime: '2026-04-01 11:00:00'
      },
      { 
        nodeIndex: 1, 
        level: 'scene', 
        operatorId: 'perm_admin', 
        operatorName: '权限管理员', 
        action: 0,
        comment: '同意，权限范围合理',
        createTime: '2026-04-01 14:00:00'
      },
      { 
        nodeIndex: 2, 
        level: 'global', 
        operatorId: 'admin001', 
        operatorName: '系统管理员', 
        action: 0,
        comment: '同意，所有审批流程已完成',
        createTime: '2026-04-01 16:00:00'
      }
    ],
    createTime: '2026-04-01 10:00:00',
  },
  {
    id: '6',
    type: 'callback_permission_apply',
    businessType: 'callback_permission_apply',
    businessId: '104',
    businessName: '我的申请-审批完成回调',
    applicantId: 'user001',
    applicantName: '张三',
    status: 2,  // 已拒绝
    currentNode: 1,
    combinedNodes: [
      { type: 'approver', userId: 'callback_admin', userName: '回调管理员', order: 1, level: 'scene', status: 2, approveTime: '2026-04-05 14:20:00', comment: '拒绝：回调地址不可访问，请检查服务状态' },
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 2, level: 'global', status: null }
    ],
    logs: [
      { 
        nodeIndex: 0, 
        level: 'scene', 
        operatorId: 'callback_admin', 
        operatorName: '回调管理员', 
        action: 1,
        comment: '拒绝：回调地址不可访问，请检查服务状态',
        createTime: '2026-04-05 14:20:00'
      }
    ],
    createTime: '2026-04-05 10:00:00',
  },
];

/**
 * 审批级别映射（用于显示审批节点级别）
 */
export const LEVEL_MAP = {
  'resource': { text: '资源审批', color: 'blue', desc: '资源提供方审核' },
  'scene': { text: '场景审批', color: 'orange', desc: '业务场景审核' },
  'global': { text: '全局审批', color: 'green', desc: '平台运营审核' },
};

/**
 * 审批类型映射（用于显示审批类型）
 */
export const TYPE_MAP = {
  'resource_register': '资源注册',
  'api_permission_apply': 'API权限申请',
  'event_permission_apply': '事件权限申请',
  'callback_permission_apply': '回调权限申请',
};

/**
 * 审批节点状态映射（用于显示审批节点处理状态）
 * 后端节点状态定义：0=待审批, 1=已通过, 2=已拒绝
 */
export const NODE_STATUS_MAP = {
  null: { text: '待审', color: 'default' },
  0: { text: '待审批', color: 'default' },
  1: { text: '已同意', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
};

// 审批流程模板 mock 数据
export const mockApprovalFlows = [
  {
    id: '1',
    nameCn: '全局审批流程',
    nameEn: 'Global Approval Flow',
    code: 'global',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'admin001', userName: '系统管理员', order: 1 }
    ]
  },
  {
    id: '2',
    nameCn: 'API权限申请审批',
    nameEn: 'API Permission Apply Approval',
    code: 'api_permission_apply',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'perm_admin', userName: '权限管理员', order: 1 }
    ]
  },
  {
    id: '3',
    nameCn: '事件权限申请审批',
    nameEn: 'Event Permission Apply Approval',
    code: 'event_permission_apply',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'perm_admin', userName: '权限管理员', order: 1 }
    ]
  },
  {
    id: '4',
    nameCn: '回调权限申请审批',
    nameEn: 'Callback Permission Apply Approval',
    code: 'callback_permission_apply',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'perm_admin', userName: '权限管理员', order: 1 }
    ]
  },
  {
    id: '5',
    nameCn: 'API注册审批',
    nameEn: 'API Register Approval',
    code: 'api_register',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'api_reviewer', userName: 'API审核员', order: 1 }
    ]
  },
  {
    id: '6',
    nameCn: '事件注册审批',
    nameEn: 'Event Register Approval',
    code: 'event_register',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'event_reviewer', userName: '事件审核员', order: 1 }
    ]
  },
  {
    id: '7',
    nameCn: '回调注册审批',
    nameEn: 'Callback Register Approval',
    code: 'callback_register',
    status: 1,
    nodes: [
      { type: 'approver', userId: 'callback_reviewer', userName: '回调审核员', order: 1 }
    ]
  }
];