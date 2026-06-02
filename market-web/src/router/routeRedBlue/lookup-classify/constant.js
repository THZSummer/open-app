import { renderAlwaysWithTooltip } from '../../../utils/common';

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

// code 字段格式校验正则：字母、数字、下划线、点、横杠
export const CODE_PATTERN = /^[a-zA-Z0-9_.-]+$/;

// 表单验证规则
export const FORM_VALIDATION_RULES = {
  classifyCode: [
    { required: true, message: '请输入分类编码' },
    { max: 100, message: '最多100个字符' },
    { pattern: CODE_PATTERN, message: '仅支持字母、数字、下划线、点、横杠' }
  ],
  classifyName: [
    { required: true, message: '请输入分类名称' },
    { max: 100, message: '最多100个字符' }
  ],
  path: [
    { required: true, message: '请输入分类路径' },
    { max: 100, message: '最多100个字符' }
  ],
  classifyDesc: [
    { max: 4000, message: '最多4000个字符' }
  ]
};

/**
 * 获取分类表格列配置
 * @param {Object} config - 配置对象
 * @param {Function} config.renderClassifyCode - 类别编码列渲染函数
 * @param {Function} config.renderStatus - 状态列渲染函数
 * @param {Function} config.renderAction - 操作列渲染函数
 * @returns {Array} 表格列配置数组
 */
export const getTableColumns = ({
  renderClassifyCode,
  renderStatus,
  renderAction
}) => {
  return [
    {
      title: '类别编码',
      dataIndex: 'classifyCode',
      key: 'classifyCode',
      width: 200,
      ellipsis: true,
      render: renderClassifyCode
    },
    {
      title: '类别名称',
      dataIndex: 'classifyName',
      key: 'classifyName',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '描述',
      dataIndex: 'classifyDesc',
      key: 'classifyDesc',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: renderStatus
    },
    {
      title: '创建人',
      dataIndex: 'createBy',
      key: 'createBy',
      width: 180,
      ellipsis: true
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      ellipsis: true,
      width: 160
    },
    {
      title: '修改人',
      dataIndex: 'lastUpdateBy',
      key: 'lastUpdateBy',
      width: 180,
      ellipsis: true,
      render: (text) => text || '-'
    },
    {
      title: '修改时间',
      dataIndex: 'lastUpdateTime',
      key: 'lastUpdateTime',
      ellipsis: true,
      width: 160,
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
