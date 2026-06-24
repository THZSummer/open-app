/**
 * ========================================
 * 运行管理模块 - 常量配置
 * ========================================
 *
 * 对齐 plan-api.md v7.0 §1.8.5 / §1.8.6 / §1.8.7：
 *   - 执行记录状态：0=成功 / 1=失败 / 2=超时
 *   - 触发方式：1=HTTP / 2=调试
 *   - 节点类型：1=trigger / 2=connector / 3=data_processor / 4=exit
 *   - 步骤状态：0=success / 1=failed / 2=timeout / 3=not_executed
 */
import React from 'react';
import {
  EXECUTION_STATUS,
  EXECUTION_STATUS_MAP,
  TRIGGER_TYPE_MAP,
} from '../../utils/constants';

/**
 * 页面配置信息
 */
export const pageInfo = {
  title: '运行管理',
  description: '查看订阅群与连接流的运行情况。',
};

/**
 * Tab 枚举
 */
export const TAB_KEYS = {
  SUBSCRIBE: 'subscribe',
  FLOW_RUN: 'flowRun',
};

/**
 * 订阅方式枚举
 */
export const SUBSCRIBE_TYPES = [
  { value: 'all', label: '全部' },
  { value: 'member_add', label: '群成员添加' },
  { value: 'api_add', label: '接口添加' },
];

/**
 * 连接流执行状态枚举（搜索下拉用，对齐 §1.8.5）
 */
export const FLOW_RUN_STATUS = [
  { value: EXECUTION_STATUS.SUCCESS, label: '成功' },
  { value: EXECUTION_STATUS.FAILED, label: '失败' },
  { value: EXECUTION_STATUS.TIMEOUT, label: '超时' },
];

/**
 * 渲染执行状态标签
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - status: 执行状态数字值（0/1/2）
 *
 * @returns {React.ReactNode} 状态标签
 */
const renderStatusTag = (params) => {
  const { status } = params;
  // 默认按未知状态展示
  const config = EXECUTION_STATUS_MAP[status] || { text: '-', color: 'default' };
  const className = status === EXECUTION_STATUS.SUCCESS
    ? 'status-tag status-tag-success'
    : 'status-tag status-tag-fail';
  return <span className={className}>{config.text}</span>;
};

/**
 * 渲染触发方式
 *
 * @param {number} triggerType - 触发方式枚举值
 * @returns {string} 展示文案
 */
const renderTriggerType = (triggerType) => {
  // 默认占位为 '-'
  const config = TRIGGER_TYPE_MAP[triggerType] || { text: '-' };
  return config.text;
};

/**
 * 渲染执行耗时（毫秒数 → "xx ms"）
 *
 * @param {number} durationMs - 执行耗时（毫秒）
 * @returns {string} 展示文案
 */
const renderDuration = (durationMs) => {
  if (durationMs == null) return '-';
  return `${durationMs} ms`;
};

/**
 * 获取「订阅群」表格列配置
 */
export const getSubscribeColumns = () => [
  { title: '群ID', dataIndex: 'groupId', key: 'groupId' },
  { title: '订阅时间', dataIndex: 'subscribeTime', key: 'subscribeTime' },
  { title: '订阅账号', dataIndex: 'subscribeAccount', key: 'subscribeAccount' },
  { title: '订阅方式', dataIndex: 'subscribeTypeLabel', key: 'subscribeTypeLabel' },
];

/**
 * 获取「连接流执行」表格列配置
 * 字段命名与 plan-api.md §3.7 #49 响应对齐
 *
 * @param {Object} callbacks - 回调函数对象
 * 包含以下字段：
 * - onShowDetail: 点击详情按钮的回调，参数为 executionId
 *
 * @returns {Array} 表格列定义
 */
export const getFlowRunColumns = (callbacks) => {
  const { onShowDetail } = callbacks;
  return [
    { title: '执行ID', dataIndex: 'executionId', key: 'executionId' },
    { title: '连接流名称', dataIndex: 'flowNameCn', key: 'flowNameCn' },
    { title: '版本号', dataIndex: 'flowVersionNumber', key: 'flowVersionNumber' },
    { title: '触发时间', dataIndex: 'triggerTime', key: 'triggerTime' },
    {
      title: '触发方式',
      dataIndex: 'triggerType',
      key: 'triggerType',
      render: (value) => renderTriggerType(value),
    },
    {
      title: '触发账号',
      dataIndex: 'triggerAccount',
      key: 'triggerAccount',
    },
    {
      title: '执行时长',
      dataIndex: 'durationMs',
      key: 'durationMs',
      render: (value) => renderDuration(value),
    },
    {
      title: '执行状态',
      dataIndex: 'status',
      key: 'status',
      // 状态以彩色标签呈现
      render: (status) => renderStatusTag({ status }),
    },
    {
      title: '操作',
      key: 'action',
      // 操作列：详情按钮（文字按钮风格）
      render: (_, record) => (
        <button
          type="button"
          className="btn-link"
          onClick={() => onShowDetail(record.executionId)}
        >
          详情
        </button>
      ),
    },
  ];
};
