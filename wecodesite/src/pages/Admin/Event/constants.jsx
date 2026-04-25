import React from 'react';
import { Tag, Button, Space } from 'antd';

export const STATUS_MAP = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审', color: 'orange' },
  2: { text: '已发布', color: 'green' },
  3: { text: '已下线', color: 'red' },
};

export const EVENT_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '事件的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Event description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/event/xxx' },
  { value: 'dataSchema', label: '数据结构', placeholder: 'JSON Schema 或数据格式说明' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

export const getEventListColumns = ({ handleView, handleEdit, handleDelete }) => [
  {
    title: '事件名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
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
  },
  {
    title: 'Topic',
    dataIndex: 'topic',
    key: 'topic',
    render: (text) => <code>{text}</code>,
  },
  {
    title: 'Scope',
    dataIndex: 'permission',
    key: 'scope',
    render: (permission) => {
      const scope = permission?.scope || '-';
      return <Tag color="cyan">{scope}</Tag>;
    },
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status) => {
      const { text, color } = STATUS_MAP[status] || STATUS_MAP[0];
      return <Tag color={color}>{text}</Tag>;
    },
  },
  {
    title: '操作',
    key: 'action',
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
