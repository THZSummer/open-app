import React from 'react';
import { Tag } from 'antd';
import { AUTH_TYPE } from '../../../utils/constants';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

/**
 * API删除二次确认弹窗配置
 */
export const ADMIN_API_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个API：${objectName}吗？`,
  impactText: '操作影响：删除后，该 API 将不可再被应用订阅或使用，相关权限配置会失效。',
};

/**
 * HTTP请求方法选项
 */
export const HTTP_METHOD_OPTIONS = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
  { value: 'PUT', label: 'PUT' },
  { value: 'DELETE', label: 'DELETE' },
  { value: 'PATCH', label: 'PATCH' },
];

/**
 * 认证方式选项
 */
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
      key: 'nameCn',
      dataIndex: 'nameCn',
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
      key: 'categoryName',
      dataIndex: 'categoryName',
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
        const label = AUTH_TYPE[authType] || 'SOA';
        return <Tag color="purple">{label}</Tag>;
      },
    },
    ...baseColumn,
  ]
}
