import React from 'react';
import { Tag, Button } from 'antd';
import { CHANNEL_TYPE } from '../../utils/constants';
import { 
  renderStatus, 
  createCallbackDrawerColumns 
} from '../../utils/commonTableConfigs';

/**
 * 回调列表表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackColumns = ({ hanldeOpenDoc, handleEdit, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
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
    title: '订阅方式',
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
    render: renderStatus,
  },
  {
    title: '操作',
    key: 'action',
    width: 300,
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

/**
 * 回调选择器表格列配置
 * @param {Object} callbacks - 事件回调对象
 */
export const getCallbackDrawerColumns = ({ handleOpenDoc }) => createCallbackDrawerColumns().map(col => {
  if (col.key === 'action') {
    return {
      ...col,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => handleOpenDoc(record.resource.docUrl)}>查看文档</Button>
      ),
    };
  }
  return col;
});