import React from 'react';
import { Tag, Button, Space } from 'antd';

export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const CALLBACK_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '回调的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Callback description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/callback/xxx' },
  { value: 'timeout', label: '超时时间', placeholder: '30000 (毫秒)' },
  { value: 'retryCount', label: '重试次数', placeholder: '3' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const getCallbackListColumns = ({ handleView, handleEdit, handleDelete }) => [
  {
    title: '回调名称',
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
        <Button type="link" size="small" onClick={() => handleEdit(record)}>编辑</Button>
        <Button type="link" size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
      </Space>
    ),
  },
];
