import React from 'react';
import { Tag } from 'antd';
import { AUTH_TYPE } from '../../../utils/constants';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

export const HTTP_METHOD_OPTIONS = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
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

export const getApiListColumns = ({ handleView, handleEdit, handleDelete }) => {
  const baseColumn = adminTableBaseColumn({ handleView, handleEdit, handleDelete });
  return [
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
    ...baseColumn,
  ]
};
