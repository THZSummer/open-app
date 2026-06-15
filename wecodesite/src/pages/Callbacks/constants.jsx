import React from 'react';
import { Tag, Button } from 'antd';
import { CHANNEL_TYPE, AUTH_TYPE } from '../../utils/constants';
import { renderStatus } from '../../utils/commonTableConfigs';

/**
 * 回调订阅删除二次确认弹窗配置
 */
export const CALLBACK_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个回调订阅：${objectName}吗？`,
  impactText: '操作影响：删除后，该回调订阅关系将被移除，应用将无法继续使用对应回调能力。',
};

/**
 * 回调订阅撤回二次确认弹窗配置
 */
export const CALLBACK_WITHDRAW_SECOND_MODAL_INFO = {
  action: 'withdraw',
  getConfirmText: ({ objectName }) => `确认要撤回这个回调：${objectName}申请吗？`,
  impactText: '操作影响：撤回后，该回调权限申请将终止审批流程，如需使用，需要先删除后重新订阅。',
};

/**
 * 回调列表表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackColumns = ({ handleOpenDoc, handleEdit, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
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
          <Button type="link" danger onClick={() => handleDelete(record)}>删除</Button>
        )}
      </div>
    ),
  },
];