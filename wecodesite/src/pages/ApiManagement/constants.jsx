import React from 'react';
import { Button } from 'antd';
import { AUTH_TYPE } from '../../utils/constants';
import { renderStatus } from '../../utils/commonTableConfigs';

/**
 * API订阅管理Tab配置键
 */
export const TAB_CONFIG_SEARCH_KEY = 'CEC.Open/Api.Drawer.TabsList';

/**
 * 生成API管理表格列配置
 *
 * @param {Object} handlers - 操作处理器
 * @param {Function} onViewDoc - 查看文档
 * @param {Function} onCopyUrl - 复制审批地址
 * @param {Function} onRevoke - 撤回申请
 * @param {Function} onRemove - 删除订阅
 */
export const getApiManagementColumns = ({ handleOpenDoc, handleCopyApprovalAddress, handleWithdraw, handleDelete }) => [
  {
    title: '权限名称',
    dataIndex: ['permission', 'nameCn'],
    key: 'nameCn',
    width: 180,
  },
  {
    title: 'Scope标识',
    dataIndex: ['permission', 'scope'],
    key: 'scope',
    width: 200,
    ellipsis: true,
    render: (code) => <code>{code}</code>,
  },
  {
    title: '认证方式',
    dataIndex: ['api', 'authType'],
    key: 'authType',
    width: 100,
    render: (type) => AUTH_TYPE[type] || '-',
  },
  {
    title: '分类',
    dataIndex: ['category', 'nameCn'],
    key: 'category',
    width: 120,
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
    width: 280,
    fixed: 'right',
    render: (_, record) => (
      <div style={{ display: 'flex', gap: 8 }}>
        <Button type="link" size="small" onClick={() => handleOpenDoc(record.api?.docUrl)}>查看文档</Button>
        {record.status === 0 && (
          <>
            <Button type="link" size="small" onClick={() => handleCopyApprovalAddress(record)}>复制审批地址</Button>
            <Button type="link" size="small" onClick={() => handleWithdraw(record.id)}>撤回审核</Button>
          </>
        )}
        {record.status !== 0 && (
          <Button type="link" size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
        )}
      </div>
    ),
  },
];
