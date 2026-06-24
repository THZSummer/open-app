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
