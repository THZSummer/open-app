/**
 * ========================================
 * 连接器管理模块 - 常量配置
 * ========================================
 *
 * 定义连接器管理页面的配置信息、表格列配置、状态映射等
 */

import React from 'react';
import { Badge, Button, Space, Tooltip } from 'antd';
import { CONNECTOR_STATUS, CONNECTOR_STATUS_MAP } from '../../../utils/constants';

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
 * 渲染文本单元格
 * 用于表格中显示文本内容，支持tooltip和空值显示
 *
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
  const {
    handleEdit,
    handleDeleteClick,
    handleConfigClick,
    handleDisableClick,
    handleRestoreClick,
  } = callbacks;

  return [
    {
      title: '连接器 ID',
      dataIndex: 'connectorId',
      key: 'connectorId',
      width: 180,
      ellipsis: true,
      render: renderTextCell,
    },
    {
      title: '中文名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 160,
      ellipsis: true,
      render: renderTextCell,
    },
    {
      title: '英文名称',
      dataIndex: 'nameEn',
      key: 'nameEn',
      width: 160,
      ellipsis: true,
      render: renderTextCell,
    },
    {
      title: '类型',
      dataIndex: 'connectorType',
      key: 'connectorType',
      width: 90,
      align: 'center',
      ellipsis: true,
      render: (connectorType) => {
        // 渲染连接器类型文本（默认占位）
        const config = CONNECTOR_TYPE_MAP[connectorType] || { text: '-' };
        return <span>{config.text}</span>;
      },
    },
    {
      title: '中文描述',
      dataIndex: 'descriptionCn',
      key: 'descriptionCn',
      width: 180,
      ellipsis: true,
      render: renderTextCell,
    },
    {
      title: '英文描述',
      dataIndex: 'descriptionEn',
      key: 'descriptionEn',
      width: 180,
      ellipsis: true,
      render: renderTextCell,
    },
    {
      title: '创建者',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: 120,
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
      title: '更新人',
      dataIndex: 'lastUpdateUserName',
      key: 'lastUpdateUserName',
      width: 120,
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
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      align: 'center',
      render: (status) => {
        // 默认按"有效不可用"处理，避免未知状态展示异常
        const config = CONNECTOR_STATUS_MAP[status] ?? CONNECTOR_STATUS_MAP[CONNECTOR_STATUS.INACTIVE];
        return <Badge color={config.color} text={config.text} />;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 220,
      fixed: 'right',
      render: (_, record) => {
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
            {canDisable && (
              <Button
                type="link"
                size="small"
                danger
                onClick={() => handleDisableClick(record)}
              >
                失效
              </Button>
            )}
            {isInvalid && (
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
            )}
          </Space>
        );
      },
    },
  ];
};

/**
 * 搜索配置
 */
export const searchConfig = {
  placeholder: '搜索连接器中英文名称',
};