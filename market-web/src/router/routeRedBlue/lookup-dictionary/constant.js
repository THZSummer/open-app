/**
 * 数据字典管理页面常量
 * 定义搜索默认值、表单验证规则、表格配置等
 */

// 搜索默认值
export const DEFAULT_SEARCH_VALUES = {
  code: '',
  name: '',
  path: '',
  status: ''
};

// 模态框标题
export const MODAL_TITLE_ADD = '新增字典';
export const MODAL_TITLE_EDIT = '编辑字典';

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
  code: [
    { required: true, message: '请输入编码' },
    { max: 100, message: '最多100个字符' }
  ],
  name: [
    { required: true, message: '请输入名称' },
    { max: 100, message: '最多100个字符' }
  ],
  value: [
    { max: 200, message: '最多200个字符' }
  ],
  path: [
    { max: 100, message: '最多100个字符' }
  ],
  description: [
    { max: 4000, message: '最多4000个字符' }
  ]
};

// 表格列宽度配置
export const TABLE_COLUMN_WIDTHS = {
  checkbox: 40,
  code: 120,
  name: 140,
  value: 150,
  description: undefined,
  path: 120,
  status: 80,
  createBy: 90,
  createTime: 160,
  lastUpdateBy: 90,
  lastUpdateTime: 160,
  action: 140
};

// 分页配置
export const PAGINATION_CONFIG = {
  pageSizeOptions: ['10', '20', '50', '100', '200', '500', '1000'],
  defaultPageSize: 10
};

// 导入配置
export const IMPORT_CONFIG = {
  maxCount: 1000,
  acceptTypes: ['.xlsx', '.xls'],
  templateName: 'dictionary_import_template.xlsx'
};

// 导出配置
export const EXPORT_CONFIG = {
  maxCount: 1000
};
