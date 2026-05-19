/**
 * ========================================
 * 连接流管理 - Mock数据
 * ========================================
 *
 * 提供连接流相关API的mock数据，用于开发和测试阶段
 * 数据结构遵循API响应格式：{ code, message, data, page }
 */

// 连接流Mock数据列表
export const mockFlows = [
  {
    id: 'flow-001',
    name: '新用户注册流程',
    description: '当用户注册时，自动创建企业微信账号并发送欢迎消息',
    type: 'automation',
    status: 1, // 1-已发布，0-草稿
    nodes: [
      {
        id: 'node-001',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          name: '用户注册触发',
          connectorId: 'connector-001',
          triggerId: 'trigger-001'
        }
      },
      {
        id: 'node-002',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          name: '创建企业微信账号',
          connectorId: 'connector-001',
          actionId: 'action-002'
        }
      },
      {
        id: 'node-003',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          name: '发送欢迎消息',
          connectorId: 'connector-001',
          actionId: 'action-001'
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
    updatedAt: '2024-02-25 14:30:00',
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
          name: '审批创建触发',
          connectorId: 'connector-002',
          triggerId: 'trigger-003'
        }
      },
      {
        id: 'node-005',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          name: '获取审批人信息',
          connectorId: 'connector-002',
          actionId: 'action-003'
        }
      },
      {
        id: 'node-006',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          name: '发送钉钉通知',
          connectorId: 'connector-002',
          actionId: 'action-003'
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
    updatedAt: '2024-03-01 09:15:00',
    publishedAt: '2024-03-01 09:15:00',
  },
  {
    id: 'flow-003',
    name: '文档同步流程',
    description: '将飞书文档变更同步到数据库中',
    type: 'sync',
    status: 0, // 草稿状态
    nodes: [
      {
        id: 'node-007',
        type: 'trigger',
        position: { x: 100, y: 200 },
        data: {
          name: '飞书文档更新触发',
          connectorId: 'connector-003',
          triggerId: 'trigger-004'
        }
      },
      {
        id: 'node-008',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          name: '获取文档内容',
          connectorId: 'connector-003',
          actionId: 'action-005'
        }
      },
      {
        id: 'node-009',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          name: '更新数据库',
          connectorId: 'connector-005',
          actionId: 'action-010'
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
    updatedAt: '2024-03-10 10:45:00',
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
          name: '新邮件触发',
          connectorId: 'connector-004',
          triggerId: 'trigger-006'
        }
      },
      {
        id: 'node-011',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          name: '提取邮件内容',
          connectorId: 'connector-004',
          actionId: 'action-007'
        }
      },
      {
        id: 'node-012',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          name: '写入数据库',
          connectorId: 'connector-005',
          actionId: 'action-010'
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
    updatedAt: '2024-02-28 16:20:00',
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
          name: '定时触发',
          connectorId: 'connector-005',
          triggerId: 'trigger-007'
        }
      },
      {
        id: 'node-014',
        type: 'action',
        position: { x: 300, y: 200 },
        data: {
          name: '查询统计数据',
          connectorId: 'connector-005',
          actionId: 'action-009'
        }
      },
      {
        id: 'node-015',
        type: 'action',
        position: { x: 500, y: 200 },
        data: {
          name: '发送邮件报告',
          connectorId: 'connector-004',
          actionId: 'action-008'
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
    updatedAt: '2024-03-05 11:30:00',
  }
];

// 生成响应数据的辅助函数
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

// 生成标准的错误响应
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

// Mock API 实现函数

/**
 * 获取连接流列表
 *
 * @param {Object} params - 查询参数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {string} [params.type] - 流程类型筛选
 * @param {number} [params.curPage=1] - 当前页码
 * @param {number} [params.pageSize=10] - 每页条数
 * @returns {Promise<Object>}
 */
export const mockFetchFlowList = async (params = {}) => {
  const {
    keyword,
    type,
    curPage = 1,
    pageSize = 10
  } = params;

  // 模拟网络延迟
  await new Promise(resolve => setTimeout(resolve, 300));

  let filteredList = [...mockFlows];

  // 关键词过滤
  if (keyword) {
    const searchKeyword = keyword.toLowerCase();
    filteredList = filteredList.filter(
      item =>
        item.name.toLowerCase().includes(searchKeyword) ||
        item.description.toLowerCase().includes(searchKeyword)
    );
  }

  // 类型过滤
  if (type) {
    filteredList = filteredList.filter(item => item.type === type);
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
    status: 0, // 新建默认为草稿
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
 * 删除连接流
 *
 * @param {string} id - 连接流ID
 * @returns {Promise<Object>}
 */
export const mockDeleteFlow = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 300));

  const index = mockFlows.findIndex(item => item.id === id);

  if (index === -1) {
    return generateErrorResponse({
      code: '404',
      message: '连接流不存在'
    });
  }

  mockFlows.splice(index, 1);

  return generateSuccessResponse({
    message: '连接流删除成功'
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
