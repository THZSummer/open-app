import React from 'react';
import { Tag } from 'antd';
import { AUTH_TYPE } from '../../../utils/constants';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

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

/**
 * 生成API列表的表格列配置
 *
 * @param {Object} options - 列配置选项
 * @param {Function} options.onView - 查看回调
 * @param {Function} options.onEdit - 编辑回调
 * @param {Function} options.onDelete - 删除回调
 * @returns {Array} 表格列配置数组
 */
export const getApiListColumns = ({ onView, onEdit, onDelete }) => {
  const baseColumn = adminTableBaseColumn({
    handleView: onView,
    handleEdit: onEdit,
    handleDelete: onDelete
  });

  return [
    {
      title: 'API名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 180,
      render: (nameCn, record) => (
        <div>
          <div>{nameCn}</div>
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
      title: '认证方式',
      dataIndex: 'authType',
      key: 'authType',
      width: 100,
      render: (authType) => {
        const label = AUTH_TYPE[authType] || 'SOA';
        return <Tag color="purple">{label}</Tag>;
      },
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
    ...baseColumn,
  ]
};
