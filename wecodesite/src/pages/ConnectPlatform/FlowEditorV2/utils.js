/**
 * ========================================
 * 连接流编辑器 V2 - 工具函数
 * ========================================
 *
 * 节点工厂、可插入类型计算、节点校验等纯函数。
 */

import { NODE_TYPE, FLOW_MODE, VALIDATE_SECTION } from './constants';

/**
 * 生成唯一 ID
 * @returns {string} 唯一 ID
 */
export const generateId = () => `node_${Date.now()}_${Math.random().toString(16).slice(2, 10)}`;

/**
 * 创建触发器节点
 * @returns {Object} 触发器节点
 */
export const createTriggerNode = () => ({
  id: 'trigger',
  type: NODE_TYPE.TRIGGER,
  triggerType: 'http',
  systokens: [],
  inputParams: {
    header: [],
    body: [],
    query: [],
  },
});

/**
 * 创建连接器节点
 * @returns {Object} 连接器节点
 */
export const createConnectorNode = () => ({
  id: generateId(),
  type: NODE_TYPE.CONNECTOR,
  connectorId: '',
  versionId: '',
  authMethodId: '',
  timeout: 3,
  inputMappings: {},
});

/**
 * 创建脚本处理节点
 * @returns {Object} 脚本处理节点
 */
export const createScriptNode = () => ({
  id: generateId(),
  type: NODE_TYPE.SCRIPT,
  script: `type Input = Record<string, unknown>;

export function transform(input: Input) {
  return {
    result: input
  };
}`,
  outputParams: [
    { paramName: 'result', paramType: 'object', description: '脚本处理结果', children: [] },
  ],
});

/**
 * 创建并行节点
 * @returns {Object} 并行节点（默认 2 个分支）
 */
export const createParallelNode = () => ({
  id: generateId(),
  type: NODE_TYPE.PARALLEL,
  activeBranchId: '',
  branches: [
    { id: generateId(), label: '分支1', connector: createConnectorNode() },
    { id: generateId(), label: '分支2', connector: createConnectorNode() },
  ],
});

/**
 * 创建数据输出节点
 * @returns {Object} 数据输出节点
 */
export const createOutputNode = () => ({
  id: 'output',
  type: NODE_TYPE.OUTPUT,
  assembleParams: {
    body: [],
    header: [],
  },
});

/**
 * 创建空连接流数据
 * @returns {Object} 空连接流数据
 */
export const createEmptyFlowData = () => ({
  flowMode: '',
  trigger: createTriggerNode(),
  steps: [],
  output: createOutputNode(),
  rateLimit: 10,
  cacheEnabled: false,
  cacheTime: 60,
  cacheKeys: [],
});

/**
 * 按编排模式初始化节点结构
 * @param {string} mode 编排模式
 * @returns {Object} 初始化后的连接流数据
 */
export const initFlowDataByMode = (mode) => {
  const data = createEmptyFlowData();
  data.flowMode = mode;

  if (mode === FLOW_MODE.SINGLE || mode === FLOW_MODE.SERIAL) {
    data.steps = [createConnectorNode()];
  }
  if (mode === FLOW_MODE.PARALLEL) {
    data.steps = [createParallelNode()];
  }
  return data;
};

/**
 * 获取可见节点列表（按步骤条顺序）
 * @param {Object} flowData 连接流数据
 * @returns {Array} 节点数组
 */
export const getVisibleNodes = (flowData) => {
  if (!flowData?.flowMode) return [];
  return [flowData.trigger, ...flowData.steps, flowData.output];
};

/**
 * 计算指定加号位置可插入的节点类型
 *
 * @param {Object} params
 * @param {string} params.flowMode 编排模式
 * @param {Array} params.steps 中间步骤节点列表
 * @param {number} params.insertIndex 插入位置（0 表示触发器和第一个 step 之间）
 * @param {Object} params.appLimits 应用级上限
 * @returns {Array<string>} 可插入节点类型
 */
