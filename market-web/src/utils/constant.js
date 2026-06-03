// 分页默认配置
export const DEFAULT_PAGINATION = {
  pageNum: 1,
  pageSize: 10,
  total: 0
};

// 页面大小选项
export const PAGE_SIZE_OPTIONS = ['10', '20', '50'];

// 状态选项
export const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 1, label: '有效' },
  { value: 0, label: '失效' }
];

// 状态映射
export const STATUS_MAP = {
  1: { text: '有效', color: 'success' },
  0: { text: '失效', color: 'default' }
};
