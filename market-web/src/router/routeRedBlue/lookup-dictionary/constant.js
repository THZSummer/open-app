/**
 * 数据字典管理页面常量
 * 定义搜索默认值、表单验证规则、表格配置等
 */
import { renderAlwaysWithTooltip } from '../../../utils/common';

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

// code 字段格式校验正则：字母、数字、下划线、点、横杠、斜杠、星号
export const CODE_PATTERN = /^[a-zA-Z0-9_./*-]+$/;

// 表单验证规则
export const FORM_VALIDATION_RULES = {
  code: [
    { required: true, message: '请输入编码' },
    { max: 100, message: '最多100个字符' },
    { pattern: CODE_PATTERN, message: '仅支持字母、数字、下划线、点、横杠、斜杠、星号' }
  ],
  name: [
    { required: true, message: '请输入名称' },
    { max: 100, message: '最多100个字符' }
  ],
  value: [
    { required: true, message: '请输入值' },
    { max: 2000, message: '最多2000个字符' }
  ],
  path: [
    { required: true, message: '请输入路径' },
    { max: 100, message: '最多100个字符' }
  ],
  description: [
    { max: 4000, message: '最多4000个字符' }
  ]
};

/**
 * 获取字典表格列配置
 * @param {Object} config - 配置对象
 * @param {Function} config.renderCode - 编码列渲染函数
 * @param {Function} config.renderStatus - 状态列渲染函数
 * @param {Function} config.renderAction - 操作列渲染函数
 * @returns {Array} 表格列配置数组
 */
export const getTableColumns = ({
  renderCode,
  renderStatus,
  renderAction
}) => {
  return [
    {
      title: '编码',
      dataIndex: 'code',
      key: 'code',
      width: 200,
      ellipsis: true,
      render: renderCode
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '值',
      dataIndex: 'value',
      key: 'value',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
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
