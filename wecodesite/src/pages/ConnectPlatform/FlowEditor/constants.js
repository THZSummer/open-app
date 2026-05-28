/**
 * ========================================
 * 连接流编辑器常量配置
 * ========================================
 */

import TriggerNode from '../../../components/CustomFlowNodes/TriggerNode';
import ActionNode from '../../../components/CustomFlowNodes/ActionNode';
import DataOutputNode from '../../../components/CustomFlowNodes/DataOutputNode';

/**
 * 节点类型对应的颜色配置
 */
export const NODE_COLORS = {
  trigger: '#1890ff',
  connector: '#52c41a',
  exit: '#fa8c16',
};

/**
 * 节点类型注册表
 * 格式：{ typeName: Component }
 */
export const nodeTypes = {
  trigger: TriggerNode,
  connector: ActionNode,
  exit: DataOutputNode,
};

/**
 * 节点库配置
 * 定义可在节点库面板中拖拽的节点
 * 注意：触发器节点已在节点库中，但画布只能有一个触发器
 */
export const NODE_LIBRARY = [
  { type: 'trigger', label: '触发器', labelEn: 'Trigger', description: '定义流程的触发方式，每个流程只能添加一个', visible: true },
  { type: 'connector', label: '连接器', labelEn: 'Connector', description: '调用连接器的执行动作', visible: true },
  { type: 'exit', label: '数据输出', labelEn: 'Exit', description: '定义对外返回的数据结构', visible: true },
];

/**
 * 节点类型元数据
 * 用于显示和配置
 */
export const NODE_TYPE_META = {
  trigger: {
    name: '触发器',
    nameEn: 'Trigger',
    color: NODE_COLORS.trigger,
    description: '定义流程的触发方式',
  },
  connector: {
    name: '连接器',
    nameEn: 'Connector',
    color: NODE_COLORS.connector,
    description: '调用连接器的执行动作',
  },
  exit: {
    name: '数据输出',
    nameEn: 'Exit',
    color: NODE_COLORS.exit,
    description: '定义对外返回的数据结构',
  },
};

/**
 * SchemaEditor组件的Carrier选项配置
 * 定义参数可以出现的位置
 */
export const CARRIER_OPTIONS = ['header', 'body', 'query'];
/**
 * SchemaEditor组件的Carrier选项配置（输出节点专用）
 * 定义参数可以出现的位置，输出节点不支持query参数
 */
export const CARRIER_OPTIONS_OUTPUT = ['header', 'body'];

/**
 * SchemaEditor组件的固定配置
 * 用于触发器节点、连接器节点和数据输出节点的配置
 */
export const SCHEMA_EDITOR_CONFIG = {
  /** 触发器节点 - 输入契约配置 */
  inputContract: {
    schemaType: 'inputContract',
    editable: true,
    showCarrier: true,
    carrierOptions: CARRIER_OPTIONS,
  },
  /** 连接器节点 - 输入映射配置 */
  inputMapping: {
    schemaType: 'inputMapping',
    editable: true,
    mode: 'reference',
    showCarrier: true,
    carrierOptions: CARRIER_OPTIONS,
    lockedFields: {
      paramName: true,
      paramType: true,
      carrier: true,
      sourceType: false,
    },
    showActionButtons: false,
  },
  /** 数据输出节点 - 输出映射配置 */
  outputMapping: {
    schemaType: 'outputMapping',
    editable: true,
    mode: 'reference',
    showCarrier: true,
    carrierOptions: CARRIER_OPTIONS_OUTPUT,
  },
};

/**
 * 节点类型到数据处理配置的映射
 * 用于NodeProperties中简化数据转换逻辑
 */
export const NODE_TRANSFORM_CONFIG = {
  trigger: {
    /** 对应的字段名 */
    fieldName: 'inputContract',
    /** 转换函数 */
    transformFuncName: 'transformInputMappingFromNested',
    /** 状态变量名 */
    stateKey: 'flowInputSchema',
    /** apiConfig中的键名 */
    apiConfigKey: 'inputContract',
  },
  connector: {
    fieldName: 'inputMapping',
    transformFuncName: 'transformInputMappingFromNested',
    stateKey: 'transformedInputMapping',
    apiConfigKey: 'inputMapping',
  },
  exit: {
    fieldName: 'outputMapping',
    transformFuncName: 'transformOutputMappingFromNested',
    stateKey: 'transformedOutputMapping',
    apiConfigKey: 'outputMapping',
  },
};

/**
 * 节点类型到 Mapping 字段名的映射
 * 用于 transformToBackend 中统一处理不同节点的 mapping 转换
 */
export const NODE_TYPE_TO_MAPPING_KEY = {
  trigger: 'inputContract',
  connector: 'inputMapping',
  exit: 'outputMapping',
};
