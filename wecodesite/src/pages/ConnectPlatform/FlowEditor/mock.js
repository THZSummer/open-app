/**
 * ========================================
 * 连接流编辑器 - Mock数据
 * ========================================
 *
 * 提供连接流编辑器相关API的mock数据，用于开发和测试阶段
 * 数据结构遵循API响应格式：{ code, message, data, page }
 */

// 连接流Mock数据列表
export const mockFlows = [
  {
    id: 'flow-001',
    name: '新用户注册流程',
    description: '当用户注册时，自动创建企业微信账号并发送欢迎消息',
    type: 'automation',
    status: 1,
    nodes: [
      {
        id: 'node-001',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          label: '用户注册触发',
          connectorId: 'connector-001',
          triggerId: 'trigger-001',
          config: {
            triggerType: 'webhook',
            webhookPath: '/webhook/register',
            enabled: true
          },
          requestSchema: [
            { paramName: 'username', paramType: 'string', description: '用户名', required: true, sourceType: 'static' },
            { paramName: 'email', paramType: 'string', description: '邮箱', required: true, sourceType: 'static' }
          ],
          responseSchema: [
            { paramName: 'userId', paramType: 'string', description: '用户ID' },
            { paramName: 'username', paramType: 'string', description: '用户名' },
            { paramName: 'email', paramType: 'string', description: '邮箱' }
          ]
        }
      },
      {
        id: 'node-002',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          label: '创建企业微信账号',
          connectorId: 'connector-001',
          actionId: 'action-002',
          config: {
            connectorId: 'connector-001',
            actionId: 'action-002',
            timeout: 8000,
            errorHandling: 'retry',
            retryConfig: {
              maxRetries: 3,
              retryDelay: 1000,
              backoffMultiplier: 2
            },
            inputMapping: {
              name: '{{trigger.output.username}}',
              owner: 'admin001'
            }
          },
          inputMapping: [
            { paramName: 'name', paramType: 'string', sourceType: 'reference', referencePath: 'node-001.response.username', description: '群聊名称' },
            { paramName: 'owner', paramType: 'string', sourceType: 'static', paramValue: 'admin001', description: '群主userId' }
          ]
        }
      },
      {
        id: 'node-003',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          label: '发送欢迎消息',
          connectorId: 'connector-001',
          actionId: 'action-001',
          config: {
            connectorId: 'connector-001',
            actionId: 'action-001',
            timeout: 5000,
            errorHandling: 'throw',
            retryConfig: {
              maxRetries: 0
            }
          },
          inputMapping: [
            { paramName: 'toUser', paramType: 'string', sourceType: 'reference', referencePath: 'node-001.response.userId', description: '接收人用户名' },
            { paramName: 'msgType', paramType: 'string', sourceType: 'static', paramValue: 'text', description: '消息类型' },
            { paramName: 'content', paramType: 'string', sourceType: 'static', paramValue: '欢迎加入！', description: '消息内容' }
          ]
        }
      }
    ],
    edges: [
      {
        id: 'edge-001',
        source: 'node-001',
        target: 'node-002'
      },
      {
        id: 'edge-002',
        source: 'node-002',
        target: 'node-003'
      }
    ],
    createdAt: '2024-01-20 10:00:00',
    updatedAt: '2024-03-15 14:30:00',
    publishedAt: '2024-02-25 14:30:00',
  },
  {
    id: 'flow-002',
    name: '审批通知流程',
    description: '当有新的待审批项时，通过钉钉发送通知给审批人',
    type: 'notification',
    status: 1,
    nodes: [
      {
        id: 'node-004',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          label: '审批创建触发',
          connectorId: 'connector-002',
          triggerId: 'trigger-003',
          config: {
            triggerType: 'webhook',
            webhookPath: '/webhook/approval',
            enabled: true
          },
          requestSchema: [
            { paramName: 'approvalId', paramType: 'string', description: '审批ID', required: true, sourceType: 'static' },
            { paramName: 'approvalTitle', paramType: 'string', description: '审批标题', required: true, sourceType: 'static' }
          ],
          responseSchema: [
            { paramName: 'approvalId', paramType: 'string', description: '审批ID' },
            { paramName: 'title', paramType: 'string', description: '审批标题' },
            { paramName: 'approverId', paramType: 'string', description: '审批人ID' }
          ]
        }
      },
      {
        id: 'node-005',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          label: '获取审批人信息',
          connectorId: 'connector-002',
          actionId: 'action-004',
          config: {
            connectorId: 'connector-002',
            actionId: 'action-004',
            timeout: 10000,
            errorHandling: 'continue'
          },
          inputMapping: [
            { paramName: 'userIds', paramType: 'array', sourceType: 'reference', referencePath: 'node-004.response.approverId', description: '员工ID列表', children: [] }
          ]
        }
      },
      {
        id: 'node-006',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          label: '发送钉钉通知',
          connectorId: 'connector-002',
          actionId: 'action-003',
          config: {
            connectorId: 'connector-002',
            actionId: 'action-003',
            timeout: 5000,
            errorHandling: 'throw'
          },
          inputMapping: [
            { paramName: 'userIds', paramType: 'array', sourceType: 'reference', referencePath: 'node-004.response.approverId', description: '接收人员工ID列表', children: [] },
            { paramName: 'agentId', paramType: 'string', sourceType: 'static', paramValue: '1000001', description: '应用AgentId' },
            { paramName: 'content', paramType: 'string', sourceType: 'reference', referencePath: 'node-004.response.title', description: '消息内容' }
          ]
        }
      }
    ],
    edges: [
      {
        id: 'edge-003',
        source: 'node-004',
        target: 'node-005'
      },
      {
        id: 'edge-004',
        source: 'node-005',
        target: 'node-006'
      }
    ],
    createdAt: '2024-01-22 11:30:00',
    updatedAt: '2024-03-15 09:15:00',
    publishedAt: '2024-03-01 09:15:00',
  },
  {
    id: 'flow-003',
    name: '文档同步流程',
    description: '将飞书文档变更同步到数据库中',
    type: 'sync',
    status: 0,
    nodes: [
      {
        id: 'node-007',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          label: '飞书文档更新触发',
          connectorId: 'connector-003',
          triggerId: 'trigger-004',
          config: {
            triggerType: 'webhook',
            webhookPath: '/webhook/feishu/doc',
            enabled: true
          },
          requestSchema: [],
          responseSchema: [
            { paramName: 'docToken', paramType: 'string', description: '文档Token' },
            { paramName: 'docTitle', paramType: 'string', description: '文档标题' },
            { paramName: 'updateTime', paramType: 'number', description: '更新时间戳' }
          ]
        }
      },
      {
        id: 'node-008',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          label: '获取文档内容',
          connectorId: 'connector-003',
          actionId: 'action-005',
          config: {
            connectorId: 'connector-003',
            actionId: 'action-005',
            timeout: 10000,
            errorHandling: 'skip'
          },
          inputMapping: [
            { paramName: 'name', paramType: 'string', sourceType: 'reference', referencePath: 'node-007.response.docTitle', description: '多维表格名称' }
          ]
        }
      },
      {
        id: 'node-009',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          label: '更新数据库',
          connectorId: 'connector-005',
          actionId: 'action-010',
          config: {
            connectorId: 'connector-005',
            actionId: 'action-010',
            timeout: 10000,
            errorHandling: 'throw'
          },
          inputMapping: [
            { paramName: 'table', paramType: 'string', sourceType: 'static', paramValue: 'documents', description: '表名' },
            { paramName: 'data', paramType: 'object', sourceType: 'reference', referencePath: 'node-008.output.data', description: '要插入的数据', children: [] }
          ]
        }
      }
    ],
    edges: [
      {
        id: 'edge-005',
        source: 'node-007',
        target: 'node-008'
      },
      {
        id: 'edge-006',
        source: 'node-008',
        target: 'node-009'
      }
    ],
    createdAt: '2024-02-05 15:20:00',
    updatedAt: '2024-03-15 10:45:00',
  },
  {
    id: 'flow-004',
    name: '邮件归档流程',
    description: '将收到的邮件自动归档到数据库',
    type: 'automation',
    status: 1,
    nodes: [
      {
        id: 'node-010',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          label: '新邮件触发',
          connectorId: 'connector-004',
          triggerId: 'trigger-006',
          config: {
            triggerType: 'webhook',
            webhookPath: '/webhook/mail/receive',
            enabled: true
          },
          requestSchema: [],
          responseSchema: [
            { paramName: 'mailId', paramType: 'string', description: '邮件ID' },
            { paramName: 'fromAddress', paramType: 'string', description: '发件人地址' },
            { paramName: 'toAddress', paramType: 'string', description: '收件人地址' },
            { paramName: 'subject', paramType: 'string', description: '主题' },
            { paramName: 'body', paramType: 'string', description: '邮件正文' }
          ]
        }
      },
      {
        id: 'node-011',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          label: '提取邮件内容',
          connectorId: 'connector-004',
          actionId: 'action-007',
          config: {
            connectorId: 'connector-004',
            actionId: 'action-007',
            timeout: 10000,
            errorHandling: 'retry',
            retryConfig: {
              maxRetries: 2,
              retryDelay: 2000
            }
          },
          inputMapping: [
            { paramName: 'to', paramType: 'array', sourceType: 'reference', referencePath: 'node-010.response.toAddress', description: '收件人列表', children: [] },
            { paramName: 'subject', paramType: 'string', sourceType: 'reference', referencePath: 'node-010.response.subject', description: '邮件主题' }
          ]
        }
      },
      {
        id: 'node-012',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          label: '写入数据库',
          connectorId: 'connector-005',
          actionId: 'action-010',
          config: {
            connectorId: 'connector-005',
            actionId: 'action-010',
            timeout: 10000,
            errorHandling: 'throw'
          },
          inputMapping: [
            { paramName: 'table', paramType: 'string', sourceType: 'static', paramValue: 'email_archive', description: '表名' },
            { paramName: 'data', paramType: 'object', sourceType: 'reference', referencePath: 'node-010.response', description: '邮件数据', children: [
              { paramName: 'mailId', paramType: 'string', sourceType: 'reference', referencePath: 'node-010.response.mailId', description: '邮件ID' },
              { paramName: 'subject', paramType: 'string', sourceType: 'reference', referencePath: 'node-010.response.subject', description: '主题' },
              { paramName: 'body', paramType: 'string', sourceType: 'reference', referencePath: 'node-010.response.body', description: '正文' }
            ]}
          ]
        }
      }
    ],
    edges: [
      {
        id: 'edge-007',
        source: 'node-010',
        target: 'node-011'
      },
      {
        id: 'edge-008',
        source: 'node-011',
        target: 'node-012'
      }
    ],
    createdAt: '2024-02-08 09:00:00',
    updatedAt: '2024-03-15 16:20:00',
    publishedAt: '2024-02-28 16:20:00',
  },
  {
    id: 'flow-005',
    name: '数据统计报告',
    description: '每天定时统计数据库数据并发送邮件报告',
    type: 'scheduled',
    status: 0,
    nodes: [
      {
        id: 'node-013',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          label: '定时触发',
          connectorId: 'connector-005',
          triggerId: 'trigger-007',
          config: {
            triggerType: 'schedule',
            cronExpression: '0 0 8 * * ?',
            timezone: 'Asia/Shanghai',
            enabled: true
          },
          requestSchema: [],
          responseSchema: [
            { paramName: 'executionTime', paramType: 'number', description: '执行时间戳' },
            { paramName: 'triggerType', paramType: 'string', description: '触发类型' }
          ]
        }
      },
      {
        id: 'node-014',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          label: '查询统计数据',
          connectorId: 'connector-005',
          actionId: 'action-009',
          config: {
            connectorId: 'connector-005',
            actionId: 'action-009',
            timeout: 30000,
            errorHandling: 'throw'
          },
          inputMapping: [
            { paramName: 'sql', paramType: 'string', sourceType: 'static', paramValue: 'SELECT COUNT(*) as total FROM users', description: 'SQL查询语句' },
            { paramName: 'limit', paramType: 'number', sourceType: 'static', paramValue: '100', description: '返回记录数限制' }
          ]
        }
      },
      {
        id: 'node-015',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          label: '发送邮件报告',
          connectorId: 'connector-004',
          actionId: 'action-008',
          config: {
            connectorId: 'connector-004',
            actionId: 'action-008',
            timeout: 30000,
            errorHandling: 'retry',
            retryConfig: {
              maxRetries: 3,
              retryDelay: 5000
            }
          },
          inputMapping: [
            { paramName: 'to', paramType: 'array', sourceType: 'static', paramValue: '["admin@example.com"]', description: '收件人列表', children: [] },
            { paramName: 'subject', paramType: 'string', sourceType: 'static', paramValue: '每日数据统计报告', description: '邮件主题' },
            { paramName: 'body', paramType: 'string', sourceType: 'reference', referencePath: 'node-014.output.data', description: '邮件正文' },
            { paramName: 'attachments', paramType: 'array', sourceType: 'static', paramValue: '[]', description: '附件列表', children: [
              { paramName: 'filename', paramType: 'string', description: '文件名' },
              { paramName: 'url', paramType: 'string', description: '文件URL' }
            ]}
          ]
        }
      }
    ],
    edges: [
      {
        id: 'edge-009',
        source: 'node-013',
        target: 'node-014'
      },
      {
        id: 'edge-010',
        source: 'node-014',
        target: 'node-015'
      }
    ],
    createdAt: '2024-02-12 14:00:00',
    updatedAt: '2024-03-15 11:30:00',
  }
];

