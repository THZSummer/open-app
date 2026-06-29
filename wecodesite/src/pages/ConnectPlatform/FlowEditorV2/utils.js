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
 * @param {string} prefix ID 前缀，默认使用 node
 * @returns {string} 格式：prefix_timestamp_randomString
 */
export const generateId = (prefix = 'node') => {
  // 使用时间戳与随机字符串组合，降低同一类型节点 ID 冲突概率
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substr(2, 9);
  return `${prefix}_${timestamp}_${randomStr}`;
};

/**
 * 脚本 Context 默认类型声明
 */
export const DEFAULT_SCRIPT_CONTEXT_TYPE = 'type Context = Record<string, unknown>;';

/**
 * 匹配脚本中的 Context 类型声明块
 */
export const SCRIPT_CONTEXT_TYPE_PATTERN = /type\s+Context\s*=\s*(?:Record<string, unknown>|\{[\s\S]*?\n\};)/;

/**
 * 匹配脚本中的 TypeScript type 声明行
 */
export const SCRIPT_TYPE_LINE_PATTERN = /^\s*type\s+\w+\s*=.*;\s*$/gm;

/**
 * 将 schema 类型映射为 TypeScript 类型
 * @param {string} type schema 参数类型
 * @returns {string} TypeScript 类型
 */
const mapSchemaTypeToTsType = (type) => {
  // 只转换编辑器提示需要的基础类型，未知类型统一使用 unknown。
  if (type === 'string') return 'string';
  if (type === 'number' || type === 'integer') return 'number';
  if (type === 'boolean') return 'boolean';
  if (type === 'array') return 'unknown[]';
  if (type === 'object') return 'Record<string, unknown>';
  return 'unknown';
};

/**
 * 判断字段名是否可直接作为 TypeScript 对象属性
 * @param {string} key 字段名
 * @returns {boolean} 是否为合法标识符
 */
const isSafeTsKey = (key) => /^[A-Za-z_$][\w$]*$/.test(key);

/**
 * 格式化 TypeScript 对象属性名
 * @param {string} key 字段名
 * @returns {string} TypeScript 属性名
 */
const formatTsKey = (key) => (isSafeTsKey(key) ? key : JSON.stringify(key));

/**
 * 向 Context 类型树写入参数路径
 * @param {Object} params 配置对象
 * @param {Object} params.root 类型树根节点
 * @param {Array<string>} params.path 参数路径
 * @param {string} params.type schema 参数类型
 */
const setContextTypePath = (params) => {
  // params.root / params.path / params.type
  const { root, path, type } = params;
  let current = root;
  path.forEach((segment, index) => {
    if (!segment) return;
    if (index === path.length - 1) {
      current[segment] = mapSchemaTypeToTsType(type);
      return;
    }
    if (!current[segment] || typeof current[segment] !== 'object') {
      current[segment] = {};
    }
    current = current[segment];
  });
};

/**
 * 将 Context 类型树渲染为 TypeScript 对象声明
 * @param {Object} tree 类型树
 * @param {number} level 缩进层级
 * @returns {string} TypeScript 对象声明内容
 */
const renderContextTypeTree = (tree, level = 1) => {
  const indent = '  '.repeat(level);
  return Object.keys(tree).map((key) => {
    const value = tree[key];
    if (value && typeof value === 'object') {
      return `${indent}${formatTsKey(key)}: {\n${renderContextTypeTree(value, level + 1)}\n${indent}};`;
    }
    return `${indent}${formatTsKey(key)}: ${value};`;
  }).join('\n');
};

/**
 * 根据上游引用参数生成脚本 Context 类型声明
 * @param {Array} refs 上游引用参数列表
 * @returns {string} Context 类型声明
 */
export const buildScriptContextType = (refs = []) => {
  // 没有上游 schema 时使用宽泛类型，避免误导用户字段一定存在。
  if (!refs.length) return DEFAULT_SCRIPT_CONTEXT_TYPE;

  const tree = {};
  refs.forEach((ref) => {
    const path = [ref.nodeId, ref.scope, ref.carrier, ...(ref.name || '').split('.')].filter(Boolean);
    setContextTypePath({ root: tree, path, type: ref.type });
  });

  return `type Context = {\n${renderContextTypeTree(tree)}\n};`;
};

/**
 * 生成 Monaco TypeScript 额外类型声明
 * @param {string} contextType Context 类型声明
 * @returns {string} Monaco 额外类型声明
 */
export const buildScriptMonacoExtraLib = (contextType) => {
  // 把 transform 的第二个参数和全局 context 都绑定到 Context 类型，提升补全命中率。
  const safeContextType = contextType || DEFAULT_SCRIPT_CONTEXT_TYPE;
  return `${safeContextType}\ndeclare const context: Context;\ndeclare function transform(context: Context): Record<string, unknown>;`;
};

