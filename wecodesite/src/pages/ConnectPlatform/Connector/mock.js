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
    authType: 'oauth2',
    authConfig: {
      clientId: 'wx_corp_abc123',
      authUrl: 'https://qyapi.weixin.qq.com/cgi-bin/gettoken',
      scope: 'snsapi_base,snsapi_private_info'
    },
    credentials: {
      status: 'connected',
      lastConnected: '2024-03-15 10:30:00',
      expiresAt: '2024-03-15 11:30:00'
    },
    triggers: [
      {
        id: 'trigger-001',
        name: '收到消息',
        description: '当企业微信收到新消息时触发',
        type: 'webhook',
        config: {
          eventType: 'message.receive',
          webhookPath: '/webhook/wx/message'
        },
        inputSchema: [
          { paramName: 'fromUsername', paramType: 'string', description: '发送者用户名', required: true },
          { paramName: 'content', paramType: 'string', description: '消息内容', required: true },
          { paramName: 'createTime', paramType: 'number', description: '创建时间戳', required: true }
        ],
        outputSchema: [
          { paramName: 'messageId', paramType: 'string', description: '消息ID' },
          { paramName: 'fromUser', paramType: 'string', description: '发送者' },
          { paramName: 'messageContent', paramType: 'string', description: '消息内容' }
        ]
      },
      {
        id: 'trigger-002',
        name: '成员加入',
        description: '当有新成员加入企业微信时触发',
        type: 'webhook',
        config: {
          eventType: 'member.join',
          webhookPath: '/webhook/wx/member'
        },
        inputSchema: [
          { paramName: 'userId', paramType: 'string', description: '用户ID', required: true },
          { paramName: 'joinTime', paramType: 'number', description: '加入时间', required: true }
        ],
        outputSchema: [
          { paramName: 'memberId', paramType: 'string', description: '成员ID' },
          { paramName: 'memberName', paramType: 'string', description: '成员名称' },
          { paramName: 'departmentIds', paramType: 'array', description: '部门ID列表', children: [] }
        ]
      }
    ],
    actions: [
      {
        id: 'action-001',
        name: '发送消息',
        description: '向企业微信群或个人发送消息',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/message/send',
          timeout: 5000
        },
        inputSchema: [
          { paramName: 'toUser', paramType: 'string', description: '接收人用户名', required: true },
          { paramName: 'msgType', paramType: 'string', description: '消息类型', required: true },
          { paramName: 'content', paramType: 'string', description: '消息内容', required: true },
          { paramName: 'agentId', paramType: 'string', description: '应用AgentId' }
        ],
        outputSchema: [
          { paramName: 'errcode', paramType: 'number', description: '错误码' },
          { paramName: 'errmsg', paramType: 'string', description: '错误信息' },
          { paramName: 'msgid', paramType: 'string', description: '消息ID' }
        ]
      },
      {
        id: 'action-002',
        name: '创建群聊',
        description: '在企业微信中创建群聊',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/group/create',
          timeout: 8000
        },
        inputSchema: [
          { paramName: 'name', paramType: 'string', description: '群聊名称', required: true },
          { paramName: 'owner', paramType: 'string', description: '群主userId', required: true },
          { paramName: 'userList', paramType: 'array', description: '群成员userId列表', required: true, children: [] }
        ],
        outputSchema: [
          { paramName: 'errcode', paramType: 'number', description: '错误码' },
          { paramName: 'chatid', paramType: 'string', description: '群聊ID' }
        ]
      }
    ],
    createdAt: '2024-01-15 10:30:00',
    updatedAt: '2024-03-15 10:30:00',
  },
  {
    id: 'connector-002',
    name: '钉钉连接器',
    description: '用于与钉钉进行集成，支持工作通知、考勤等',
    status: 1,
    icon: '📱',
    authType: 'apiKey',
    authConfig: {
      apiKey: 'ding_***',
      baseUrl: 'https://oapi.dingtalk.com'
    },
    credentials: {
      status: 'connected',
      lastConnected: '2024-03-14 15:20:00',
      expiresAt: null
    },
    triggers: [
      {
        id: 'trigger-003',
        name: '工作通知',
        description: '收到钉钉工作通知时触发',
        type: 'webhook',
        config: {
          eventType: 'work.notify',
          webhookPath: '/webhook/dingtalk/notify'
        },
        inputSchema: [
          { paramName: 'msgId', paramType: 'string', description: '消息ID', required: true },
          { paramName: 'senderStaffId', paramType: 'string', description: '发送人员工ID', required: true },
          { paramName: 'msgContent', paramType: 'object', description: '消息内容', required: true, children: [] }
        ],
        outputSchema: [
          { paramName: 'messageId', paramType: 'string', description: '消息ID' },
          { paramName: 'senderId', paramType: 'string', description: '发送者ID' },
          { paramName: 'content', paramType: 'string', description: '消息内容' }
        ]
      }
    ],
    actions: [
      {
        id: 'action-003',
        name: '发送工作通知',
        description: '发送钉钉工作通知消息',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/message/work',
          timeout: 5000
        },
        inputSchema: [
          { paramName: 'userIds', paramType: 'array', description: '接收人员工ID列表', required: true, children: [] },
          { paramName: 'agentId', paramType: 'string', description: '应用AgentId', required: true },
          { paramName: 'content', paramType: 'string', description: '消息内容', required: true }
        ],
        outputSchema: [
          { paramName: 'errcode', paramType: 'number', description: '错误码' },
          { paramName: 'taskId', paramType: 'number', description: '任务ID' }
        ]
      },
      {
        id: 'action-004',
        name: '获取考勤数据',
        description: '获取钉钉考勤数据',
        type: 'api',
        config: {
          method: 'GET',
          endpoint: '/attendance/list',
          timeout: 10000
        },
        inputSchema: [
          { paramName: 'userIds', paramType: 'array', description: '员工ID列表', required: true, children: [] },
          { paramName: 'workDateFrom', paramType: 'string', description: '开始日期', required: true },
          { paramName: 'workDateTo', paramType: 'string', description: '结束日期', required: true }
        ],
        outputSchema: [
          { paramName: 'errcode', paramType: 'number', description: '错误码' },
          { paramName: 'attendanceList', paramType: 'array', description: '考勤记录列表', children: [] }
        ]
      }
    ],
    createdAt: '2024-01-18 09:20:00',
    updatedAt: '2024-03-14 15:20:00',
  },
  {
    id: 'connector-003',
    name: '飞书连接器',
    description: '用于与飞书进行集成，支持多维表格、消息、日历等',
    status: 1,
    icon: '🚀',
    authType: 'oauth2',
    authConfig: {
      clientId: 'cli_abc123456',
      authUrl: 'https://open.feishu.cn/open-apis/auth/v1/authorize',
      tokenUrl: 'https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal',
      scope: 'docx:document:readonly,im:message:send_as_bot,calendar:calendar:readonly'
    },
    credentials: {
      status: 'connected',
      lastConnected: '2024-03-15 09:00:00',
      expiresAt: '2024-03-15 10:00:00'
    },
    triggers: [
      {
        id: 'trigger-004',
        name: '文档更新',
        description: '当飞书文档更新时触发',
        type: 'webhook',
        config: {
          eventType: 'doc.update',
          webhookPath: '/webhook/feishu/doc'
        },
        inputSchema: [
          { paramName: 'docToken', paramType: 'string', description: '文档Token', required: true },
          { paramName: 'docTitle', paramType: 'string', description: '文档标题', required: true },
          { paramName: 'updateType', paramType: 'string', description: '更新类型', required: true }
        ],
        outputSchema: [
          { paramName: 'documentToken', paramType: 'string', description: '文档Token' },
          { paramName: 'documentTitle', paramType: 'string', description: '文档标题' },
          { paramName: 'updateUserId', paramType: 'string', description: '更新用户ID' }
        ]
      },
      {
        id: 'trigger-005',
        name: '日程变更',
        description: '当日历日程变更时触发',
        type: 'webhook',
        config: {
          eventType: 'calendar.update',
          webhookPath: '/webhook/feishu/calendar'
        },
        inputSchema: [
          { paramName: 'calendarId', paramType: 'string', description: '日历ID', required: true },
          { paramName: 'eventId', paramType: 'string', description: '日程ID', required: true },
          { paramName: 'changeType', paramType: 'string', description: '变更类型', required: true }
        ],
        outputSchema: [
          { paramName: 'calendarId', paramType: 'string', description: '日历ID' },
          { paramName: 'eventId', paramType: 'string', description: '日程ID' },
          { paramName: 'eventTitle', paramType: 'string', description: '日程标题' }
        ]
      }
    ],
    actions: [
      {
        id: 'action-005',
        name: '创建多维表格',
        description: '在飞书中创建多维表格',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/bitable/v1/apps',
          timeout: 10000
        },
        inputSchema: [
          { paramName: 'name', paramType: 'string', description: '多维表格名称', required: true },
          { paramName: 'folderToken', paramType: 'string', description: '文件夹Token' }
        ],
        outputSchema: [
          { paramName: 'code', paramType: 'number', description: '错误码' },
          { paramName: 'app', paramType: 'object', description: '创建的多维表格信息', children: [
            { paramName: 'name', paramType: 'string', description: '名称' },
            { paramName: 'appToken', paramType: 'string', description: 'App Token' }
          ]}
        ]
      },
      {
        id: 'action-006',
        name: '发送消息',
        description: '向飞书群或个人发送消息',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/im/v1/messages',
          timeout: 5000
        },
        inputSchema: [
          { paramName: 'receiveId', paramType: 'string', description: '接收者ID', required: true },
          { paramName: 'receiveIdType', paramType: 'string', description: '接收者ID类型', required: true },
          { paramName: 'msgType', paramType: 'string', description: '消息类型', required: true },
          { paramName: 'content', paramType: 'string', description: '消息内容', required: true }
        ],
        outputSchema: [
          { paramName: 'code', paramType: 'number', description: '错误码' },
          { paramName: 'msgId', paramType: 'string', description: '消息ID' }
        ]
      }
    ],
    createdAt: '2024-02-01 11:00:00',
    updatedAt: '2024-03-15 09:00:00',
  },
  {
    id: 'connector-004',
    name: '邮件服务连接器',
    description: '提供邮件发送和接收功能',
    status: 0,
    icon: '✉️',
    authType: 'basic',
    authConfig: {
      smtpHost: 'smtp.example.com',
      smtpPort: 587,
      imapHost: 'imap.example.com',
      imapPort: 993
    },
    credentials: {
      status: 'not_connected',
      lastConnected: null,
      expiresAt: null
    },
    triggers: [
      {
        id: 'trigger-006',
        name: '收到新邮件',
        description: '当收到新邮件时触发',
        type: 'webhook',
        config: {
          eventType: 'mail.receive',
          webhookPath: '/webhook/mail/receive'
        },
        inputSchema: [
          { paramName: 'messageId', paramType: 'string', description: '邮件Message-ID', required: true },
          { paramName: 'from', paramType: 'string', description: '发件人', required: true },
          { paramName: 'subject', paramType: 'string', description: '邮件主题', required: true }
        ],
        outputSchema: [
          { paramName: 'mailId', paramType: 'string', description: '邮件ID' },
          { paramName: 'fromAddress', paramType: 'string', description: '发件人地址' },
          { paramName: 'toAddress', paramType: 'string', description: '收件人地址' },
          { paramName: 'subject', paramType: 'string', description: '主题' },
          { paramName: 'body', paramType: 'string', description: '邮件正文' }
        ]
      }
    ],
    actions: [
      {
        id: 'action-007',
        name: '发送邮件',
        description: '发送普通邮件',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/mail/send',
          timeout: 10000
        },
        inputSchema: [
          { paramName: 'to', paramType: 'array', description: '收件人列表', required: true, children: [] },
          { paramName: 'cc', paramType: 'array', description: '抄送列表', children: [] },
          { paramName: 'subject', paramType: 'string', description: '邮件主题', required: true },
          { paramName: 'body', paramType: 'string', description: '邮件正文', required: true },
          { paramName: 'isHtml', paramType: 'boolean', description: '是否HTML格式' }
        ],
        outputSchema: [
          { paramName: 'success', paramType: 'boolean', description: '是否成功' },
          { paramName: 'messageId', paramType: 'string', description: '邮件Message-ID' }
        ]
      },
      {
        id: 'action-008',
        name: '发送附件邮件',
        description: '发送带附件的邮件',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/mail/send/attachment',
          timeout: 30000
        },
        inputSchema: [
          { paramName: 'to', paramType: 'array', description: '收件人列表', required: true, children: [] },
          { paramName: 'subject', paramType: 'string', description: '邮件主题', required: true },
          { paramName: 'body', paramType: 'string', description: '邮件正文', required: true },
          { paramName: 'attachments', paramType: 'array', description: '附件列表', required: true, children: [
            { paramName: 'filename', paramType: 'string', description: '文件名' },
            { paramName: 'url', paramType: 'string', description: '文件URL' },
            { paramName: 'size', paramType: 'number', description: '文件大小(字节)' }
          ]}
        ],
        outputSchema: [
          { paramName: 'success', paramType: 'boolean', description: '是否成功' },
          { paramName: 'messageId', paramType: 'string', description: '邮件Message-ID' }
        ]
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
    authType: 'bearer',
    authConfig: {
      host: 'db.example.com',
      port: 3306,
      database: 'production',
      ssl: true
    },
    credentials: {
      status: 'connected',
      lastConnected: '2024-03-15 08:00:00',
      expiresAt: null
    },
    triggers: [
      {
        id: 'trigger-007',
        name: '数据变更',
        description: '当数据库记录变更时触发',
        type: 'polling',
        config: {
          interval: 60000,
          table: 'users',
          pollingType: 'new_rows'
        },
        inputSchema: [
          { paramName: 'tableName', paramType: 'string', description: '表名', required: true },
          { paramName: 'lastPolledAt', paramType: 'number', description: '上次轮询时间', required: true }
        ],
        outputSchema: [
          { paramName: 'changedRows', paramType: 'array', description: '变更的行数据', children: [] },
          { paramName: 'changeType', paramType: 'string', description: '变更类型(insert/update/delete)' }
        ]
      }
    ],
    actions: [
      {
        id: 'action-009',
        name: '执行查询',
        description: '执行SQL查询语句',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/db/query',
          timeout: 30000
        },
        inputSchema: [
          { paramName: 'sql', paramType: 'string', description: 'SQL查询语句', required: true },
          { paramName: 'params', paramType: 'array', description: '查询参数', children: [] },
          { paramName: 'limit', paramType: 'number', description: '返回记录数限制' }
        ],
        outputSchema: [
          { paramName: 'success', paramType: 'boolean', description: '是否成功' },
          { paramName: 'data', paramType: 'array', description: '查询结果', children: [] },
          { paramName: 'rowCount', paramType: 'number', description: '结果行数' }
        ]
      },
      {
        id: 'action-010',
        name: '插入数据',
        description: '向数据库插入新记录',
        type: 'api',
        config: {
          method: 'POST',
          endpoint: '/db/insert',
          timeout: 10000
        },
        inputSchema: [
          { paramName: 'table', paramType: 'string', description: '表名', required: true },
          { paramName: 'data', paramType: 'object', description: '要插入的数据', required: true, children: [] }
        ],
        outputSchema: [
          { paramName: 'success', paramType: 'boolean', description: '是否成功' },
          { paramName: 'insertId', paramType: 'number', description: '插入记录的ID' },
          { paramName: 'affectedRows', paramType: 'number', description: '影响的行数' }
        ]
      }
    ],
    createdAt: '2024-02-15 08:50:00',
    updatedAt: '2024-03-15 08:00:00',
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