export const computeInsertableTypes = (params) => {
  // params.flowMode / params.steps / params.insertIndex / params.appLimits
  const { flowMode, steps, insertIndex, appLimits } = params;

  // 单节点模式：不允许插入
  if (flowMode === FLOW_MODE.SINGLE) {
    return [];
  }

  // 串行模式
  if (flowMode === FLOW_MODE.SERIAL) {
    const result = [];

    // 连接器：受应用级上限约束
    const connectorCount = steps.filter(node => node.type === NODE_TYPE.CONNECTOR).length;
    if (connectorCount < (appLimits?.serialConnectorMax ?? 3)) {
      result.push(NODE_TYPE.CONNECTOR);
    }

    // 脚本：不能与脚本相邻
    const prev = steps[insertIndex - 1];
    const next = steps[insertIndex];
    const isAdjacentScript = prev?.type === NODE_TYPE.SCRIPT || next?.type === NODE_TYPE.SCRIPT;
    if (!isAdjacentScript) {
      result.push(NODE_TYPE.SCRIPT);
    }

    return result;
  }

  // 并行模式：仅 触发器↔并行节点 和 并行节点↔输出 两个位置，且仅可插入脚本
  if (flowMode === FLOW_MODE.PARALLEL) {
    const prev = steps[insertIndex - 1];
    const next = steps[insertIndex];
    const isAdjacentScript = prev?.type === NODE_TYPE.SCRIPT || next?.type === NODE_TYPE.SCRIPT;
    return isAdjacentScript ? [] : [NODE_TYPE.SCRIPT];
  }

  return [];
};

/**
 * 按类型创建节点实例
 * @param {string} type 节点类型
 * @returns {Object} 节点实例
 */
export const createNodeByType = (type) => {
  if (type === NODE_TYPE.CONNECTOR) return createConnectorNode();
  if (type === NODE_TYPE.SCRIPT) return createScriptNode();
  if (type === NODE_TYPE.PARALLEL) return createParallelNode();
  return null;
};

/**
 * 判断节点是否可删除
 *
 * @param {Object} params
 * @param {Object} params.node 当前节点
 * @param {string} params.flowMode 编排模式
 * @param {Array} params.steps 中间步骤节点列表
 * @returns {boolean} 是否可删除
 */
export const canDeleteNode = (params) => {
  // params.node / params.flowMode / params.steps
  const { node, flowMode, steps } = params;

  if (!node) return false;
  if (node.type === NODE_TYPE.TRIGGER || node.type === NODE_TYPE.OUTPUT) return false;
  if (node.type === NODE_TYPE.PARALLEL) return false;

  if (node.type === NODE_TYPE.CONNECTOR) {
    // 单节点模式：固定连接器，不可删
    if (flowMode === FLOW_MODE.SINGLE) return false;
    // 串行模式：连接器数量 > 1 时可删
    if (flowMode === FLOW_MODE.SERIAL) {
      const connectorCount = steps.filter(item => item.type === NODE_TYPE.CONNECTOR).length;
      return connectorCount > 1;
    }
    // 并行模式：分支内连接器不可删
    return false;
  }

  if (node.type === NODE_TYPE.SCRIPT) return true;

  return false;
};