/**
 * 移除脚本中仅供编辑器使用的 TypeScript 类型声明
 * @param {string} script 脚本内容
 * @returns {string} 运行时脚本内容
 */
export const stripScriptEditorTypes = (script) => {
  // GraalJS 运行 JavaScript，保存运行脚本前必须移除 TypeScript 类型声明和参数类型标注。
  return (script || '')
    .replace(SCRIPT_CONTEXT_TYPE_PATTERN, '')
    .replace(SCRIPT_TYPE_LINE_PATTERN, '')
    .replace(/(\w+)\s*:\s*Context/g, '$1')
    .trim();
};

/**
 * 取值模式选项：静态值 / 引用上游参数
 */
export const VALUE_MODE_OPTIONS = [
  { value: 'static', label: '静态值' },
  { value: 'ref', label: '引用上游参数' },
];

/**
 * 把上游 ref 列表转换为分组选项
 * @param {Array} refs 上游引用列表
 * @returns {Array} 分组选项
 */
export const buildRefOptions = (refs = []) => {
  // 按上游节点分组展示完整引用表达式，便于区分不同节点来源
  const groupMap = new Map();
  refs.forEach((item) => {
    const groupLabel = item.groupLabel || item.nodeId || '上游参数';
    const groupOptions = groupMap.get(groupLabel) || [];
    groupOptions.push({
      value: item.value,
      label: item.label || item.path || item.value,
    });
    groupMap.set(groupLabel, groupOptions);
  });
  return Array.from(groupMap.entries()).map(([label, options]) => ({ label, options }));
};

/**
 * 标准化 mapping，兼容旧字符串值
 * @param {*} raw 原始 mapping 值
 * @returns {Object} 标准化后的 mapping 对象
 */
export const normalizeMapping = (raw) => {
  // 新版结构直接保留 mode/value，缺失字段用静态空值兜底
  if (raw && typeof raw === 'object' && 'mode' in raw) {
    return { mode: raw.mode || 'static', value: raw.value ?? '' };
  }
  const str = typeof raw === 'string' ? raw : '';
  return { mode: 'static', value: str };
};

/**
 * 创建触发器节点
 * @returns {Object} 触发器节点
 */
export const createTriggerNode = () => ({
  id: generateId(NODE_TYPE.TRIGGER),
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
  id: generateId(NODE_TYPE.CONNECTOR),
  type: NODE_TYPE.CONNECTOR,
  connectorId: '',
  versionId: '',
  authMethodId: '',
  authMappings: {},
  authConfigs: [],
  connectorVersionConfig: {},
  timeout: 3000,
  inputMappings: {},
  outputParams: {
    header: [],
    body: [],
  },
});

/**
 * 创建脚本处理节点
 * @returns {Object} 脚本处理节点
 */
