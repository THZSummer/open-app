// SchemaEditor 样式常量定义
export const STYLES = {
  paramRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
    flexWrap: 'wrap'
  },
  paramInput: {
    flex: 1,
    minWidth: 120
  },
  typeSelect: {
    width: 90
  },
  carrierSelect: {
    width: 90
  },
  sourceTypeSelect: {
    width: 100
  },
  valueInput: {
    flex: 2
  },
  complexValueInput: {
    flex: 1,
    minWidth: 200
  },
  cardStyle: {
    marginBottom: 12,
    border: '1px solid #e8e8e8',
    borderRadius: 6,
    padding: 12,
    backgroundColor: '#fafbfc'
  }
};

// 参数默认值配置
export const PARAM_DEFAULTS = {
  paramName: '',
  paramType: 'string',
  sourceType: 'static',
  paramValue: '',
  referencePath: '',
  description: '',
  children: []
};

// 可选的参数类型列表
export const TYPE_OPTIONS = ['string', 'number', 'boolean', 'object', 'array'];

/**
 * 根据参数类型判断是否为复杂类型（object或array）
 */
export const isComplexType = (paramType) => paramType === 'object' || paramType === 'array';

// ========================================
// SchemaEditorV2 专用常量与方法
// ========================================

/**
 * 嵌套层级上限（硬编码 10 层）
 */
export const MAX_SCHEMA_DEPTH = 10;

/**
 * 默认参数类型选项（V2）
 */
export const DEFAULT_TYPE_OPTIONS = ['string', 'number', 'boolean', 'object', 'array'];

/**
 * 参数初始字段（V2，不含 sourceType、paramValue、referencePath）
 */
export const PARAM_DEFAULTS_V2 = {
  paramName: '',
  paramType: 'string',
  description: '',
  children: [],
};

/**
 * 创建一个新的默认参数对象（V2 版）
 * @param {Object} options 配置对象
 * options.carrier 当前 Tab 锁定的 carrier，写入新参数的 carrier 字段
 */
export const createDefaultParamV2 = (options) => {
  // 从配置对象中取出当前 Tab 锁定的 carrier
  const { carrier } = options;

  return {
    ...PARAM_DEFAULTS_V2,
    carrier: carrier || '',
  };
};
