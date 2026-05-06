import React from 'react';
import { Tag, Button, Space } from 'antd';
import { SUBSCRIPTION_STATUS, STATUS_MAP } from './constants';

export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const renderSubscriptionStatus = (isSubscribed) => {
  if (isSubscribed === 1) {
    return <Tag color="success">已订阅</Tag>;
  }
  return <Tag color="default">未订阅</Tag>;
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

export const createDrawerColumns = ({ nameLabel = '名称', identifierField = 'scope', identifierPlaceholder = '' }) => {
  return [
    {
      title: nameLabel,
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 200,
      render: (text, record) => {
        const name = record.nameCn || record.name || '-';
        const identifier = record[identifierField] || '';
        return (
          <div>
            <div>{name}</div>
            {identifier && (
              <span style={{ fontSize: 12, color: '#8c8c8c' }}>{identifier}</span>
            )}
          </div>
        );
      },
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
        <Button type="link" size="small" onClick={() => record.docUrl && window.open(record.docUrl, '_blank')}>
          查看文档
        </Button>
      ),
    },
  ];
};

export const createCallbackDrawerColumns = () => createDrawerColumns({
  nameLabel: '回调名称',
  identifierField: 'scope',
});

export const createEventDrawerColumns = () => createDrawerColumns({
  nameLabel: '事件名称',
  identifierField: 'topic',
});

export const createApiDrawerColumns = () => createDrawerColumns({
  nameLabel: '权限名称',
  identifierField: 'scope',
});

export const adminTableBaseColumn = ({ handleView, handleEdit, handleDelete }) => [
  {
    title: 'Scope',
    dataIndex: 'permission',
    key: 'scope',
    width: 200,
    ellipsis: true,
    render: (permission) => {
      const scope = permission?.scope || '-';
      return <Tag color='cyan'>{scope}</Tag>
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    render: (status) => {
      const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
      return <Tag color={color}>{text}</Tag>
    },
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
    render: (_, record) => {
      <Space>
        <Button type='link' size='small' onClick={() => handleView(record)}>详情</Button>
        {record.status !== 0 && <Button type='link' size='small' onClick={() => handleEdit(record)}>编辑</Button>}
        <Button type='link' size='small' danger onClick={() => handleDelete(record)}>删除</Button>
      </Space>
    }
  },
]
