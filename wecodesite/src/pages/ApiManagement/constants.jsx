import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, AUTH_TYPE } from '../../utils/constants';

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

export const IDENTITY_TABS = [
  { key: 'BUSINESS_IDENTITY', label: '业务身份权限' },
  { key: 'PERSONAL_IDENTITY', label: '个人身份权限' },
];

export const BUSINESS_BUSINESS_API_TABS = [
  { key: 'api_business_app_soa', label: 'SOA类型' },
  { key: 'api_business_app_apig', label: 'APIG类型' },
];

export const BUSINESS_PERSONAL_API_TABS = [
  { key: 'api_business_user_soa', label: 'SOA类型' },
  { key: 'api_business_user_apig', label: 'APIG类型' },
];

export const PERSONAL_API_TABS = [
  { key: 'api_personal_user_aksk', label: 'AKSK类型' },
];

export const DEFAULT_API_TYPE = {
  business: 'api_business_app_soa',
  personal: 'api_personal_user_aksk',
};

export const getApiManagementColumns = ({ handleOpenDoc, handleCopyApprovalAddress, handleWithdraw }) => [
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
    render: (status) => {
      const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
      return <Tag color={color}>{text}</Tag>;
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
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
      </div>
    ),
  },
];

export const getApiPermissionDrawerColumns = () => [
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
    render: (needApproval, record) => {
      const val = needApproval !== undefined ? needApproval : record.needReview;
      return val ?
        <Tag color="orange">需要审核</Tag> :
        <Tag color="green">无需审核</Tag>;
    },
  },
  {
    title: '订阅状态',
    dataIndex: 'isSubscribed',
    key: 'isSubscribed',
    width: 100,
    render: (isSubscribed) => {
      if (isSubscribed === 1) {
        return <Tag color="success">已订阅</Tag>;
      }
      return <Tag color="default">未订阅</Tag>;
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
    render: () => null,
  },
];
