import React from 'react';
import { Button } from 'antd';

export const getOwnerColumns = (handleRemoveOwner) => [
  { title: '用户ID', dataIndex: 'userId', key: 'userId' },
  { title: '用户名称', dataIndex: 'userName', key: 'userName' },
  {
    title: '操作',
    key: 'action',
    render: (_, record) => (
      <Button type="link" danger size="small" onClick={() => handleRemoveOwner(record.userId)}>
        移除
      </Button>
    ),
  },
];
