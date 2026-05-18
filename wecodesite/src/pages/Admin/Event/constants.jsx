import React from 'react';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

/**
 * 事件列表表格配置
 * @param {Object} callbacks - 回调函数集合
 */
export const getEventListColumns = (callbacks) => {
  const { handleView, handleEdit, handleDelete } = callbacks;
  const baseColumn = adminTableBaseColumn({
    handleView,
    handleEdit,
    handleDelete
  });

  return [
    {
      title: '事件名称',
      dataIndex: 'nameCn',
      key: 'nameCn',
      width: 180,
      render: (nameCn, record) => (
        <div>
          <div>{nameCn}</div>
          <div style={{ fontSize: 12, color: '#999' }}>{record.nameEn}</div>
        </div>
      ),
    },
    {
      title: 'Topic',
      dataIndex: 'topic',
      key: 'topic',
      width: 200,
      ellipsis: true,
      render: (text) => <code>{text}</code>,
    },
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120,
    },
    ...baseColumn,
  ];
};