/**
 * 校验单个节点
 *
 * @param {Object} params
 * @param {Object} params.node 节点
 * @param {Object} params.appLimits 应用级上限
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateNode = (params) => {
  // params.node / params.appLimits
  const { node, appLimits } = params;

  if (node.type === NODE_TYPE.TRIGGER) {
    if (!node.triggerType) return '触发器：未选择触发方式';
    return null;
  }

  if (node.type === NODE_TYPE.CONNECTOR) {
    if (!node.connectorId) return '连接器节点：未选择连接器';
    if (!node.versionId) return '连接器节点：未选择连接器版本';
    const timeoutMax = appLimits?.connectorTimeoutMax ?? 3;
    if (Number(node.timeout) > timeoutMax) {
      return `连接器节点：超时时间超过应用上限 ${timeoutMax} 秒`;
    }
    // 校验入参映射的取值合法性
    const mappings = node.inputMappings || {};
    for (const carrier of ['header', 'body', 'query']) {
      const carrierMap = mappings[carrier] || {};
      for (const paramName of Object.keys(carrierMap)) {
        const raw = carrierMap[paramName];
        // 兼容旧字符串值：跳过校验
        if (typeof raw === 'string') continue;
        if (!raw || typeof raw !== 'object') continue;
        const mode = raw.mode || 'static';
        const value = raw.value;
        if (mode === 'ref') {
          // 引用模式：必须形如 ${...}
          if (!value || typeof value !== 'string' || !/^\$\{.+\}$/.test(value.trim())) {
            return `连接器节点：参数「${paramName}」引用表达式无效`;
          }
        }
      }
    }
    return null;
  }

  if (node.type === NODE_TYPE.SCRIPT) {
    if (!node.script || !node.script.trim()) return '脚本处理节点：脚本内容不能为空';
    // 用 SchemaEditorV2 的字段名：paramName
    const names = (node.outputParams || []).map(p => p.paramName);
    const hasEmpty = names.some(name => !name?.trim());
    if (hasEmpty) return '脚本处理节点：出参名称不能为空';
    if (new Set(names).size !== names.length) return '脚本处理节点：出参名称不能重复';
    return null;
  }

  if (node.type === NODE_TYPE.PARALLEL) {
    const branchMax = appLimits?.parallelBranchMax ?? 8;
    if (!node.branches || node.branches.length === 0) return '并行节点：至少需要 1 个分支';
    if (node.branches.length > branchMax) return `并行节点：分支数量超过上限 ${branchMax}`;
    for (const branch of node.branches) {
      if (!branch.label?.trim()) return '并行节点：分支名称不能为空';
      if (!branch.connector?.connectorId) return `并行节点：分支「${branch.label}」未选择连接器`;
      if (!branch.connector?.versionId) return `并行节点：分支「${branch.label}」未选择连接器版本`;
      // 校验分支内连接器入参映射的取值
      const mappings = branch.connector?.inputMappings || {};
      for (const carrier of ['header', 'body', 'query']) {
        const carrierMap = mappings[carrier] || {};
        for (const paramName of Object.keys(carrierMap)) {
          const raw = carrierMap[paramName];
          if (typeof raw === 'string') continue;
          if (!raw || typeof raw !== 'object') continue;
          const mode = raw.mode || 'static';
          const value = raw.value;
          if (mode === 'ref') {
            if (!value || typeof value !== 'string' || !/^\$\{.+\}$/.test(value.trim())) {
              return `并行节点：分支「${branch.label}」参数「${paramName}」引用表达式无效`;
            }
          }
        }
      }
    }
    return null;
  }

  if (node.type === NODE_TYPE.OUTPUT) {
    const all = [...(node.assembleParams?.body || []), ...(node.assembleParams?.header || [])];
    // 用 SchemaEditorV2 的字段名：paramName
    const names = all.map(p => p.paramName);
    const hasEmpty = names.some(name => !name?.trim());
    if (hasEmpty) return '数据输出节点：参数名称不能为空';
    return null;
  }

  return null;
};

/**
 * 发布前全量校验
 *
 * @param {Object} params
 * @param {Object} params.flowData 连接流数据
 * @param {Object} params.appLimits 应用级上限
 * @returns {Object|null} 错误信息 { section, message }；null 表示校验通过
 */
export const validateForPublish = (params) => {
  // params.flowData / params.appLimits
  const { flowData, appLimits } = params;

  if (!flowData?.flowMode) {
    return { section: null, message: '请先选择编排模式' };
  }

  // 触发器
  const triggerErr = validateNode({ node: flowData.trigger, appLimits });
  if (triggerErr) return { section: VALIDATE_SECTION.TRIGGER, message: triggerErr };

  // 中间节点
  for (const node of flowData.steps) {
    const err = validateNode({ node, appLimits });
    if (err) {
      let section = VALIDATE_SECTION.CONNECTOR;
      if (node.type === NODE_TYPE.SCRIPT) section = VALIDATE_SECTION.SCRIPT;
      if (node.type === NODE_TYPE.PARALLEL) section = VALIDATE_SECTION.PARALLEL;
      return { section, message: err };
    }
  }

  // 输出节点
  const outputErr = validateNode({ node: flowData.output, appLimits });
  if (outputErr) return { section: VALIDATE_SECTION.OUTPUT, message: outputErr };

  return null;
};

/**
 * 收集指定节点之前的所有可引用上游参数
 *
 * @param {Object} params
 * @param {Object} params.flowData 连接流数据
 * @param {string} params.currentNodeId 当前节点 ID
 * @returns {Array} 上游可引用参数列表
 */
