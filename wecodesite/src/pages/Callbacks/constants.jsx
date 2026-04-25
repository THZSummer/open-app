import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, CALLBACK_CHANNEL_TYPE } from '../../utils/constants';

export const PAGE_SIZE_OPTIONS = [10, 20, 50];

export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

export const getCallbackColumns = ({ handleOpenDoc, handleEdit, handleCopyApprovalAddress, handleWithdraw, handleDeleteClick }) => [
  {
    title: '回调名称',
    dataIndex: ['permission', 'nameCn'],
    key: 'nameCn',
    render: (text, record) => (
      <div>
        <div>{text}</div>
        <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.permission?.scope}</span>
      </div>
    ),
  },
  {
    title: '分类',
    dataIndex: ['category', 'nameCn'],
    key: 'category',
  },
  {
    title: '所需权限',
    dataIndex: ['permission', 'nameCn'],
    key: 'permissionName',
  },
  {
    title: '通道类型',
    dataIndex: 'channelType',
    key: 'channelType',
    render: (type) => CALLBACK_CHANNEL_TYPE[type] || '-',
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    render: (status) => {
      const { text, color } = SUBSCRIPTION_STATUS[status] || { text: '未知', color: 'default' };
      return <Tag color={color}>{text}</Tag>;
    },
  },
  {
    title: '操作',
    key: 'action',
    render: (_, record) => (
      <div>
        <Button type="link" onClick={() => handleOpenDoc(record.callback?.docUrl || record.docUrl)}>查看文档</Button>
        {record.status === 1 && (
          <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
        )}
        {record.status === 0 && (
          <>
            <Button type="link" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
            <Button type="link" onClick={() => handleWithdraw(record)}>撤回审核</Button>
          </>
        )}
        {record.status !== 0 && (
          <Button type="link" danger onClick={() => handleDeleteClick(record.id)}>删除</Button>
        )}
      </div>
    ),
  },
];

export const getCallbackDrawerColumns = () => [
  {
    title: '回调名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    render: (text, record) => {
      const name = record.nameCn || record.name || '-';
      return (
        <div>
          <div>{name}</div>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.scope}</span>
        </div>
      );
    },
  },
  {
    title: '是否需要审核',
    dataIndex: 'needApproval',
    key: 'needApproval',
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
    render: () => null,
  },
];
