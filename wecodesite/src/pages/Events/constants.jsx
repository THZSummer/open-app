import React from 'react';
import { Tag, Button } from 'antd';
import { SUBSCRIPTION_STATUS, CHANNEL_TYPE } from '../../utils/constants';
import {
  renderStatus,
  createEventDrawerColumns
} from '../../utils/commonTableConfigs';

/**
 * 事件选择器表格列配置
 *
 * @param {Object} props - 组件属性
 * @param {Function} props.onViewDoc - 查看文档回调
 */
export const getEventDrawerColumns = ({ onViewDoc }) => createEventDrawerColumns().map(col => {
  if (col.key === 'action') {
    return {
      ...col,
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => onViewDoc(record.docUrl)}>查看文档</Button>
      ),
    };
  }
  return col;
});

/**
 * 事件列表表格列配置
 *
 * @param {Object} handlers - 处理器集合
 * @param {Function} handlers.openDoc - 打开文档
 * @param {Function} handlers.modifyItem - 修改条目
 * @param {Function} handlers.copyApproval - 复制审批地址
 * @param {Function} handlers.cancelSubscription - 取消订阅
 * @param {Function} handlers.removeItem - 移除条目
 */
export const getEventColumns = (handlers) => {
  const {
    openDoc,
    modifyItem,
    copyApproval,
    cancelSubscription,
    removeItem
  } = handlers;

  return [
    {
      title: '事件名称',
      dataIndex: ['permission', 'nameCn'],
      key: 'nameCn',
      width: 200,
      render: (text, record) => (
        <div>
          <div>{text}</div>
          <span style={{ fontSize: 12, color: '#8c8c8c' }}>{record.event?.topic}</span>
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
          <Button type="link" onClick={() => openDoc(record.event?.docUrl || record.docUrl)}>查看文档</Button>
          {record.status === 1 && (
            <Button type="link" onClick={() => modifyItem(record)}>编辑</Button>
          )}
          {record.status === 0 && (
            <>
              <Button type="link" onClick={() => copyApproval(record)}>复制审批地址</Button>
              <Button type="link" onClick={() => cancelSubscription(record)}>撤回审核</Button>
            </>
          )}
          {record.status !== 0 && (
            <Button type="link" danger onClick={() => removeItem(record.id)}>删除</Button>
          )}
        </div>
      ),
    },
  ];
};
