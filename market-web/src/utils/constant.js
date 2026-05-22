// 分页默认配置
export const DEFAULT_PAGINATION = {
  current: 1,
  pageSize: 10,
  total: 0
};

// 查询参数默认值
export const DEFAULT_QUERY_PARAMS = {
  pageNum: 1,
  pageSize: 10
};

// 页面大小选项
export const PAGE_SIZE_OPTIONS = ['10', '20', '50', '100', '200', '500', '1000'];

// 任务分页大小选项
export const TASK_PAGE_SIZE_OPTIONS = ['5', '10', '20', '50'];

// 状态选项
export const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 1, label: '有效' },
  { value: 0, label: '失效' }
];

// 通用状态映射（有效/失效）
export const COMMON_STATUS_MAP = {
  1: { text: '有效', color: 'success' },
  0: { text: '失效', color: 'default' }
};
