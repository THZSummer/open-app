/**
 * ========================================
 * 流程工具函数库
 * ========================================
 * 
 * 提供连接器和连接流管理功能所需的核心工具函数
 * 包括节点ID生成、位置计算、验证等功能
 */

/**
 * 生成唯一的节点ID
 * @param {string} prefix - ID前缀，默认为'node'
 * @returns {string} 格式：prefix_timestamp_randomString
 * @example generateNodeId('trigger') => "trigger_1715000000000_abc123xyz"
 */
export const generateNodeId = (prefix = 'node') => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `${prefix}_${timestamp}_${randomStr}`;
};

/**
 * 生成唯一的连线ID
 * @returns {string} 格式：edge_timestamp_randomString
 */
export const generateEdgeId = () => {
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `edge_${timestamp}_${randomStr}`;
};

/**
 * 初始化节点位置（网格对齐）
 * 将节点位置对齐到10px的网格
 * @param {number} x - X坐标
 * @param {number} y - Y坐标
 * @returns {{x: number, y: number}} 对齐后的坐标
 */
export const getInitialNodePosition = (x = 100, y = 100) => ({
  x: Math.round(x / 10) * 10,
  y: Math.round(y / 10) * 10,
});

/**
 * 节点类型常量
 * 定义支持的6种节点类型
 */
export const NODE_TYPES = {
  TRIGGER: 'trigger',    // 触发器节点
  ACTION: 'action',      // 执行动作节点
  CONDITION: 'condition', // 条件分支节点
  DELAY: 'delay',        // 延时节点
  PARALLEL: 'parallel',  // 并行执行节点
  LOOP: 'loop',          // 循环执行节点
};

/**
 * 验证节点配置
 * @param {Object} node - 节点对象
 * @returns {{valid: boolean, errors: Array}} 验证结果
 */
export const validateNodeConfig = (node) => {
  const errors = [];
  const { type, data } = node;
  
  switch (type) {
    case NODE_TYPES.TRIGGER:
      if (!data.config?.triggerType) {
        errors.push('请选择触发类型');
      }
      if (data.config?.triggerType === 'schedule' && !data.config?.cronExpression) {
        errors.push('定时触发需要配置Cron表达式');
      }
      if (data.config?.triggerType === 'webhook' && !data.config?.webhookPath) {
        errors.push('Webhook触发需要配置Webhook路径');
      }
      break;
      
    case NODE_TYPES.ACTION:
      if (!data.config?.connectorId) {
        errors.push('请选择连接器');
      }
      if (!data.config?.actionId) {
        errors.push('请选择执行动作');
      }
      break;
      
    case NODE_TYPES.CONDITION:
      if (!data.config?.conditions || data.config.conditions.length === 0) {
        errors.push('请至少添加一个条件');
      }
      break;
      
    case NODE_TYPES.PARALLEL:
      if (!data.config?.branches || data.config.branches.length < 2) {
        errors.push('并行执行至少需要2个分支');
      }
      break;
      
    case NODE_TYPES.LOOP:
      if (data.config?.loopType === 'times') {
        if (!data.config?.maxIterations || data.config.maxIterations < 1) {
          errors.push('循环次数必须大于0');
        }
        if (data.config.maxIterations > 1000) {
          errors.push('循环次数不能超过1000');
        }
      }
      break;
      
    case NODE_TYPES.DELAY:
      if (!data.config?.duration || data.config.duration < 1) {
        errors.push('延时时长必须大于0秒');
      }
      if (data.config?.duration > 86400) {
        errors.push('延时时长不能超过24小时（86400秒）');
      }
      break;
  }
  
  return {
    valid: errors.length === 0,
    errors,
  };
};

/**
 * 验证流程配置
 * 检查流程是否包含必需的节点和有效的连线
 * @param {Array} nodes - 节点数组
 * @param {Array} edges - 连线数组
 * @returns {{valid: boolean, errors: Array, warnings: Array}} 验证结果
 */
