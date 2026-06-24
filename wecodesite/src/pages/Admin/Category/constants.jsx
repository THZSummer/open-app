import React from 'react';
import { Button } from 'antd';

/**
 * 分类删除二次确认弹窗配置
 */
export const CATEGORY_DELETE_SECOND_MODAL_INFO = {
  action: 'delete',
  getConfirmText: ({ objectName }) => `确认要删除这个分类：${objectName}吗？`,
  impactText: '操作影响：删除后，该分类及其所有子分类将被删除，分类下关联资源可能受到影响。',
};

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
