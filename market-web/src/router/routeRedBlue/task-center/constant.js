// 任务中心页面常量

// 任务类型映射
export const TASK_TYPE_MAP = {
  1: { text: '导入', type: 'import', className: null },
  2: { text: '导出', type: 'export', className: null }
};

// 业务类型映射
export const BIZ_TYPE_MAP = {
  1: { text: 'LookUp', type: 'lookup', className: null }
};

// 任务状态映射
export const STATUS_MAP = {
  0: { text: '待处理', dotClass: null, labelClass: null },
  1: { text: '处理中', dotClass: null, labelClass: null },
  2: { text: '已完成', dotClass: null, labelClass: null },
  3: { text: '失败', dotClass: null, labelClass: null }
};

// 搜索过滤器默认值
export const DEFAULT_FILTERS = {
  taskType: '',
  bizType: '',
  status: ''
};

// 任务类型选项
export const TASK_TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: '1', label: '导入' },
  { value: '2', label: '导出' }
];

// 业务类型选项
export const BIZ_TYPE_OPTIONS = [
  { value: '', label: '全部' },
  { value: '1', label: 'LookUp' }
];

// 状态选项
export const STATUS_OPTIONS = [
  { value: '', label: '全部' },
  { value: '0', label: '待处理' },
  { value: '1', label: '处理中' },
  { value: '2', label: '已完成' },
  { value: '3', label: '失败' }
];

// 表格列宽度配置
export const TABLE_COLUMN_WIDTHS = {
  taskId: 180,
  bizType: 100,
  taskType: 80,
  status: 100,
  result: undefined,
  createTime: 160,
  lastUpdateTime: 160,
  action: 100
};
