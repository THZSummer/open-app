import React from 'react';
import { Tag, Button, Space } from 'antd';

export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const AUTH_TYPE_MAP = {
  0: 'Cookie',
  1: 'SOA',
  2: 'APIG',
  3: 'IAM',
  4: '免认证',
  5: 'AKSK',
  6: 'CLITOKEN',
};

export const HTTP_METHOD_OPTIONS = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
];

export const API_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: 'API的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'API description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/api/xxx' },
  { value: 'rateLimit', label: '速率限制', placeholder: '100/minute' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const AUTH_TYPE_OPTIONS = [
  { value: 0, label: 'Cookie' },
  { value: 1, label: 'SOA' },
  { value: 2, label: 'APIG' },
  { value: 3, label: 'IAM' },
  { value: 4, label: '免认证' },
  { value: 5, label: 'AKSK' },
  { value: 6, label: 'CLITOKEN' },
];

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

export const getApiListColumns = ({ handleView, handleEdit, handleDelete }) => [
  {
    title: 'API名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 180,
    render: (text, record) => (
      <div>
        <div>{text}</div>
        <div style={{ fontSize: 12, color: '#999' }}>{record.nameEn}</div>
      </div>
    ),
  },
  {
    title: '分类',
    dataIndex: 'categoryName',
    key: 'categoryName',
    width: 120,
  },
  {
    title: '路径',
    dataIndex: 'path',
    key: 'path',
    width: 180,
    ellipsis: true,
    render: (text) => <code>{text}</code>,
  },
  {
    title: '方法',
    dataIndex: 'method',
    key: 'method',
    width: 80,
    render: (method) => <Tag color="blue">{method}</Tag>,
  },
  {
    title: '认证方式',
    dataIndex: 'authType',
    key: 'authType',
    width: 100,
    render: (authType) => {
      const label = AUTH_TYPE_MAP[authType] || 'SOA';
      return <Tag color="purple">{label}</Tag>;
    },
  },
  {
    title: 'Scope',
    dataIndex: 'permission',
    key: 'scope',
    width: 200,
    ellipsis: true,
    render: (permission) => {
      const scope = permission?.scope || '-';
      return <Tag color="cyan">{scope}</Tag>;
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    render: (status) => {
      const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
      return <Tag color={color}>{text}</Tag>;
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
    render: (_, record) => (
      <Space>
        <Button type="link" size="small" onClick={() => handleView(record)}>详情</Button>
        {record.docUrl && (
          <Button type="link" size="small" onClick={() => window.open(record.docUrl, '_blank')}>文档</Button>
        )}
        <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
        <Button type="link" size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
      </Space>
    ),
  },
];