export const collectUpstreamRefs = (params) => {
  const { flowData, currentNodeId } = params;
  const refs = [];

  if (!flowData) return refs;

  /**
   * 递归把 SchemaEditorV2 结构里的参数（包括嵌套 children）平铺为 dotted name
   *
   * @param {Object} options
   *   options.list      参数列表
   *   options.prefix    上层 name 拼接前缀
   *   options.onLeaf    叶子节点回调（接收 { name, type }）
   */
  const walkSchema = (options) => {
    const { list, prefix, onLeaf } = options;
    (list || []).forEach((param) => {
      const name = param.paramName || '';
      if (!name) return;
      const fullName = prefix ? `${prefix}.${name}` : name;
      onLeaf({ name: fullName, type: param.paramType || 'string' });
      if ((param.paramType === 'object' || param.paramType === 'array') && Array.isArray(param.children) && param.children.length > 0) {
        walkSchema({ list: param.children, prefix: fullName, onLeaf });
      }
    });
  };

  // 触发器输出
  const trigger = flowData.trigger;
  if (trigger && trigger.id !== currentNodeId) {
    const inputParams = trigger.inputParams || {};
    ['header', 'body', 'query'].forEach((group) => {
      walkSchema({
        list: inputParams[group] || [],
        prefix: '',
        onLeaf: ({ name, type }) => {
          refs.push({
            nodeId: trigger.id,
            nodeType: trigger.type,
            source: `trigger.${group}`,
            name,
            type,
          });
        },
      });
    });
  }

  // 中间节点输出
  for (const node of flowData.steps || []) {
    if (node.id === currentNodeId) break;

    if (node.type === NODE_TYPE.SCRIPT) {
      walkSchema({
        list: node.outputParams || [],
        prefix: '',
        onLeaf: ({ name, type }) => {
          refs.push({
            nodeId: node.id,
            nodeType: node.type,
            source: `${node.id}.output`,
            name,
            type,
          });
        },
      });
    }

    if (node.type === NODE_TYPE.CONNECTOR) {
      refs.push({
        nodeId: node.id,
        nodeType: node.type,
        source: `${node.id}.response`,
        name: 'response',
        type: 'object',
      });
    }

    if (node.type === NODE_TYPE.PARALLEL) {
      (node.branches || []).forEach((branch) => {
        refs.push({
          nodeId: node.id,
          nodeType: node.type,
          source: `${node.id}.${branch.id}.response`,
          name: `${branch.label}.response`,
          type: 'object',
        });
      });
    }
  }

  return refs;
};

/**
 * 按类型重新编号生成节点显示标题
 *
 * @param {Object} params
 * @param {Array} params.nodes 节点列表
 * @param {string} params.flowMode 编排模式
 * @returns {Map<string, string>} nodeId → title
 */
export const buildNodeTitles = (params) => {
  // params.nodes / params.flowMode
  const { nodes, flowMode } = params;
  const titleMap = new Map();
  const counter = {
    [NODE_TYPE.TRIGGER]: 0,
    [NODE_TYPE.CONNECTOR]: 0,
    [NODE_TYPE.SCRIPT]: 0,
    [NODE_TYPE.PARALLEL]: 0,
    [NODE_TYPE.OUTPUT]: 0,
  };

  const baseTitleMap = {
    [NODE_TYPE.TRIGGER]: '触发器',
    [NODE_TYPE.CONNECTOR]: '连接器',
    [NODE_TYPE.SCRIPT]: '脚本处理',
    [NODE_TYPE.PARALLEL]: '并行节点',
    [NODE_TYPE.OUTPUT]: '数据输出',
  };

  // 统计每类节点总数
  const totalCount = {};
  nodes.forEach(node => {
    totalCount[node.type] = (totalCount[node.type] || 0) + 1;
  });

  nodes.forEach(node => {
    counter[node.type] += 1;
    const base = baseTitleMap[node.type];
    const skipNumbering = flowMode === FLOW_MODE.SINGLE
      || node.type === NODE_TYPE.TRIGGER
      || node.type === NODE_TYPE.OUTPUT
      || totalCount[node.type] === 1;
    titleMap.set(node.id, skipNumbering ? base : `${base} ${counter[node.type]}`);
  });

  return titleMap;
};