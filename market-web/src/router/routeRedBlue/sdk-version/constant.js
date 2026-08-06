/**
 * SDK版本管理页面常量
 */
import { renderAlwaysWithTooltip } from '../../../utils/common';

// 搜索默认值
export const DEFAULT_SEARCH_VALUES = {
  status: '',
  versionName: ''
};

// 状态枚举映射
export const STATUS_MAP = {
  1: { text: '已发布', color: '#00b578' },
  2: { text: '已废弃', color: '#fa8c16' }
};

// 状态选项
export const STATUS_OPTIONS = [
  { value: '', label: '全部状态' },
  { value: 1, label: '已发布' },
  { value: 2, label: '已废弃' }
];

// 模态框标题
export const MODAL_TITLE_CREATE = '上架新版本';
export const MODAL_TITLE_EDIT = '编辑版本';

/**
 * 获取SDK版本表格列配置
 */
export const getTableColumns = ({
  renderVersionName,
  renderPermission,
  renderStatus,
  renderAction
}) => {
  return [
    {
      title: '版本号',
      dataIndex: 'versionName',
      key: 'versionName',
      width: 100,
      render: renderVersionName
    },
    {
      title: '关联权限',
      dataIndex: 'permissionName',
      key: 'permissionName',
      width: 180,
      ellipsis: true,
      render: renderPermission
    },
    {
      title: '更新说明',
      dataIndex: 'updateNotes',
      key: 'updateNotes',
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: renderStatus
    },
    {
      title: '创建人',
      dataIndex: 'createBy',
      key: 'createBy',
      width: 90,
      ellipsis: true
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 160,
      ellipsis: true
    },
    {
      title: '更新人',
      dataIndex: 'lastUpdateBy',
      key: 'lastUpdateBy',
      width: 90,
      ellipsis: true,
      render: (text) => text || '-'
    },
    {
      title: '更新时间',
      dataIndex: 'lastUpdateTime',
      key: 'lastUpdateTime',
      width: 160,
      ellipsis: true,
      render: (text) => text || '-'
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      width: 200,
      fixed: 'right',
      render: renderAction
    }
  ];
};
