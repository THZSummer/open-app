/**
 * ========================================
 * 连接器管理 - Mock数据
 * ========================================
 *
 * 提供连接器列表相关API的mock数据
 */

// 连接器Mock数据列表
export const mockConnectors = [
  {
    id: 'connector-001',
    name: '企业微信连接器',
    description: '用于与企业微信进行集成，支持消息推送、成员管理等',
    status: 1,
    icon: '💬',
    triggers: [
      {
        id: 'trigger-001',
        name: '收到消息',
        description: '当企业微信收到新消息时触发',
        type: 'webhook',
        config: { eventType: 'message.receive' }
      },
      {
        id: 'trigger-002',
        name: '成员加入',
        description: '当有新成员加入企业微信时触发',
        type: 'webhook',
        config: { eventType: 'member.join' }
      }
    ],
    actions: [
      {
        id: 'action-001',
        name: '发送消息',
        description: '向企业微信群或个人发送消息',
        type: 'api',
        config: { method: 'POST', endpoint: '/message/send' }
      },
      {
        id: 'action-002',
        name: '创建群聊',
        description: '在企业微信中创建群聊',
        type: 'api',
        config: { method: 'POST', endpoint: '/group/create' }
      }
    ],
    createdAt: '2024-01-15 10:30:00',
    updatedAt: '2024-02-20 14:25:00',
  },
  {
    id: 'connector-002',
    name: '钉钉连接器',
    description: '用于与钉钉进行集成，支持工作通知、考勤等',
    status: 1,
    icon: '📱',
    triggers: [
      {
        id: 'trigger-003',
        name: '工作通知',
        description: '收到钉钉工作通知时触发',
        type: 'webhook',
        config: { eventType: 'work.notify' }
      }
    ],
    actions: [
      {
        id: 'action-003',
        name: '发送工作通知',
        description: '发送钉钉工作通知消息',
        type: 'api',
        config: { method: 'POST', endpoint: '/message/work' }
      },
      {
        id: 'action-004',
        name: '获取考勤数据',
        description: '获取钉钉考勤数据',
        type: 'api',
        config: { method: 'GET', endpoint: '/attendance/list' }
      }
    ],
    createdAt: '2024-01-18 09:20:00',
    updatedAt: '2024-02-18 16:30:00',
  },
  {
    id: 'connector-003',
    name: '飞书连接器',
    description: '用于与飞书进行集成，支持多维表格、消息、日历等',
    status: 1,
    icon: '🚀',
    triggers: [
      {
        id: 'trigger-004',
        name: '文档更新',
        description: '当飞书文档更新时触发',
        type: 'webhook',
        config: { eventType: 'doc.update' }
      },
      {
        id: 'trigger-005',
        name: '日程变更',
        description: '当日历日程变更时触发',
        type: 'webhook',
        config: { eventType: 'calendar.update' }
      }
    ],
    actions: [
      {
        id: 'action-005',
        name: '创建多维表格',
        description: '在飞书中创建多维表格',
        type: 'api',
        config: { method: 'POST', endpoint: '/bitable/app' }
      },
      {
        id: 'action-006',
        name: '发送消息',
        description: '向飞书群或个人发送消息',
        type: 'api',
        config: { method: 'POST', endpoint: '/im/v1/messages' }
      }
    ],
    createdAt: '2024-02-01 11:00:00',
    updatedAt: '2024-03-05 10:15:00',
  },
  {
    id: 'connector-004',
    name: '邮件服务连接器',
    description: '提供邮件发送和接收功能',
    status: 0,
    icon: '✉️',
    triggers: [
      {
        id: 'trigger-006',
        name: '收到新邮件',
        description: '当收到新邮件时触发',
        type: 'webhook',
        config: { eventType: 'mail.receive' }
      }
    ],
    actions: [
      {
        id: 'action-007',
        name: '发送邮件',
        description: '发送普通邮件',
        type: 'api',
        config: { method: 'POST', endpoint: '/mail/send' }
      },
      {
        id: 'action-008',
        name: '发送附件邮件',
        description: '发送带附件的邮件',
        type: 'api',
        config: { method: 'POST', endpoint: '/mail/send/attachment' }
      }
    ],
    createdAt: '2024-02-10 15:40:00',
    updatedAt: '2024-03-01 09:30:00',
  },
  {
    id: 'connector-005',
    name: '数据库连接器',
    description: '支持MySQL、PostgreSQL等数据库的连接和操作',
    status: 1,
    icon: '🗄️',
    triggers: [
      {
        id: 'trigger-007',
        name: '数据变更',
        description: '当数据库记录变更时触发',
        type: 'polling',
        config: { interval: 60000, table: 'users' }
      }
    ],
    actions: [
      {
        id: 'action-009',
        name: '执行查询',
        description: '执行SQL查询语句',
        type: 'api',
        config: { method: 'POST', endpoint: '/db/query' }
      },
      {
        id: 'action-010',
        name: '插入数据',
        description: '向数据库插入新记录',
        type: 'api',
        config: { method: 'POST', endpoint: '/db/insert' }
      }
    ],
    createdAt: '2024-02-15 08:50:00',
    updatedAt: '2024-03-10 11:20:00',
  }
];

/**
 * 生成标准的成功响应
 */
const generateSuccessResponse = ({
  data,
  message = '操作成功',
  page
}) => {
  const response = { code: '200', message };
  if (page) {
    response.page = page;
  }
  if (data !== undefined) {
    response.data = data;
  }
  return response;
};

/**
 * 生成标准的错误响应
 */
const generateErrorResponse = ({
  code = '500',
  message = '操作失败',
}) => {
  return { code, message };
};

/**
 * 获取连接器列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {number} [params.curPage=1] - 当前页码
 * @param {number} [params.pageSize=10] - 每页条数
 * @param {number} [params.status] - 状态筛选
 * @returns {Promise<Object>}
 */
export const mockFetchConnectorList = async (params = {}) => {
  const {
    keyword,
    curPage = 1,
    pageSize = 10,
    status
  } = params;

  await new Promise(resolve => setTimeout(resolve, 300));

  let filteredList = [...mockConnectors];

  // 关键词过滤
  if (keyword) {
    const searchKeyword = keyword.toLowerCase();
    filteredList = filteredList.filter(
      item =>
        item.name.toLowerCase().includes(searchKeyword) ||
        item.description.toLowerCase().includes(searchKeyword)
    );
  }

  // 状态过滤
  if (status !== undefined && status !== '') {
    filteredList = filteredList.filter(item => item.status === Number(status));
  }

  // 分页计算
  const total = filteredList.length;
  const startIndex = (curPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const list = filteredList.slice(startIndex, endIndex);

  return generateSuccessResponse({
    data: list,
    page: {
      curPage,
      pageSize,
      total
    }
  });
};

/**
 * 删除连接器
 *
 * @param {string} id - 连接器ID
 * @returns {Promise<Object>}
 */
export const mockDeleteConnector = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 300));

  const index = mockConnectors.findIndex(item => item.id === id);

  if (index === -1) {
    return generateErrorResponse({
      code: '404',
      message: '连接器不存在'
    });
  }

  mockConnectors.splice(index, 1);

  return generateSuccessResponse({
    message: '连接器删除成功'
  });
};
