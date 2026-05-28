/**
 * ========================================
 * 连接流管理模块 - 常量配置
 * ========================================
 *
 * 定义连接流管理页面的配置信息、表格列配置、状态映射等
 */
import React from 'react';
import { Badge, Button, Space, Tooltip } from 'antd';

/**
 * 页面配置信息
 */
export const pageInfo = {
  addButtonText: '新建连接流',
  description: '管理平台的连接流配置，支持可视化流程编排',
  title: '连接流管理',
};

/**
 * 连接流生命周期状态映射
 */
export const FLOW_STATUS_MAP = {
  0: {
    text: '未部署',
    color: 'default',
    status: 'default'
  },
  1: {
    text: '运行中',
    color: 'success',
    status: 'success'
  },
  2: {
    text: '已停止',
    color: 'error',
    status: 'error'
  },
};

/**
 * 连接流生命周期状态选项
 * 用于搜索表单下拉选择
 */
export const flowStatusOptions = [
  { value: 0, label: '未部署' },
  { value: 1, label: '运行中' },
  { value: 2, label: '已停止' },
];

/**
 * 渲染文本（带Tooltip）
 * @param {string} text - 要渲染的文本
 * @returns {ReactNode} 包裹在Tooltip中的文本
 */
const renderText = (text) => (
  <Tooltip title={text}>
    {text}
  </Tooltip>
);

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * @param {Function} callbacks.handleEdit - 编辑回调
 * @param {Function} callbacks.handleView - 查看按钮点击回调
 * @param {Function} callbacks.handleActionClick - 操作按钮点击回调（统一处理删除/停止/启动）
 * @returns {Array} 表格列配置数组
 */
export const getFlowColumns = ({ handleEdit, handleView, handleActionClick }) => [
  {
    title: '中文名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 150,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '英文名称',
    dataIndex: 'nameEn',
    key: 'nameEn',
    width: 150,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '中文描述',
    dataIndex: 'descriptionCn',
    key: 'descriptionCn',
    width: 180,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '英文描述',
    dataIndex: 'descriptionEn',
    key: 'descriptionEn',
    width: 180,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '状态',
    dataIndex: 'lifecycleStatus',
    key: 'lifecycleStatus',
    width: 100,
    align: 'center',
    ellipsis: true,
    render: (status) => {
      const config = FLOW_STATUS_MAP[status] || { text: status, color: 'default', status: 'default' };
      return <Badge status={config.status} text={config.text} />;
    },
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '更新时间',
    dataIndex: 'lastUpdateTime',
    key: 'lastUpdateTime',
    width: 180,
    ellipsis: true,
    render: renderText,
  },
  {
    title: '操作',
    key: 'action',
    width: 230,
    fixed: 'right',
    render: (_, record) => {
      const lifecycleStatus = record.lifecycleStatus;
      const isRunning = lifecycleStatus === 1;
      const isStopped = lifecycleStatus === 2;
      const isUndeployed = lifecycleStatus === 0;

      return (
        <Space size="small">
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Button type="link" size="small" onClick={() => handleView(record.id)}>
            配置
          </Button>
          {isRunning && (
            <Button type="link" size="small" onClick={() => handleActionClick(record, 'stop')}>
              停止
            </Button>
          )}
          {isStopped && (
            <Button type="link" size="small" onClick={() => handleActionClick(record, 'start')}>
              启动
            </Button>
          )}
          {(isStopped || isUndeployed) && (
            <Button type="link" size="small" danger onClick={() => handleActionClick(record, 'delete')}>
              删除
            </Button>
          )}
        </Space>
      );
    },
  },
];

/**
 * 搜索配置
 */
export const flowSearchConfig = {
  placeholder: '搜索流程名称或描述',
};

/**
 * 删除二次确认配置
 */
export const deleteFlowModalInfo = {
  confirmButtonText: '确认删除',
  content: '删除后将无法恢复，确认要删除这个连接流吗？',
  dangerColor: '#ff4d4f',
  loadingText: '删除中...',
  title: '确认删除连接流吗？',
};

/**
 * 停止二次确认配置
 */
export const stopFlowModalInfo = {
  title: '确认停止连接流吗？',
  content: '停止后将无法接收HTTP触发请求，确认要停止这个连接流吗？',
  confirmButtonText: '确认停止',
  loadingText: '停止中...',
  dangerColor: '#ff4d4f'
};
