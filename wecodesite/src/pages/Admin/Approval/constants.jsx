import React from 'react';
import { Tag, Button, Space, Popconfirm } from 'antd';
import { CheckOutlined, CloseOutlined, EyeOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';

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

export const BUSINESS_COLUMNS = [
  {
    title: '业务类型',
    dataIndex: 'businessType',
    key: 'businessType',
    width: 120,
  },
  {
    title: '业务名称',
    dataIndex: 'businessName',
    key: 'businessName',
    width: 150,
    ellipsis: true,
  },
  {
    title: '业务ID',
    dataIndex: 'businessId',
    key: 'businessId',
    width: 150,
    ellipsis: true,
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
];

export const NODE_STATUS_MAP = {
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
    width: 180,
    ellipsis: true,
  },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
    width: 100,
  },
  ...BUSINESS_COLUMNS,
  {
    title: '操作',
    key: 'action',
    width: 240,
    fixed: 'right',
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
    width: 180,
    ellipsis: true,
  },
  ...BUSINESS_COLUMNS,
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    render: (_, record) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
    ),
  },
];

export const getAllApprovalColumns = ({ handleViewDetail }) => [
  {
    title: '申请编号',
    dataIndex: 'id',
    key: 'id',
    width: 180,
    ellipsis: true,
  },
  {
    title: '申请人',
    dataIndex: 'applicantName',
    key: 'applicantName',
    width: 100,
  },
  ...BUSINESS_COLUMNS,
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    render: (_, record) => (
      <Button type="link" size="small" icon={<EyeOutlined />} onClick={() => handleViewDetail(record)}>详情</Button>
    ),
  },
];

export const getApprovalFlowColumns = ({ handleEdit, handleDelete }) => [
  {
    title: '流程ID',
    dataIndex: 'id',
    key: 'id',
    width: 80,
  },
  {
    title: '流程名称（中文）',
    dataIndex: 'nameCn',
    key: 'nameCn',
    width: 200,
  },
  {
    title: '流程名称（英文）',
    dataIndex: 'nameEn',
    key: 'nameEn',
    width: 200,
  },
  {
    title: '审批类型',
    dataIndex: 'code',
    key: 'approvalType',
    width: 120,
    render: (code) => (
      <Tag color={code === 'global' ? 'green' : 'blue'}>
        {code === 'global' ? '全局审批' : '场景审批'}
      </Tag>
    ),
  },
  {
    title: '流程代码',
    dataIndex: 'code',
    key: 'code',
    width: 180,
    render: (code) => (
      <Tag color="blue">{code}</Tag>
    ),
  },
  {
    title: '审批节点数',
    dataIndex: 'nodes',
    key: 'nodes',
    width: 100,
    render: (nodes) => nodes?.length || 0,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80,
    render: (status) => (
      <Tag color={status === 1 ? 'green' : 'default'}>
        {status === 1 ? '启用' : '禁用'}
      </Tag>
    ),
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    render: (_, record) => (
      <Space>
        <Button
          type="link"
          size="small"
          icon={<EditOutlined />}
          onClick={() => handleEdit(record)}
        >
          编辑
        </Button>
        <Button
          type="link"
          size="small"
          danger
          icon={<DeleteOutlined />}
          onClick={() => handleDelete(record)}
        >
          删除
        </Button>
      </Space>
    ),
  },
];
