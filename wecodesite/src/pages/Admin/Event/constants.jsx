import React from 'react';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

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
      dataIndex: 'categoryName',
      key: 'categoryName',
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
