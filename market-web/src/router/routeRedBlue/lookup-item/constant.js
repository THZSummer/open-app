// LookUp 项管理页面常量
import { renderAlwaysWithTooltip } from '../../../utils/common';

// 搜索默认值
export const DEFAULT_SEARCH_VALUES = {
  itemCode: '',
  itemName: '',
  status: undefined
};

// 详情面板模式
export const DETAIL_MODE_VIEW = 'view';
export const DETAIL_MODE_EDIT = 'edit';

// code 字段格式校验正则：字母、数字、下划线、点、横杠
export const CODE_PATTERN = /^[a-zA-Z0-9_.-]+$/;

// 正整数校验正则
export const POSITIVE_INTEGER_PATTERN = /^[1-9]\d*$/;

// 表单验证规则
export const FORM_VALIDATION_RULES = {
  itemCode: [
    { required: true, message: '请输入项编码' },
    { max: 100, message: '最多100个字符' },
    { pattern: CODE_PATTERN, message: '仅支持字母、数字、下划线、点、横杠' }
  ],
  itemName: [
    { required: true, message: '请输入项名称' },
    { max: 100, message: '最多100个字符' }
  ],
  itemValue: [
    { required: true, message: '请输入项值' },
    { max: 2000, message: '最多2000个字符' }
  ],
  itemIndex: [
    { pattern: POSITIVE_INTEGER_PATTERN, message: '请输入正整数' }
  ],
  itemDesc: [
    { max: 4000, message: '最多4000个字符' }
  ],
  itemAttr: [
    { max: 4000, message: '最多4000个字符' }
  ]
};

// 抽屉配置
export const DRAWER_WIDTH = 520;

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

/**
 * 获取项表格列配置
 * @param {Object} config - 配置对象
 * @param {Function} config.renderItemCode - 项编码列渲染函数
 * @param {Function} config.renderStatus - 状态列渲染函数
 * @param {Function} config.renderAction - 操作列渲染函数
 * @returns {Array} 表格列配置数组
 */
export const getTableColumns = ({
  renderItemCode,
  renderStatus,
  renderAction
}) => {
  return [
    {
      title: '项编码',
      dataIndex: 'itemCode',
      key: 'itemCode',
      width: 200,
      ellipsis: true,
      render: renderItemCode
    },
    {
      title: '项名称',
      dataIndex: 'itemName',
      key: 'itemName',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '项值',
      dataIndex: 'itemValue',
      key: 'itemValue',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '描述',
      dataIndex: 'itemDesc',
      key: 'itemDesc',
      width: 200,
      ellipsis: true,
      render: (text) => renderAlwaysWithTooltip(text)
    },
    {
      title: '排序',
      dataIndex: 'itemIndex',
      key: 'itemIndex',
      width: 60,
      align: 'center',
      render: (text) => text || '-'
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
