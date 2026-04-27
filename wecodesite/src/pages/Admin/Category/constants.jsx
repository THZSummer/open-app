import React from 'react';
import { Button } from 'antd';

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