/**
 * 生成标准的成功响应
 *
 * @param {Object} options - 响应配置
 * @param {Object} options.data - 响应数据
 * @param {string} [options.message='操作成功'] - 响应消息
 * @param {Object} [options.page] - 分页信息
 * @returns {Object}
 */
const generateSuccessResponse = ({
  data,
  message = '操作成功',
  page
}) => {
  const response = {
    code: '200',
    message
  };

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
 *
 * @param {Object} options - 响应配置
 * @param {string} [options.code='500'] - 错误码
 * @param {string} [options.message='操作失败'] - 错误消息
 * @returns {Object}
 */
const generateErrorResponse = ({
  code = '500',
  message = '操作失败'
}) => {
  return { code, message };
};

/**
 * 获取连接流详情
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const mockFetchFlowDetail = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 200));

  const flow = mockFlows.find(item => item.id === id);

  if (!flow) {
    return generateErrorResponse({
      code: '404',
      message: '连接流不存在'
    });
  }

  return generateSuccessResponse({
    data: flow
  });
};

/**
 * 创建连接流
 *
 * @param {Object} data - 连接流数据
 * @param {string} data.name - 连接流名称
 * @param {string} [data.description] - 连接流描述
 * @param {string} [data.type='automation'] - 流程类型
 * @param {number} [data.status=0] - 状态（0-草稿，1-已发布）
 * @param {Array} [data.nodes] - 节点列表
 * @param {Array} [data.edges] - 连线列表
 * @returns {Promise<Object>}
 */
export const mockCreateFlow = async (data) => {
  await new Promise(resolve => setTimeout(resolve, 400));

  const newFlow = {
    id: `flow-${Date.now()}`,
    name: data.name,
    description: data.description || '',
    type: data.type || 'automation',
    status: 0,
    nodes: data.nodes || [],
    edges: data.edges || [],
    createdAt: new Date().toLocaleString(),
    updatedAt: new Date().toLocaleString()
  };

  mockFlows.push(newFlow);

  return generateSuccessResponse({
    data: newFlow,
    message: '连接流创建成功'
  });
};

/**
 * 更新连接流
 *
 * @param {string} id - 连接流ID
 * @param {Object} data - 更新数据
 * @returns {Promise<Object>}
 */
export const mockUpdateFlow = async (id, data) => {
  await new Promise(resolve => setTimeout(resolve, 300));

  const index = mockFlows.findIndex(item => item.id === id);

  if (index === -1) {
    return generateErrorResponse({
      code: '404',
      message: '连接流不存在'
    });
  }

  mockFlows[index] = {
    ...mockFlows[index],
    ...data,
    updatedAt: new Date().toLocaleString()
  };

  return generateSuccessResponse({
    data: mockFlows[index],
    message: '连接流更新成功'
  });
};

/**
 * 发布连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const mockPublishFlow = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 400));

  const flow = mockFlows.find(item => item.id === id);

  if (!flow) {
    return generateErrorResponse({
      code: '404',
      message: '连接流不存在'
    });
  }

  if (flow.status === 1) {
    return generateErrorResponse({
      code: '400',
      message: '连接流已经发布，不能重复发布'
    });
  }

  flow.status = 1;
  flow.publishedAt = new Date().toLocaleString();
  flow.updatedAt = new Date().toLocaleString();

  return generateSuccessResponse({
    data: flow,
    message: '连接流发布成功'
  });
};

/**
 * 取消发布连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const mockUnpublishFlow = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 400));

  const flow = mockFlows.find(item => item.id === id);

  if (!flow) {
    return generateErrorResponse({
      code: '404',
      message: '连接流不存在'
    });
  }

  if (flow.status === 0) {
    return generateErrorResponse({
      code: '400',
      message: '连接流未发布，无法取消发布'
    });
  }

  flow.status = 0;
  flow.updatedAt = new Date().toLocaleString();
  delete flow.publishedAt;

  return generateSuccessResponse({
    data: flow,
    message: '连接流取消发布成功'
  });
};

/**
 * ========================================
 * 连接流执行历史Mock数据
 * ========================================
 */

export const mockExecutionHistory = [
  {
    id: 'exec-001',
    flowId: 'flow-001',
    flowName: '新用户注册流程',
    status: 'success',
    startTime: '2024-03-15 10:30:00',
    endTime: '2024-03-15 10:30:05',
    duration: 5000,
    triggerType: 'webhook',
    triggerSource: '/webhook/register',
    inputData: {
      username: 'testuser001',
      email: 'test@example.com'
    },
    outputData: {
      userId: 'user-12345',
      status: 'created'
    },
    nodeExecutions: [
      {
        nodeId: 'node-001',
        nodeName: '用户注册触发',
        status: 'success',
        startTime: '2024-03-15 10:30:00',
        endTime: '2024-03-15 10:30:01',
        duration: 1000,
        input: { username: 'testuser001', email: 'test@example.com' },
        output: { userId: 'user-12345', username: 'testuser001' },
        logs: [
          { level: 'info', message: '触发器接收到注册请求', timestamp: '2024-03-15 10:30:00.100' },
          { level: 'info', message: '开始处理注册流程', timestamp: '2024-03-15 10:30:00.500' },
          { level: 'success', message: '触发器执行成功', timestamp: '2024-03-15 10:30:01.000' }
        ]
      },
      {
        nodeId: 'node-002',
        nodeName: '创建企业微信账号',
        status: 'success',
        startTime: '2024-03-15 10:30:01',
        endTime: '2024-03-15 10:30:03',
        duration: 2000,
        input: { name: 'testuser001', owner: 'admin001' },
        output: { errcode: 0, errmsg: 'ok', chatid: 'wx_group_123' },
        logs: [
          { level: 'info', message: '开始创建企业微信群聊', timestamp: '2024-03-15 10:30:01.200' },
          { level: 'info', message: '调用企业微信API: POST /group/create', timestamp: '2024-03-15 10:30:01.500' },
          { level: 'success', message: '群聊创建成功，chatid: wx_group_123', timestamp: '2024-03-15 10:30:03.000' }
        ]
      },
      {
        nodeId: 'node-003',
        nodeName: '发送欢迎消息',
        status: 'success',
        startTime: '2024-03-15 10:30:03',
        endTime: '2024-03-15 10:30:05',
        duration: 2000,
        input: { toUser: 'testuser001', msgType: 'text', content: '欢迎加入！' },
        output: { errcode: 0, errmsg: 'ok', msgid: 'msg_123456' },
        logs: [
          { level: 'info', message: '准备发送欢迎消息', timestamp: '2024-03-15 10:30:03.100' },
          { level: 'info', message: '调用企业微信API: POST /message/send', timestamp: '2024-03-15 10:30:03.300' },
          { level: 'success', message: '消息发送成功，msgid: msg_123456', timestamp: '2024-03-15 10:30:05.000' }
        ]
      }
    ]
  },
  {
    id: 'exec-002',
    flowId: 'flow-001',
    flowName: '新用户注册流程',
    status: 'failed',
    startTime: '2024-03-15 09:15:00',
    endTime: '2024-03-15 09:15:03',
    duration: 3000,
    triggerType: 'webhook',
    triggerSource: '/webhook/register',
    inputData: {
      username: 'testuser002',
      email: 'test2@example.com'
    },
    outputData: null,
    error: {
      code: 'WECHAT_API_ERROR',
      message: '企业微信API调用失败',
      details: '群聊创建接口返回错误: errcode=40013, errmsg=invalid chatid'
    },
    nodeExecutions: [
      {
        nodeId: 'node-001',
        nodeName: '用户注册触发',
        status: 'success',
        startTime: '2024-03-15 09:15:00',
        endTime: '2024-03-15 09:15:01',
        duration: 1000,
        input: { username: 'testuser002', email: 'test2@example.com' },
        output: { userId: 'user-12346', username: 'testuser002' },
        logs: [
          { level: 'info', message: '触发器接收到注册请求', timestamp: '2024-03-15 09:15:00.100' },
          { level: 'success', message: '触发器执行成功', timestamp: '2024-03-15 09:15:01.000' }
        ]
      },
      {
        nodeId: 'node-002',
        nodeName: '创建企业微信账号',
        status: 'failed',
        startTime: '2024-03-15 09:15:01',
        endTime: '2024-03-15 09:15:03',
        duration: 2000,
        input: { name: 'testuser002', owner: 'admin001' },
        output: null,
        error: {
          code: 'WECHAT_API_ERROR',
          message: '企业微信API调用失败',
          details: '群聊创建接口返回错误: errcode=40013, errmsg=invalid chatid'
        },
        logs: [
          { level: 'info', message: '开始创建企业微信群聊', timestamp: '2024-03-15 09:15:01.200' },
          { level: 'error', message: 'API调用失败: errcode=40013, errmsg=invalid chatid', timestamp: '2024-03-15 09:15:03.000' }
        ]
      }
    ]
  },
  {
    id: 'exec-003',
    flowId: 'flow-002',
    flowName: '审批通知流程',
    status: 'success',
    startTime: '2024-03-15 08:00:00',
    endTime: '2024-03-15 08:00:08',
    duration: 8000,
    triggerType: 'webhook',
    triggerSource: '/webhook/approval',
    inputData: {
      approvalId: 'approval-001',
      approvalTitle: '年假申请'
    },
    outputData: {
      sentCount: 3,
      status: 'completed'
    },
    nodeExecutions: [
      {
        nodeId: 'node-004',
        nodeName: '审批创建触发',
        status: 'success',
        startTime: '2024-03-15 08:00:00',
        endTime: '2024-03-15 08:00:01',
        duration: 1000,
        input: { approvalId: 'approval-001', approvalTitle: '年假申请' },
        output: { approvalId: 'approval-001', title: '年假申请', approverId: 'manager001' },
        logs: [
          { level: 'info', message: '接收到审批创建事件', timestamp: '2024-03-15 08:00:00.100' },
          { level: 'success', message: '触发器执行成功', timestamp: '2024-03-15 08:00:01.000' }
        ]
      },
      {
        nodeId: 'node-005',
        nodeName: '获取审批人信息',
        status: 'success',
        startTime: '2024-03-15 08:00:01',
        endTime: '2024-03-15 08:00:04',
        duration: 3000,
        input: { userIds: ['manager001'] },
        output: { data: [{ userId: 'manager001', name: '张经理', email: 'zhang@example.com' }] },
        logs: [
          { level: 'info', message: '查询审批人信息', timestamp: '2024-03-15 08:00:01.200' },
          { level: 'info', message: '找到审批人: 张经理', timestamp: '2024-03-15 08:00:04.000' }
        ]
      },
      {
        nodeId: 'node-006',
        nodeName: '发送钉钉通知',
        status: 'success',
        startTime: '2024-03-15 08:00:04',
        endTime: '2024-03-15 08:00:08',
        duration: 4000,
        input: { userIds: ['manager001'], agentId: '1000001', content: '年假申请' },
        output: { errcode: 0, errmsg: 'ok', taskId: 12345 },
        logs: [
          { level: 'info', message: '准备发送钉钉工作通知', timestamp: '2024-03-15 08:00:04.100' },
          { level: 'info', message: '调用钉钉API: POST /message/work', timestamp: '2024-03-15 08:00:04.500' },
          { level: 'success', message: '通知发送成功，taskId: 12345', timestamp: '2024-03-15 08:00:08.000' }
        ]
      }
    ]
  }
];

/**
 * 获取连接流执行历史列表
 *
 * @param {Object} params - 查询参数
 * @param {string} params.flowId - 连接流ID
 * @param {string} params.status - 执行状态
 * @param {number} params.curPage - 当前页码
 * @param {number} params.pageSize - 每页条数
 * @returns {Promise<Object>}
 */
export const mockFetchExecutionHistory = async (params = {}) => {
  const {
    flowId,
    status,
    curPage = 1,
    pageSize = 10
  } = params;

  await new Promise(resolve => setTimeout(resolve, 300));

  let filteredList = [...mockExecutionHistory];

  if (flowId) {
    filteredList = filteredList.filter(item => item.flowId === flowId);
  }

  if (status) {
    filteredList = filteredList.filter(item => item.status === status);
  }

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
 * 获取执行详情
 *
 * @param {string} executionId - 执行ID
 * @returns {Promise<Object>}
 */
export const mockFetchExecutionDetail = async (executionId) => {
  await new Promise(resolve => setTimeout(resolve, 200));

  const execution = mockExecutionHistory.find(item => item.id === executionId);

  if (!execution) {
    return generateErrorResponse({
      code: '404',
      message: '执行记录不存在'
    });
  }

  return generateSuccessResponse({
    data: execution
  });
};

/**
 * 获取执行日志
 *
 * @param {string} executionId - 执行ID
 * @param {string} nodeId - 节点ID（可选）
 * @returns {Promise<Object>}
 */
export const mockFetchExecutionLogs = async (executionId, nodeId) => {
  await new Promise(resolve => setTimeout(resolve, 200));

  const execution = mockExecutionHistory.find(item => item.id === executionId);

  if (!execution) {
    return generateErrorResponse({
      code: '404',
      message: '执行记录不存在'
    });
  }

  let logs = [];

  if (nodeId) {
    const nodeExecution = execution.nodeExecutions.find(n => n.nodeId === nodeId);
    logs = nodeExecution?.logs || [];
  } else {
    logs = execution.nodeExecutions.flatMap(node => node.logs);
  }

  return generateSuccessResponse({
    data: logs
  });
};
