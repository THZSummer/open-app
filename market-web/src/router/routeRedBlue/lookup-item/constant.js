// LookUp 项管理页面常量

// 搜索默认值
export const DEFAULT_SEARCH_VALUES = {
  itemCode: '',
  itemName: '',
  status: undefined
};

// 详情面板模式
export const DETAIL_MODE_VIEW = 'view';
export const DETAIL_MODE_EDIT = 'edit';

// 状态映射
export const STATUS_MAP = {
  1: { text: '有效', color: 'success' },
  0: { text: '失效', color: 'default' }
};

// 表单验证规则
export const FORM_VALIDATION_RULES = {
  itemCode: [
    { required: true, message: '请输入项编码' }
  ],
  itemName: [
    { required: true, message: '请输入项名称' }
  ],
  itemValue: [
    { required: true, message: '请输入项值' }
  ],
  itemIndex: [],
  itemDesc: [
    { max: 500, message: '最多500个字符' }
  ]
};

// 抽屉配置
export const DRAWER_WIDTH = 520;

// 表格列宽度配置
export const TABLE_COLUMN_WIDTHS = {
  itemCode: 100,
  itemName: 120,
  itemValue: 100,
  itemDesc: undefined,
  itemIndex: 60,
  status: 80,
  createBy: 90,
  createTime: 160,
  lastUpdateBy: 90,
  lastUpdateTime: 160,
  action: 180
};

// 扩展属性标签
export const ITEM_ATTR_LABELS = [
  '属性1',
  '属性2',
  '属性3',
  '属性4',
  '属性5',
  '属性6'
];

// 扩展属性字段名
export const ITEM_ATTR_FIELDS = [
  'itemAttr1',
  'itemAttr2',
  'itemAttr3',
  'itemAttr4',
  'itemAttr5',
  'itemAttr6'
];
