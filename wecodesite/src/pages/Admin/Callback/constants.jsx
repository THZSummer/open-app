import React from 'react';
import { adminTableBaseColumn } from '../../../utils/commonTableConfigs';

/**
 * 回调列表列配置
 *
 * @param {Object} props
 * @param {Function} props.onView - 查看操作
 * @param {Function} props.onEdit - 编辑操作
 * @param {Function} props.onDelete - 删除操作
 */
export const getCallbackListColumns = ({ onView, onEdit, onDelete }) => {
  const base = adminTableBaseColumn({
    handleView: onView,
    handleEdit: onEdit,
    handleDelete: onDelete
  });

  return [
    {
      title: '分类',
      dataIndex: 'categoryName',
      key: 'categoryName',
      width: 120,
    },
    {
      title: '回调名称',
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
    ...base,
  ];
};
