import React from 'react';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

/**
 * 事件删除二次确认弹窗配置
 */
export const ADMIN_EVENT_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个事件：${objectName}吗？`,
  impactText: '操作影响：删除后，该事件将不可再被订阅，已配置的事件订阅可能无法继续使用。',
};

export const getEventListColumns = ({ handleView, handleEdit, handleDelete }) => {
  const baseColumn = adminTableBaseColumn({ handleView, handleEdit, handleDelete });
  return [
    {
      title: '事件名称',
      key: 'nameCn',
      dataIndex: 'nameCn',
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
      key: 'categoryName',
      dataIndex: 'categoryName',
      width: 120,
    },
    {
      title: 'Topic',
      dataIndex: 'topic',
      key: 'topic',
      width: 200,
      ellipsis: true,
      render: (text) => <code>{text}</code>,
    },
    ...baseColumn,
  ]
}
