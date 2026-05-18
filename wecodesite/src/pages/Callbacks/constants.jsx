import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, CHANNEL_TYPE } from '../../utils/constants';

// 分页选项
export const PAGE_SIZE = [10, 20, 50];

// 是否需要审核选项
export const NEED_REVIEW_OPTIONS = [
  { value: 'all', label: '全部' },
  { value: 'true', label: '需要审核' },
  { value: 'false', label: '无需审核' },
];

/**
 * 回调列表表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackColumns = (callbacks) => {
  const { onDocLink, onEditItem, onCopyAddr, onWithdraw, onDeleteItem } = callbacks;

  return [
    {
      title: '回调名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      width: 200,
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
      width: 120,
    },
    {
      title: '所需权限',
      dataIndex: ['permission', 'nameCn'],
      key: 'permissionName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '通道类型',
      dataIndex: 'channelType',
      key: 'channelType',
      width: 100,
      render: (type) => CHANNEL_TYPE[type] || '-',
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
      width: 300,
      fixed: 'right',
      render: (_, record) => (
        <div>
          <Button type="link" onClick={() => onDocLink(record.callback?.docUrl || record.docUrl)}>查看文档</Button>
          {record.status === 1 && (
            <Button type="link" onClick={() => onEditItem(record)}>编辑</Button>
          )}
          {record.status === 0 && (
            <>
              <Button type="link" onClick={() => onCopyAddr(record)}>复制审批地址</Button>
              <Button type="link" onClick={() => onWithdraw(record)}>撤回审核</Button>
            </>
          )}
          {record.status !== 0 && (
            <Button type="link" danger onClick={() => onDeleteItem(record.id)}>删除</Button>
          )}
        </div>
      ),
    },
  ];
};

/**
 * 回调选择器表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackDrawerColumns = ({ onDocLink }) => [
  {
    title: '回调名称',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 200,
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
    width: 100,
    render: (_, record) => (
      <Button type="link" size="small" onClick={() => onDocLink(record.docUrl)}>查看文档</Button>
    ),
  },
];
