import React from 'react';
import { Tag, Button } from 'antd';
import { CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import { renderStatus } from '../../utils/commonTableConfigs';

/**
 * 回调列表表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackColumns = ({ hanldeOpenDoc, handleEdit, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
  {
    title: '权限名称',
    key: 'nameCn',
    dataIndex: ['permission', 'nameCn'],
    width: 180,
  },
  {
    title: 'ScopeId',
    key: 'scope',
    dataIndex: ['permission', 'scope'],
    width: 200,
    ellipsis: true,
    render: (code) => <code>{code}</code>,
  },
  {
    title: '订阅方式',
    key: 'channelType',
    dataIndex: 'channelType',
    width: 100,
    render: (type) => CHANNEL_TYPE[type] || '-',
  },
  {
    title: '认证方式',
    key: 'authType',
    dataIndex: 'authType',
    width: 100,
    render: (type) => AUTH_TYPE[type] || '-',
  },
  {
    title: '状态',
    key: 'status',
    dataIndex: 'status',
    width: 100,
    render: renderStatus,
  },
  {
    title: '操作',
    key: 'action',
    width: 200,
    fixed: 'right',
    render: (_, record) => (
      <div>
        <Button type="link" onClick={() => hanldeOpenDoc(record.callback?.docUrl || record.docUrl)}>查看文档</Button>
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