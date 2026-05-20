import React from 'react';
import { Button } from 'antd';
import { CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import { renderStatus } from '../../utils/commonTableConfigs';

/**
 * 事件列表表格列配置
 *
 * @param {Function} handleOpenDoc - 打开文档
 * @param {Function} handleEdit - 修改条目
 * @param {Function} handleCopyApprovalAddress - 复制审批地址
 * @param {Function} handleWithdraw - 取消订阅
 * @param {Function} handleDelete - 移除条目
 */
export const getEventColumns = ({ handleOpenDoc, handleEdit, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
  {
    title: '权限名称',
    key: 'nameCn',
    width: 180,
    dataIndex: ['permission', 'nameCn'],
  },
  {
    title: 'Scope标识',
    key: 'scope',
    width: 200,
    dataIndex: ['permission', 'scope'],
    ellipsis: true,
    render: (code) => <code>{code}</code>,
  },
  {
    title: '事件Topic',
    key: 'topic',
    width: 200,
    dataIndex: ['event', 'topic'],
    ellipsis: true,
    render: (topic) => <code>{topic}</code>,
  },
  {
    title: '订阅方式',
    key: 'channelType',
    width: 100,
    dataIndex: 'channelType',
    render: (type) => CHANNEL_TYPE[type] || '-',
  },
  {
    title: '认证方式',
    key: 'authType',
    width: 100,
    dataIndex: 'authType',
    render: (type) => AUTH_TYPE[type] || '-',
  },
  {
    title: '状态',
    width: 100,
    dataIndex: 'status',
    key: 'status',
    render: renderStatus,
  },
  {
    title: '操作',
    width: 300,
    key: 'action',
    fixed: 'right',
    render: (_, record) => (
      <div>
        <Button type="link" onClick={() => handleOpenDoc(record.event?.docUrl || record.docUrl)}>查看文档</Button>
        {record.status === 1 && (
          <Button type="link" onClick={() => handleEdit(record)}>编辑</Button>
        )}
        {record.status === 0 && (
          <>
            <Button type="link" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
            <Button type="link" onClick={() => handleWithdraw(record.id)}>撤回审核</Button>
          </>
        )}
        {record.status !== 0 && (
          <Button type="link" danger onClick={() => handleDelete(record.id)}>删除</Button>
        )}
      </div>
    ),
  },
];
