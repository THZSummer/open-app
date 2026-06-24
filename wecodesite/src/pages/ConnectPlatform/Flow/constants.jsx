/**
 * ========================================
 * 连接流管理模块 - 常量配置
 * ========================================
 *
 * 定义连接流管理页面的配置信息、表格列配置、状态映射、更多菜单规则等
 * 整改依据：连接流列表需求设计说明书 V1.3
 */
import React from 'react';
import { Badge, Button, Space, Tooltip, Dropdown } from 'antd';
import { DownOutlined } from '@ant-design/icons';
import { FLOW_LIFECYCLE_STATUS, FLOW_LIFECYCLE_STATUS_MAP } from '../../../utils/constants';

/**
 * 页面配置信息
 */
export const pageInfo = {
  addButtonText: '新建连接流',
  description: '管理平台的连接流配置，支持可视化流程编排',
  title: '连接流管理',
};

/**
 * 状态枚举（重导出，方便外部使用）
 */
export const FLOW_STATUS_MAP = FLOW_LIFECYCLE_STATUS_MAP;

/**
 * 连接流生命周期状态选项（搜索表单下拉使用）
 * 对齐 plan-api.md §1.8.3：1=已停止 / 2=运行中 / 3=已失效
 */
export const flowStatusOptions = [
  { value: FLOW_LIFECYCLE_STATUS.STOPPED, label: '已停止' },
  { value: FLOW_LIFECYCLE_STATUS.RUNNING, label: '运行中' },
  { value: FLOW_LIFECYCLE_STATUS.INVALID, label: '已失效' },
];

/**
 * 渲染文本（带Tooltip）
 *
 * @param {string} text - 要渲染的文本
 * @returns {React.ReactNode} 包裹在Tooltip中的文本
 */
const renderText = (text) => (
  <Tooltip title={text}>
    <span>{text || '-'}</span>
  </Tooltip>
);

/**
 * 根据状态获取更多菜单项 key 列表
 * 对齐 plan-api.md §1.8.3 状态枚举：
 * - 已停止（1）：复制流、复制ID、启动、部署、失效
 * - 运行中（2）：复制流、复制ID、部署、停止
 * - 已失效（3）：复制ID、恢复、删除
 * - 其他/未知：仅复制ID
 *
 * @param {number} status - 连接流生命周期状态
 * @returns {string[]} 菜单项 key 数组
 */
export const getMoreMenuKeys = (status) => {
  switch (status) {
    case FLOW_LIFECYCLE_STATUS.STOPPED:
      // 已停止：可启动、可部署、可失效
      return ['copy', 'copyId', 'start', 'deploy', 'disable'];
    case FLOW_LIFECYCLE_STATUS.RUNNING:
      // 运行中：可部署、可停止
      return ['copy', 'copyId', 'deploy', 'stop'];
    case FLOW_LIFECYCLE_STATUS.INVALID:
      // 已失效：可恢复、可删除
      return ['copyId', 'restore', 'delete'];
    default:
      // 未知状态仅保留低风险定位能力
      return ['copyId'];
  }
};

/**
 * 更多菜单项基础配置
 * key 与 label 映射，danger 表示危险操作
 */
const MORE_MENU_ITEM_BASE = {
  copy: { label: '复制流' },
  copyId: { label: '复制 ID' },
  start: { label: '启动' },
  deploy: { label: '部署' },
  stop: { label: '停止', danger: true },
  disable: { label: '失效', danger: true },
  restore: { label: '恢复' },
  delete: { label: '删除', danger: true },
};

/**
 * 根据状态构造 antd Dropdown 的 menu.items
 *
 * @param {Object} params - 构造参数
 * 包含以下字段：
 * - status: 当前连接流生命周期状态
 * - record: 当前连接流记录
 * - onMenuClick: 菜单点击回调 (key, record) => void
 *
 * @returns {Array} antd Dropdown items 配置
 */
const buildMoreMenuItems = (params) => {
  // 解构传入对象中需要使用的参数
  const { status, record, onMenuClick } = params;

  const keys = getMoreMenuKeys(status);
  return keys.map((key) => ({
    key,
    label: MORE_MENU_ITEM_BASE[key].label,
    danger: MORE_MENU_ITEM_BASE[key].danger,
    onClick: () => onMenuClick(key, record),
  }));
};

/**
 * 获取表格列配置
 *
 * @param {Object} callbacks - 回调函数对象
 * 包含以下字段：
 * - handleEdit: 编辑回调
 * - handleConfig: 配置按钮点击回调
 * - handleMoreMenuClick: 更多菜单点击回调 (key, record) => void
 *
 * @returns {Array} 表格列配置数组
 */
export const getFlowColumns = (callbacks) => {
  const { handleEdit, handleConfig, handleMoreMenuClick } = callbacks;

  return [
    {
      title: '连接流 ID',
      dataIndex: 'id',
      key: 'id',
      width: 180,
      ellipsis: true,
      render: renderText,
    },
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
      render: (status) => {
        // 未知状态展示默认占位
        const config = FLOW_LIFECYCLE_STATUS_MAP[status] || { text: '-', color: 'default' };
        return <Badge color={config.color} text={config.text} />;
      },
    },
    {
      title: '创建者',
      dataIndex: 'createUserName',
      key: 'createUserName',
      width: 120,
      ellipsis: true,
      render: renderText,
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
      title: '更新人',
      dataIndex: 'lastUpdateUserName',
      key: 'lastUpdateUserName',
      width: 120,
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
      width: 200,
      fixed: 'right',
      render: (_, record) => {
        // 操作列固定展示：编辑、配置、更多
        const items = buildMoreMenuItems({
          status: record.lifecycleStatus,
          record,
          onMenuClick: handleMoreMenuClick,
        });

        return (
          <Space size="small">
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              编辑
            </Button>
            <Button type="link" size="small" onClick={() => handleConfig(record)}>
              配置
            </Button>
            <Dropdown menu={{ items }} trigger={['click']}>
              <Button type="link" size="small">
                更多 <DownOutlined />
              </Button>
            </Dropdown>
          </Space>
        );
      },
    },
  ];
};

/**
 * 搜索配置
 */
export const flowSearchConfig = {
  placeholder: '搜索连接流中英文名称',
};

/**
 * 连接流删除二次确认弹窗配置
 */
export const FLOW_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个连接流：${objectName} 吗？`,
  impactText: '操作影响：删除后将无法恢复，该连接流的版本、配置与运行记录会一并清理。',
};

/**
 * 连接流停止二次确认弹窗配置
 */
export const FLOW_STOP_SECOND_MODAL_INFO = {
  action: 'stop',
  getConfirmText: ({ objectName }) => `确认要停止这个连接流：${objectName} 吗？`,
  impactText: '操作影响：停止后将无法接收 HTTP 触发请求，部署版本绑定保持不变，可后续启动恢复。',
};

/**
 * 连接流失效二次确认弹窗配置
 * 备注：失效能力后端暂未支持
 */
export const FLOW_DISABLE_SECOND_MODAL_INFO = {
  action: 'disable',
  getConfirmText: ({ objectName }) => `确认要将这个连接流：${objectName} 置为失效吗？`,
  impactText: '操作影响：失效后，该连接流将无法被使用，已订阅的触发与下游能力会同步停用。',
};