export const createScriptNode = () => ({
  id: generateId(NODE_TYPE.SCRIPT),
  type: NODE_TYPE.SCRIPT,
  script: `/**
 * 处理上游节点上下文。
 * @param context 上游节点参数上下文
 */
export function transform(context: Context) {
  return {
    result: context
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
  id: generateId(NODE_TYPE.PARALLEL),
  type: NODE_TYPE.PARALLEL,
  activeBranchId: '',
  branches: [
    { id: generateId('branch'), label: '分支1', connector: createConnectorNode() },
    { id: generateId('branch'), label: '分支2', connector: createConnectorNode() },
  ],
});

/**
 * 创建数据输出节点
 * @returns {Object} 数据输出节点
 */
export const createOutputNode = () => ({
  id: generateId(NODE_TYPE.OUTPUT),
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
 * 校验触发器节点
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateTriggerNode = (params) => {
  // params.node
  const { node } = params;
  if (!node.triggerType) return '触发器：未选择触发方式';
  return null;
};

/**
 * 校验连接器节点
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateConnectorNode = (params) => {
  // params.node / params.appLimits
  const { node, appLimits } = params;
  if (!node.connectorId) return '连接器节点：未选择连接器';
  if (!node.versionId) return '连接器节点：未选择连接器版本';

  const timeoutMax = appLimits?.connectorTimeoutMax ?? 300000;
  if (Number(node.timeout) > timeoutMax) {
    return `连接器节点：超时时间超过应用上限 ${timeoutMax} 毫秒`;
  }

  return null;
};

/**
 * 校验脚本处理节点
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateScriptNode = (params) => {
  // params.node
  const { node } = params;
  if (!node.script || !node.script.trim()) return '脚本处理节点：脚本内容不能为空';

  // 用 SchemaEditorV2 的字段名：paramName
  const names = (node.outputParams || []).map(p => p.paramName);
  const hasEmpty = names.some(name => !name?.trim());
  if (hasEmpty) return '脚本处理节点：出参名称不能为空';
  if (new Set(names).size !== names.length) return '脚本处理节点：出参名称不能重复';
  return null;
};

/**
 * 校验并行分支映射值
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateParallelBranchMappingValue = (params) => {
  // params.raw / params.branchLabel / params.paramName
  const { raw, branchLabel, paramName } = params;
  if (typeof raw === 'string') return null;
  if (!raw || typeof raw !== 'object') return null;

  const mode = raw.mode || 'static';
  const value = raw.value;
  if (mode !== 'ref') return null;

  if (!value || typeof value !== 'string' || !/^\$\{.+\}$/.test(value.trim())) {
    return `并行节点：分支「${branchLabel}」参数「${paramName}」引用表达式无效`;
  }
  return null;
};

/**
 * 校验并行分支连接器入参映射
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateParallelBranchMappings = (params) => {
  // params.branch
  const { branch } = params;
  const mappings = branch.connector?.inputMappings || {};

  for (const carrier of ['header', 'body', 'query']) {
    const carrierMap = mappings[carrier] || {};
    for (const paramName of Object.keys(carrierMap)) {
      const error = validateParallelBranchMappingValue({
        raw: carrierMap[paramName],
        branchLabel: branch.label,
        paramName,
      });
      if (error) return error;
    }
  }

  return null;
};

/**
 * 校验单个并行分支
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateParallelBranch = (params) => {
  // params.branch
  const { branch } = params;
  if (!branch.label?.trim()) return '并行节点：分支名称不能为空';
  if (!branch.connector?.connectorId) return `并行节点：分支「${branch.label}」未选择连接器`;
  if (!branch.connector?.versionId) return `并行节点：分支「${branch.label}」未选择连接器版本`;
  return validateParallelBranchMappings({ branch });
};

/**
 * 校验并行分支列表
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateParallelBranches = (params) => {
  // params.branches
  const { branches } = params;
  for (const branch of branches) {
    const error = validateParallelBranch({ branch });
    if (error) return error;
  }
  return null;
};

/**
 * 校验并行节点
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateParallelNode = (params) => {
  // params.node / params.appLimits
  const { node, appLimits } = params;
  const branchMax = appLimits?.parallelBranchMax ?? 8;
  if (!node.branches || node.branches.length === 0) return '并行节点：至少需要 1 个分支';
  if (node.branches.length > branchMax) return `并行节点：分支数量超过上限 ${branchMax}`;
  return validateParallelBranches({ branches: node.branches });
};

/**
 * 递归校验输出参数名称
 *
 * @param {Array} list 参数列表
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateOutputList = (list) => {
  // 递归检查输出参数树，确保每层参数名称都存在。
  for (const param of list || []) {
    if (!param.paramName?.trim()) return '数据输出节点：参数名称不能为空';
    const childError = validateOutputList(param.children || []);
    if (childError) return childError;
  }
  return null;
};

/**
 * 校验数据输出节点
 *
 * @param {Object} params 参数对象
 * @returns {string|null} 错误信息（null 表示通过）
 */
const validateOutputNode = (params) => {
  // params.node
  const { node } = params;
  const all = [...(node.assembleParams?.body || []), ...(node.assembleParams?.header || [])];
  return validateOutputList(all);
};

/**
 * 节点类型校验器映射
 */
const NODE_VALIDATOR_MAP = {
  [NODE_TYPE.TRIGGER]: validateTriggerNode,
  [NODE_TYPE.CONNECTOR]: validateConnectorNode,
  [NODE_TYPE.SCRIPT]: validateScriptNode,
  [NODE_TYPE.PARALLEL]: validateParallelNode,
  [NODE_TYPE.OUTPUT]: validateOutputNode,
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
  const validator = NODE_VALIDATOR_MAP[node.type];
  if (!validator) return null;
  return validator({ node, appLimits });
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
 * 获取节点类型展示名称
 * @param {string} nodeType 节点类型
 * @returns {string} 节点类型展示名称
 */
const getNodeTypeLabel = (nodeType) => {
  // 按节点类型返回引用分组展示名称。
  if (nodeType === NODE_TYPE.TRIGGER) return '触发器节点';
  if (nodeType === NODE_TYPE.CONNECTOR) return '连接器节点';
  if (nodeType === NODE_TYPE.SCRIPT) return '脚本处理节点';
  if (nodeType === NODE_TYPE.PARALLEL) return '并行节点';
  if (nodeType === NODE_TYPE.OUTPUT) return '数据输出节点';
  return '节点';
};

/**
 * 写入一条上游引用参数
 *
 * @param {Object} params 参数对象
 */
const pushUpstreamRef = (params) => {
  // params.refs / params.nodeId / params.nodeType / params.scope / params.carrier / params.name / params.type / params.groupName
  const { refs, nodeId, nodeType, scope, carrier, name, type, groupName } = params;
  const path = `${nodeId}.${scope}.${carrier}.${name}`;
  refs.push({
    nodeId,
    nodeType,
    scope,
    carrier,
    name,
    type,
    path,
    value: path,
    label: path,
    groupLabel: groupName || `${getNodeTypeLabel(nodeType)} ${nodeId}`,
  });
};

/**
 * 递归把 SchemaEditorV2 结构里的参数平铺为 dotted name
 *
 * @param {Object} params 参数对象
 */
const walkSchemaParams = (params) => {
  // params.list / params.prefix / params.onItem
  const { list, prefix, onItem } = params;
  (list || []).forEach((param) => {
    const name = param.paramName || param.name || '';
    if (!name) return;

    const fullName = prefix ? `${prefix}.${name}` : name;
    const type = param.paramType || param.type || 'string';
    onItem({ name: fullName, type });
    if ((type === 'object' || type === 'array') && Array.isArray(param.children) && param.children.length > 0) {
      walkSchemaParams({ list: param.children, prefix: fullName, onItem });
    }
  });
};

/**
 * 按位置收集参数列表
 *
 * @param {Object} params 参数对象
 */
const collectCarrierParams = (params) => {
  // params.refs / params.node / params.scope / params.carriers / params.sourceMap / params.groupName
  const { refs, node, scope, carriers, sourceMap, groupName } = params;
  carriers.forEach((carrier) => {
    walkSchemaParams({
      list: sourceMap?.[carrier] || [],
      prefix: '',
      onItem: ({ name, type }) => pushUpstreamRef({
        refs,
        nodeId: node.id,
        nodeType: node.type,
        scope,
        carrier,
        name,
        type,
        groupName,
      }),
    });
  });
};

/**
 * 收集触发器节点可引用参数
 *
 * @param {Object} params 参数对象
 */
const collectTriggerRefs = (params) => {
  // params.refs / params.trigger
  const { refs, trigger } = params;
  collectCarrierParams({
    refs,
    node: trigger,
    scope: 'input',
    carriers: ['header', 'query', 'body'],
    sourceMap: trigger.inputParams || {},
  });
};

/**
 * 收集脚本节点可引用参数
 *
 * @param {Object} params 参数对象
 */
const collectScriptRefs = (params) => {
  // params.refs / params.node
  const { refs, node } = params;
  collectCarrierParams({
    refs,
    node,
    scope: 'output',
    carriers: ['body'],
    sourceMap: { body: node.outputParams || [] },
  });
};

/**
 * 收集连接器节点可引用参数
 *
 * @param {Object} params 参数对象
 */
const collectConnectorRefs = (params) => {
  // params.refs / params.node
  const { refs, node } = params;
  collectCarrierParams({
    refs,
    node,
    scope: 'output',
    carriers: ['header', 'body'],
    sourceMap: node.outputParams || {},
  });
};

/**
 * 收集并行节点分支连接器可引用参数
 *
 * @param {Object} params 参数对象
 */
const collectParallelRefs = (params) => {
  // params.refs / params.node
  const { refs, node } = params;
  (node.branches || []).forEach((branch) => {
    const connector = branch.connector || {};
    collectCarrierParams({
      refs,
      node: { ...connector, type: NODE_TYPE.CONNECTOR },
      scope: 'output',
      carriers: ['header', 'body'],
      sourceMap: connector.outputParams || {},
      groupName: `并行分支 ${branch.label || branch.id} ${connector.id || ''}`.trim(),
    });
  });
};

/**
 * 按节点类型收集可引用参数
 *
 * @param {Object} params 参数对象
 */
const collectStepRefs = (params) => {
  // params.refs / params.node
  const { refs, node } = params;
  if (node.type === NODE_TYPE.SCRIPT) collectScriptRefs({ refs, node });
  if (node.type === NODE_TYPE.CONNECTOR) collectConnectorRefs({ refs, node });
  if (node.type === NODE_TYPE.PARALLEL) collectParallelRefs({ refs, node });
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
  // params.flowData / params.currentNodeId
  const { flowData, currentNodeId } = params;
  const refs = [];

  if (!flowData) return refs;

  const trigger = flowData.trigger;
  if (trigger && trigger.id !== currentNodeId) collectTriggerRefs({ refs, trigger });

  for (const node of flowData.steps || []) {
    if (node.id === currentNodeId) break;
    collectStepRefs({ refs, node });
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