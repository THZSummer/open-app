import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, AUTH_TYPE } from '../../utils/constants';
import {
  renderStatus,
  renderNeedApprovalStatus,
} from '../../utils/commonTableConfigs';

/**
 * API订阅管理Tab配置键
 */
export const TAB_CONFIG_SEARCH_KEY = 'CEC.open/Api.Drawer.TabsList';

/**
 * 生成API管理表格列配置
 *
 * @param {Object} handlers - 操作处理器
 * @param {Function} handlers.onViewDoc - 查看文档
 * @param {Function} handlers.onCopyUrl - 复制审批地址
 * @param {Function} handlers.onRevoke - 撤回申请
 * @param {Function} handlers.onRemove - 删除订阅
 */
export const getApiManagementColumns = (handlers) => {
  const { onViewDoc, onCopyUrl, onRevoke, onRemove } = handlers;

  return [
    {
      title: '权限名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      width: 180,
    },
    {
      title: 'scope',
      dataIndex: ['permission', 'scope'],
      key: 'scope',
      width: 180,
      ellipsis: true,
      render: (scope) => <code>{scope}</code>,
    },
    {
      title: '认证方式',
      dataIndex: ['api', 'authType'],
      key: 'authType',
      width: 100,
      render: (type) => AUTH_TYPE[type] || '-',
    },
    {
      title: '分类',
      dataIndex: ['category', 'nameCn'],
      key: 'category',
      width: 120,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: renderStatus,
    },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right',
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Button type="link" size="small" onClick={() => onViewDoc(record.api?.docUrl)}>查看文档</Button>
          {record.status === 0 && (
            <>
              <Button type="link" size="small" onClick={() => onCopyUrl(record)}>复制审批地址</Button>
              <Button type="link" size="small" onClick={() => onRevoke(record)}>撤回审核</Button>
            </>
          )}
          {record.status !== 0 && (
            <Button type="link" size="small" danger onClick={() => onRemove(record.id)}>删除</Button>
          )}
        </div>
      ),
    },
  ];
};

/**
 * 生成API权限选择器表格列配置
 *
 * @param {Object} handlers - 操作处理器
 * @param {Function} handlers.onViewDoc - 查看文档
 */
export const getApiPermissionDrawerColumns = ({ onViewDoc }) => [
  {
    title: '权限名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 180,
    render: (text, record) => {
      const name = record.nameCn || record.name || '-';
      return <span>{name}</span>;
    },
  },
  {
    title: 'Scope',
    dataIndex: 'scope',
    key: 'scope',
    width: 200,
    ellipsis: true,
    render: (scope) => <code>{scope || '-'}</code>,
  },
  {
    title: '是否需要审核',
    dataIndex: 'needApproval',
    key: 'needApproval',
    width: 120,
    render: renderNeedApprovalStatus,
  },
  {
    title: '订阅状态',
    dataIndex: 'isSubscribed',
    key: 'isSubscribed',
    width: 100,
    render: (isSubscribed, record) => {
      const { text, color } = SUBSCRIPTION_STATUS[record.status] || { text: '未订阅', color: 'default' };
      return <Tag color={color}>{text}</Tag>;
    }
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render: (_, record) => (
      <Button type="link" size="small" onClick={() => onViewDoc(record.docUrl)}>查看文档</Button>
    ),
  },
];
