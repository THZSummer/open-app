/**
 * ========================================
 * 连接器管理模块 - 常量配置
 * ========================================
 *
 * 定义连接器管理页面的配置信息、表格列配置、状态映射等
 */

import React from 'react';
import { Tag, Button, Space } from 'antd';
import { CONNECTOR_STATUS, CONNECTOR_STATUS_MAP } from '../../../utils/constants';
import { renderTooltipTextCell } from '../../../utils/commonTableConfigs';

/**
 * 连接器删除二次确认弹窗配置
 */
export const CONNECTOR_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个连接器：${objectName}吗？`,
  impactText: '操作影响：删除后，该连接器相关触发器、动作或连接流配置可能不可用。',
};

/**
 * 连接器失效二次确认弹窗配置
 */
export const CONNECTOR_DISABLE_SECOND_MODAL_INFO = {
  action: 'disable',
  getConfirmText: ({ objectName }) => `确认要将这个连接器：${objectName} 置为失效吗？`,
  impactText: '操作影响：失效后，该连接器将不可被引用，已配置的触发器、动作或连接流可能无法正常运行。',
};

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
 * 连接器状态筛选选项（搜索表单下拉使用）
 * 对齐 plan-api.md §1.8.1
 */
export const connectorStatusOptions = [
  { value: CONNECTOR_STATUS.ACTIVE, label: '有效可用' },
  { value: CONNECTOR_STATUS.INACTIVE, label: '有效不可用' },
  { value: CONNECTOR_STATUS.INVALID, label: '已失效' },
];

/**
 * 连接器类型映射
 * 用于表格中的类型显示
 */
export const CONNECTOR_TYPE_MAP = {
  1: { text: 'HTTP' },
};

/**
 * 渲染连接器类型列内容
 *
 * @param {number} connectorType - 连接器类型
 * @returns {React.ReactNode} 连接器类型展示节点
 */
const renderConnectorTypeCell = (connectorType) => {
  // 渲染连接器类型文本（默认占位）
  const config = CONNECTOR_TYPE_MAP[connectorType] || { text: '-' };
  return <span>{config.text}</span>;
};

/**
 * 渲染连接器状态列内容
 *
 * @param {number} status - 连接器状态
 * @returns {React.ReactNode} 连接器状态展示节点
 */
const renderConnectorStatusCell = (status) => {
  // 默认按"有效不可用"处理，避免未知状态展示异常
  const config = CONNECTOR_STATUS_MAP[status] ?? CONNECTOR_STATUS_MAP[CONNECTOR_STATUS.INACTIVE];
  return <Tag color={config.color}>{config.text}</Tag>;
};

/**
 * 获取连接器基础信息列配置
 *
 * @returns {Array} 基础信息列配置数组
 */
const getConnectorBaseColumns = () => [
  {
    title: '连接器 ID',
    dataIndex: 'connectorId',
    key: 'connectorId',
    width: 180,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '中文名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 160,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '英文名称',
    dataIndex: 'nameEn',
    key: 'nameEn',
    width: 160,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '类型',
    dataIndex: 'connectorType',
    key: 'connectorType',
    width: 90,
    align: 'center',
    ellipsis: true,
    render: renderConnectorTypeCell,
  },
  {
    title: '中文描述',
    dataIndex: 'descriptionCn',
    key: 'descriptionCn',
    width: 180,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '英文描述',
    dataIndex: 'descriptionEn',
    key: 'descriptionEn',
    width: 180,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
];

/**
 * 获取连接器时间审计列配置
 *
 * @returns {Array} 时间审计列配置数组
 */
const getConnectorAuditColumns = () => [
  {
    title: '创建人',
    dataIndex: 'createBy',
    key: 'createBy',
    width: 120,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 180,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '更新人',
    dataIndex: 'lastUpdateBy',
    key: 'lastUpdateBy',
    width: 120,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
  {
    title: '更新时间',
    dataIndex: 'lastUpdateTime',
    key: 'lastUpdateTime',
    width: 180,
    ellipsis: true,
    render: renderTooltipTextCell,
  },
];

/**
 * 获取连接器状态列配置
 *
 * @returns {Object} 状态列配置
 */
const getConnectorStatusColumn = () => ({
  title: '状态',
  dataIndex: 'status',
  key: 'status',
  width: 120,
  align: 'center',
  render: renderConnectorStatusCell,
});

/**
 * 渲染连接器失效按钮
 *
 * @param {Object} params - 渲染参数对象
 * 包含以下字段：
 * - canDisable: 是否可执行失效操作
 * - record: 当前连接器记录
 * - handleDisableClick: 失效按钮点击回调
 *
 * @returns {React.ReactNode} 失效按钮节点
 */
const renderConnectorDisableButton = (params) => {
  // 解构传入对象中需要使用的参数
  const { canDisable, record, handleDisableClick } = params;

  if (!canDisable) {
    return null;
  }

  return (
    <Button
      type="link"
      size="small"
      danger
      onClick={() => handleDisableClick(record)}
    >
      失效
    </Button>
  );
};

/**
 * 渲染连接器已失效状态操作按钮
 *
 * @param {Object} params - 渲染参数对象
 * 包含以下字段：
 * - isInvalid: 是否为已失效状态
 * - record: 当前连接器记录
 * - handleRestoreClick: 恢复按钮点击回调
 * - handleDeleteClick: 删除按钮点击回调
 *
 * @returns {React.ReactNode} 恢复和删除按钮节点
 */
const renderConnectorInvalidButtons = (params) => {
  // 解构传入对象中需要使用的参数
  const { isInvalid, record, handleRestoreClick, handleDeleteClick } = params;

  if (!isInvalid) {
    return null;
  }

  return (
    <>
      <Button type="link" size="small" onClick={() => handleRestoreClick(record)}>
        恢复
      </Button>
      <Button
        type="link"
        size="small"
        danger
        onClick={() => handleDeleteClick(record)}
      >
        删除
      </Button>
    </>
  );
};

/**
 * 渲染连接器操作列内容
 *
 * @param {Object} params - 渲染参数对象
 * 包含以下字段：
 * - record: 当前连接器记录
 * - handleEdit: 编辑回调
 * - handleDeleteClick: 删除按钮点击回调
 * - handleConfigClick: 配置按钮点击回调
 * - handleDisableClick: 失效按钮点击回调
 * - handleRestoreClick: 恢复按钮点击回调
 *
 * @returns {React.ReactNode} 操作列展示节点
 */
const renderConnectorActionCell = (params) => {
  // 解构传入对象中需要使用的参数
  const {
    record,
    handleEdit,
    handleDeleteClick,
    handleConfigClick,
    handleDisableClick,
    handleRestoreClick,
  } = params;
  // 当前连接器状态：1=有效不可用 / 2=有效可用 / 3=已失效
  const status = record.status;
  // 可操作"失效"按钮的状态：有效可用 / 有效不可用
  const canDisable = status === CONNECTOR_STATUS.ACTIVE || status === CONNECTOR_STATUS.INACTIVE;
  // 可操作"恢复+删除"按钮的状态：已失效
  const isInvalid = status === CONNECTOR_STATUS.INVALID;

  return (
    <Space size="small">
      <Button type="link" size="small" onClick={() => handleEdit(record)}>
        编辑
      </Button>
      <Button type="link" size="small" onClick={() => handleConfigClick(record)}>
        配置
      </Button>
      {renderConnectorDisableButton({ canDisable, record, handleDisableClick })}
      {renderConnectorInvalidButtons({
        isInvalid,
        record,
        handleRestoreClick,
        handleDeleteClick,
      })}
    </Space>
  );
};

/**
 * 获取连接器操作列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * 包含以下字段：
 * - handleEdit: 编辑回调
 * - handleDeleteClick: 删除按钮点击回调
 * - handleConfigClick: 配置按钮点击回调
 * - handleDisableClick: 失效按钮点击回调
 * - handleRestoreClick: 恢复按钮点击回调
 *
 * @returns {Object} 操作列配置
 */
const getConnectorActionColumn = (callbacks) => {
  // 解构传入对象中需要使用的回调
  const {
    handleEdit,
    handleDeleteClick,
    handleConfigClick,
    handleDisableClick,
    handleRestoreClick,
  } = callbacks;

  return {
    title: '操作',
    key: 'action',
    width: 260,
    fixed: 'right',
    render: (_, record) => renderConnectorActionCell({
      record,
      handleEdit,
      handleDeleteClick,
      handleConfigClick,
      handleDisableClick,
      handleRestoreClick,
    }),
  };
};

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * 包含以下字段：
 * - handleEdit: 编辑回调
 * - handleDeleteClick: 删除按钮点击回调（仅已失效状态展示）
 * - handleConfigClick: 配置按钮点击回调（跳转到配置页面）
 * - handleDisableClick: 失效按钮点击回调（仅正常状态展示，需二次确认）
 * - handleRestoreClick: 恢复按钮点击回调（仅已失效状态展示，直接执行）
 *
 * @returns {Array} 表格列配置数组
 */
export const getConnectorColumns = (callbacks) => {
  // 按业务分组组合连接器表格列
  return [
    ...getConnectorBaseColumns(),
    ...getConnectorAuditColumns(),
    getConnectorStatusColumn(),
    getConnectorActionColumn(callbacks),
  ];
};

/**
 * 搜索配置
 */
export const searchConfig = {
  placeholder: '搜索连接器中英文名称',
};