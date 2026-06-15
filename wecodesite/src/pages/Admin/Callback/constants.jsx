import React from 'react';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

/**
 * 回调删除二次确认弹窗配置
 */
export const ADMIN_CALLBACK_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个回调：${objectName}吗？`,
  impactText: '操作影响：删除后，该回调将不可再被配置或调用，关联回调配置可能失效。',
};

export const getCallbackListColumns = ({ handleView, handleEdit, handleDelete }) => {
  const baseColumn = adminTableBaseColumn({ handleView, handleEdit, handleDelete });
  return [
    {
      title: '回调名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 180,
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
      width: 120,
    },
    ...baseColumn,
  ]
}