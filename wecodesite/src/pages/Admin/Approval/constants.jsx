import React from 'react';
import { Tag, Button, Space, Popconfirm } from 'antd';
import { CheckOutlined, CloseOutlined, EyeOutlined } from '@ant-design/icons';

export const STATUS_MAP = {
  0: { text: '待审', color: 'orange' },
  1: { text: '已通过', color: 'green' },
  2: { text: '已拒绝', color: 'red' },
  3: { text: '已撤销', color: 'default' },
};

export const LEVEL_MAP = {
  'resource': { text: '资源审批', color: 'blue' },
  'scene': { text: '场景审批', color: 'orange' },
  'global': { text: '全局审批', color: 'green' },
};

export const NODE_STATUS_MAP = {
  null: { text: '待审', color: 'default' },
  0: { text: '待审批', color: 'default' },
  1: { text: '已同意', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
};

export const APPROVAL_TYPE_MAP = {
  'resource_register': '资源注册',
  'permission_apply': '权限申请',
};

export const APPROVAL_TABS = [
  { key: 'pending', label: '我的待审' },
  { key: 'mine', label: '我发起的' },
  { key: 'all', label: '全部' },
];

export const getApprovalColumns = ({ handleViewDetail, handleApprove, handleReject }) => [
  {
    title: '申请编号',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
  },
  {
    title: '业务类型',
    dataIndex: 'businessType',
    key: 'businessType',
  },
  {
    title: '业务名称',
    dataIndex: 'businessName',
    key: 'businessName',
  },
  {
    title: '业务ID',
    dataIndex: 'businessId',
    key: 'businessId',
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
        <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
        {record.status === 0 && (
          <>
            <Popconfirm title="确定通过该申请吗？" onConfirm={() => handleApprove(record.id)} okText="确定" cancelText="取消">
              <Button type="link" size="small" icon={<CheckOutlined />}>通过</Button>
            </Popconfirm>
            <Popconfirm title="确定拒绝该申请吗？" onConfirm={() => handleReject(record.id)} okText="确定" cancelText="取消">
              <Button type="link" size="small" danger icon={<CloseOutlined />}>拒绝</Button>
            </Popconfirm>
          </>
        )}
      </Space>
    ),
  },
];

export const getMyApprovalColumns = ({ handleViewDetail }) => [
  {
    title: '申请编号',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '业务类型',
    dataIndex: 'businessType',
    key: 'businessType',
  },
  {
    title: '业务名称',
    dataIndex: 'businessName',
    key: 'businessName',
  },
  {
    title: '业务ID',
    dataIndex: 'businessId',
    key: 'businessId',
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
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
    ),
  },
];
