/**
 * ========================================
 * 连接器管理模块 - 常量配置
 * ========================================
 *
 * 定义连接器管理页面的配置信息、表格列配置、状态映射等
 */

import React from 'react';
import { Tag, Badge, Button, Space, Tooltip } from 'antd';

/**
 * 页面配置信息
 * 定义连接器管理页面的标题、描述等基本信息
 */
export const pageInfo = {
  title: '连接器管理',
  description: '管理平台的连接器配置，包括触发事件和执行动作的定义',
  addButtonText: '新建连接器',
};

/**
 * 连接器状态映射
 * 用于表格中的状态显示
 */
export const CONNECTOR_STATUS_MAP = {
  0: { text: '禁用', color: 'default' },
  1: { text: '启用', color: 'success' },
};

/**
 * 连接器状态选项
 * 用于搜索表单下拉选择
 */
export const connectorStatusOptions = [
  { value: 1, label: '启用' },
  { value: 0, label: '禁用' },
];

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * @param {Function} callbacks.handleEdit - 编辑回调
 * @param {Function} callbacks.handleDeleteClick - 删除按钮点击回调
 * @returns {Array} 表格列配置数组
 */
export const getConnectorColumns = ({ handleEdit, handleDeleteClick }) => [
  {
    title: '连接器名称',
    dataIndex: 'name',
    key: 'name',
    width: 200,
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true,
    render: (text) => (
      <Tooltip title={text}>
        <span>{text || '-'}</span>
      </Tooltip>
    ),
  },
  {
    title: '触发事件',
    dataIndex: 'triggers',
    key: 'triggers',
    width: 100,
    align: 'center',
    render: (triggers) => (
      <Tag color="blue">{triggers?.length || 0}</Tag>
    ),
  },
  {
    title: '执行动作',
    dataIndex: 'actions',
    key: 'actions',
    width: 100,
    align: 'center',
    render: (actions) => (
      <Tag color="green">{actions?.length || 0}</Tag>
    ),
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    align: 'center',
    render: (status) => {
      const config = CONNECTOR_STATUS_MAP[status] || { text: '未知', color: 'default' };
      return <Badge status={config.color === 'success' ? 'success' : 'default'} text={config.text} />;
    },
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
    width: 150,
    fixed: 'right',
    render: (_, record) => (
      <Space size="small">
        <Button
          type="link"
          size="small"
          onClick={() => handleEdit(record)}
        >
          查看
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
export const searchConfig = {
  placeholder: '搜索连接器名称或描述',
};
