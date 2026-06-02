/**
 * ========================================
 * 连接器管理模块 - 常量配置
 * ========================================
 *
 * 定义连接器管理页面的配置信息、表格列配置、状态映射等
 */

import React from 'react';
import { Button, Space, Tooltip } from 'antd';

/**
 * 渲染文本单元格
 * 用于表格中显示文本内容，支持tooltip和空值显示
 * @param {string} text - 单元格文本
 * @returns {React.ReactNode} 渲染的单元格组件
 */
const renderTextCell = (text) => (
  <Tooltip title={text}>
    <span>{text || '-'}</span>
  </Tooltip>
);

/**
 * 页面配置信息
 * 定义连接器管理页面的标题、描述等基本信息
 */
export const pageInfo = {
  addButtonText: '新建连接器',
  description: '管理平台的连接器配置，包括触发事件和执行动作的定义',
  title: '连接器管理',
};

/**
 * 删除确认弹窗配置
 */
export const deleteConnectorModalInfo = {
  confirmButtonText: '确认删除',
  content: '删除后将无法恢复，确认要删除这个连接器吗？',
  dangerColor: '#ff4d4f',
  loadingText: '删除中...',
  title: '确认删除连接器吗？',
};

/**
 * 连接器类型映射
 * 用于表格中的类型显示
 */
export const CONNECTOR_TYPE_MAP = {
  1: { text: 'HTTP' },
};

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * @param {Function} callbacks.handleEdit - 编辑回调
 * @param {Function} callbacks.handleDeleteClick - 删除按钮点击回调
 * @param {Function} callbacks.handleConfigClick - 配置按钮点击回调（跳转到配置页面）
 * @returns {Array} 表格列配置数组
 */
export const getConnectorColumns = ({ handleEdit, handleDeleteClick, handleConfigClick }) => [
  {
    title: '中文名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 200,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '英文名称',
    dataIndex: 'nameEn',
    key: 'nameEn',
    width: 200,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '类型',
    dataIndex: 'connectorType',
    key: 'connectorType',
    width: 100,
    align: 'center',
    ellipsis: true,
    render: (connectorType) => {
      const config = CONNECTOR_TYPE_MAP[connectorType] || { text: '-' };
      return <span>{config.text}</span>;
    },
  },
  {
    title: '中文描述',
    dataIndex: 'descriptionCn',
    key: 'descriptionCn',
    width: 200,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '英文描述',
    dataIndex: 'descriptionEn',
    key: 'descriptionEn',
    width: 200,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '更新时间',
    dataIndex: 'lastUpdateTime',
    key: 'lastUpdateTime',
    width: 180,
    ellipsis: true,
    render: renderTextCell,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right',
    render: (_, record) => (
      <Space size="small">
        <Button type="link" size="small" onClick={() => handleEdit(record)} >
          编辑
        </Button>
        <Button type="link" size="small" onClick={() => handleConfigClick(record)}>
          配置
        </Button>
        <Button type="link" size="small" danger onClick={() => handleDeleteClick(record.id)}>
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
  placeholder: '搜索连接器中英文名称',
};
