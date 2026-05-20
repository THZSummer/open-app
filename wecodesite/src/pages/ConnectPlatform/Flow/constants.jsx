/**
 * ========================================
 * 连接流管理模块 - 常量配置
 * ========================================
 *
 * 定义连接流管理页面的配置信息、表格列配置、状态映射等
 */

import { Badge, Button, Space } from 'antd';

/**
 * 页面配置信息
 */
export const pageInfo = {
  title: '连接流管理',
  description: '管理平台的连接流配置，支持可视化流程编排',
  addButtonText: '新建连接流',
};

/**
 * 流程状态映射
 */
export const FLOW_STATUS_MAP = {
  0: {
    text: '草稿',
    color: 'default',
    status: 'default'
  },
  1: {
    text: '已发布',
    color: 'success',
    status: 'success'
  },
  3: {
    text: '已下线',
    color: 'error',
    status: 'error'
  },
};

/**
 * 连接流状态选项
 * 用于搜索表单下拉选择
 */
export const flowStatusOptions = [
  { value: 1, label: '已发布' },
  { value: 0, label: '草稿' },
  { value: 3, label: '已下线' },
];

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * @param {Function} callbacks.handleEdit - 编辑回调
 * @param {Function} callbacks.handleDeleteClick - 删除按钮点击回调
 * @param {Function} callbacks.handleView - 查看按钮点击回调
 * @returns {Array} 表格列配置数组
 */
export const getFlowColumns = ({ handleEdit, handleDeleteClick, handleView }) => [
  {
    title: '流程名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    align: 'center',
    render: (status) => {
      const config = FLOW_STATUS_MAP[status] || { text: status, color: 'default', status: 'default' };
      return <Badge status={config.status} text={config.text} />;
    },
  },
  {
    title: '节点数量',
    dataIndex: 'nodes',
    key: 'nodes',
    width: 100,
    align: 'center',
    render: (nodes) => (
      <span style={{ fontWeight: 500 }}>{nodes?.length || 0}</span>
    ),
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
  },
  {
    title: '更新时间',
    dataIndex: 'updatedAt',
    key: 'updatedAt',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 280,
    fixed: 'right',
    render: (_, record) => (
      <Space size="small">
        <Button
          type="link"
          size="small"
          onClick={() => handleView(record)}
        >
          查看
        </Button>
        <Button
          type="link"
          size="small"
          onClick={() => handleEdit(record)}
        >
          编辑
        </Button>
        <Button
          type="link"
          size="small"
          danger
          onClick={() => handleDeleteClick(record.id)}
        >
          删除
        </Button>
      </Space>
    ),
  },
];

/**
 * 搜索配置
 */
export const flowSearchConfig = {
  placeholder: '搜索流程名称或描述',
};
