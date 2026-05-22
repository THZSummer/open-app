// LookUp 分类管理页面常量

// 搜索默认值
export const DEFAULT_SEARCH_VALUES = {
  classifyCode: '',
  classifyName: '',
  classifyDesc: '',
  status: ''
};

// 模态框标题
export const MODAL_TITLE_ADD = '新增分类';
export const MODAL_TITLE_EDIT = '编辑分类';

// 任务通知类型
export const TASK_NOTIFY_TYPE_IMPORT = 'import';
export const TASK_NOTIFY_TYPE_EXPORT = 'export';

// 状态映射（带样式类名）
export const STATUS_MAP = {
  1: { text: '有效', dotClass: null, labelClass: null },
  0: { text: '失效', dotClass: null, labelClass: null }
};

// 表单验证规则
export const FORM_VALIDATION_RULES = {
  classifyCode: [
    { required: true, message: '请输入分类编码' },
    { max: 100, message: '最多100个字符' }
  ],
  classifyName: [
    { required: true, message: '请输入分类名称' },
    { max: 100, message: '最多100个字符' }
  ],
  path: [
    { max: 100, message: '最多100个字符' }
  ],
  classifyDesc: [
    { max: 500, message: '最多500个字符' }
  ]
};

// 表格列宽度配置
export const TABLE_COLUMN_WIDTHS = {
  checkbox: 40,
  classifyCode: 120,
  classifyName: 140,
  path: 120,
  classifyDesc: undefined,
  status: 80,
  createBy: 90,
  createTime: 160,
  lastUpdateBy: 90,
  lastUpdateTime: 160,
  action: 140
};
