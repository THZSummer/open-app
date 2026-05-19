import React from 'react';
import { Button } from 'antd';
import { ACTION_CONFIG } from '../../../utils/constants';

export const getOwnerColumns = (handleRemoveOwner) => [
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 150 },
  { title: '用户名称', dataIndex: 'userName', key: 'userName', width: 200 },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render: (_, record) => (
      <Button type="link" danger size="small" onClick={() => handleRemoveOwner(record.userId)}>
        移除
      </Button>
    ),
  },
];

export const secondModalInfo = {
  ...ACTION_CONFIG.delete,
  title: '删除分类',
  content: '此操作将永久删除该分类及其所有子分类，无法恢复！',
}