export const validateFlowConfig = (nodes = [], edges = []) => {
  const errors = [];
  const warnings = [];
  
  // 检查是否有触发器节点
  const hasTrigger = nodes.some(node => node.type === NODE_TYPES.TRIGGER);
  if (!hasTrigger) {
    errors.push('流程必须包含至少一个触发器节点');
  }
  
  // 检查是否有孤立节点
  const connectedNodeIds = new Set();
  edges.forEach(edge => {
    connectedNodeIds.add(edge.source);
    connectedNodeIds.add(edge.target);
  });
  
  nodes.forEach(node => {
    if (node.type !== NODE_TYPES.TRIGGER && !connectedNodeIds.has(node.id)) {
      warnings.push(`节点 "${node.data?.label || node.id}" 未连接到流程`);
    }
  });
  
  // 检查每个节点的配置
  nodes.forEach(node => {
    const nodeValidation = validateNodeConfig(node);
    if (!nodeValidation.valid) {
      nodeValidation.errors.forEach(error => {
        errors.push(`节点 "${node.data?.label || node.id}"：${error}`);
      });
    }
  });
  
  // 检查连线是否有效
  edges.forEach(edge => {
    const sourceNode = nodes.find(n => n.id === edge.source);
    const targetNode = nodes.find(n => n.id === edge.target);
    
    if (!sourceNode) {
      errors.push(`连线起点 "${edge.source}" 不存在`);
    }
    if (!targetNode) {
      errors.push(`连线终点 "${edge.target}" 不存在`);
    }
  });
  
  return {
    valid: errors.length === 0,
    errors,
    warnings,
  };
};

/**
 * 获取节点的所有上游节点（去重）
 * @param {string} nodeId - 当前节点ID
 * @param {Array} nodes - 节点列表
 * @param {Array} edges - 连线列表
 * @returns {Array} 上游节点数组
 */
export const getUpstreamNodes = (nodeId, nodes = [], edges = []) => {
  const visited = new Set();
  const result = [];

  function traverse(currentId) {
    if (visited.has(currentId)) return;
    visited.add(currentId);

    const incomingEdges = edges.filter(e => e.target === currentId);
    for (const edge of incomingEdges) {
      const upstreamNode = nodes.find(n => n.id === edge.source);
      if (upstreamNode && !visited.has(upstreamNode.id)) {
        result.push(upstreamNode);
        traverse(upstreamNode.id);
      }
    }
  }

  traverse(nodeId);
  return result;
};

/**
 * 获取节点的输出参数
 * @param {Object} node - 节点对象
 * @returns {Array} 参数数组
 */
export const getNodeOutputParams = (node) => {
  if (!node) return [];

  const { type, data } = node;

  switch (type) {
    case 'trigger':
      return data.config?.inputParams || [];
    case 'action':
      return data.config?.outputParams || [];
    case 'condition':
      return [
        { paramName: data.config?.trueOutput || 'trueOutput', paramType: 'string', description: '条件满足时输出' },
        { paramName: data.config?.falseOutput || 'falseOutput', paramType: 'string', description: '条件不满足时输出' },
      ];
    case 'loop':
      return [
        { paramName: data.config?.loopVariable || 'loopIndex', paramType: 'number', description: '循环计数器' },
      ];
    case 'dataTransform':
      return data.config?.mappings?.map(m => ({
        paramName: m.targetField,
        paramType: 'string',
        description: `映射自 ${m.sourceField}`,
      })) || [];
    default:
      return [];
  }
};

/**
 * 递归平铺参数（处理嵌套的 object/array 类型）
 * @param {Array} params - 参数数组
 * @param {string} prefix - 路径前缀
 * @returns {Array} 平铺后的参数数组
 */
export const flattenParams = (params = [], prefix = '') => {
  const result = [];

  for (const param of params) {
    const fullPath = prefix ? `${prefix}.${param.paramName}` : param.paramName;

    result.push({
      paramName: param.paramName,
      paramPath: fullPath,
      paramType: param.paramType,
      description: param.description || '',
    });

    if ((param.paramType === 'object' || param.paramType === 'array') && param.children?.length > 0) {
      result.push(...flattenParams(param.children, fullPath));
    }
  }

  return result;
};

/**
 * 获取节点的上游参数列表（按节点分组）
 * @param {string} nodeId - 当前节点ID
 * @param {Array} nodes - 节点列表
 * @param {Array} edges - 连线列表
 * @returns {Array} 按节点分组的参数列表 [{ nodeName, nodeId, params: [] }]
 */
export const getUpstreamParams = (nodeId, nodes = [], edges = []) => {
  const upstreamNodes = getUpstreamNodes(nodeId, nodes, edges);

  return upstreamNodes.map(node => {
    const outputParams = getNodeOutputParams(node);
    const flattenedParams = flattenParams(outputParams);

    return {
      nodeName: node.data?.label || node.id,
      nodeId: node.id,
      nodeType: node.type,
      params: flattenedParams,
    };
  });
};
