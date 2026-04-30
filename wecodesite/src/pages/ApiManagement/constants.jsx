import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, AUTH_TYPE } from '../../utils/constants';
import {
  renderStatus,
  renderNeedApprovalStatus,
  renderSubscriptionStatus,
  createApiDrawerColumns
} from '../../utils/commonTableConfigs';

export { NEED_REVIEW_OPTIONS, PAGE_SIZE_OPTIONS } from '../../utils/commonTableConfigs';

export const TAB_CONFIG_SEARCH_KEY = 'CEC.open/Api.Drawer.TabsList';

export const getApiManagementColumns = ({ handleOpenDoc, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
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
    width: 200,
    ellipsis: true,
    render: (code) => <code>{code}</code>,
  },
  {
    title: '认证方式',
    dataIndex: 'authType',
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
        <Button type="link" size="small" onClick={() => handleOpenDoc(record.api?.docUrl)}>查看文档</Button>
        {record.status === 0 && (
          <>
            <Button type="link" size="small" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
            <Button type="link" size="small" onClick={() => handleWithdraw(record)}>撤回审核</Button>
          </>
        )}
        {record.status !== 0 && (
          <Button type="link" size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
        )}
      </div>
    ),
  },
];

export const getApiPermissionDrawerColumns = ({ handleOpenDoc }) => [
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
      <Button type="link" size="small" onClick={() => handleOpenDoc(record.docUrl)}>查看文档</Button>
    ),
  },
];
