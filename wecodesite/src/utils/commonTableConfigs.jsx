import React from 'react';
import { Tag, Button, Space, Tooltip } from 'antd';
import { SUBSCRIPTION_STATUS, STATUS_MAP } from './constants';
import { openUrl } from './common';

export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

/**
 * 渲染带 Tooltip 的文本单元格
 * 用于表格中显示文本内容，支持 Tooltip 和空值占位展示
 *
 * @param {string} text - 单元格文本
 * @returns {React.ReactNode} 渲染后的文本单元格
 */
export const renderTooltipTextCell = (text) => (
  <Tooltip title={text}>
    <span>{text || '-'}</span>
  </Tooltip>
);

export const renderSubscriptionStatus = (isSubscribed) => {
  const { text, color } = SUBSCRIPTION_STATUS[isSubscribed];
  return <Tag color={color}>{text}</Tag>;
};

export const renderNeedApprovalStatus = (needApproval, record) => {
  const val = needApproval !== undefined ? needApproval : record?.needReview;
  return val ?
    <Tag color="orange">需要审核</Tag> :
    <Tag color="green">无需审核</Tag>;
};

export const renderStatus = (status) => {
  const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
  return <Tag color={color}>{text}</Tag>;
};

export const createDrawerColumns = (type) => {
  const baseColumns = [
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
      title: 'ScopeId',
      dataIndex: 'scope',
      key: 'scope',
      width: 150,
      ellipsis: true,
      render: (scope) => <code>{scope || '-'}</code>,
    },
    {
      title: '事件Topic',
      dataIndex: ['resource', 'topic'],
      key: 'topic',
      width: 150,
      render: (topic) => <code>{topic || '-'}</code>,
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
      render: renderSubscriptionStatus,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => openUrl(record.resource.docUrl)}>
          查看文档
        </Button>
      ),
    },
  ];
  if (type === 'event') {
    return baseColumns;
  } else {
    return baseColumns.filter(item => item.key !== 'topic');
  }
};

export const adminTableBaseColumn = ({ handleView, handleEdit, handleDelete }) => [
  {
    title: 'ScopeId',
    dataIndex: 'permission',
    key: 'scope',
    width: 200,
    ellipsis: true,
    render: (permission) => {
      const scope = permission?.scope || '-';
      return <Tag color='cyan'>{scope}</Tag>;
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
        <Button type='link' size='small' onClick={() => handleView(record.id)}>详情</Button>
        {record.status !== 0 && <Button type='link' size='small' onClick={() => handleEdit(record.id)}>编辑</Button>}
        <Button type='link' size='small' danger onClick={() => handleDelete(record)}>删除</Button>
      </Space>
    )
  },
]
