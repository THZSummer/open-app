/**
 * 能力目录管理列表页面
 *
 * 提供能力目录的分页列表展示功能。
 * 对应 FR-001：平台管理员在 market-web 查看所有 ability 类型列表。
 */
import React, { useState, useEffect, useCallback } from 'react';
import { Table, Pagination, Modal, message } from 'antd';
import { PlusOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import { getAbilityList, deleteAbility } from './thunk';
import CreateForm from './components/CreateForm';
import EditForm from './components/EditForm';
import { PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import less from './index.module.less';

/** 默认分页 */
const DEFAULT_PAGE_SIZE = 10;

/**
 * 能力目录管理列表页面
 */
const AbilityAdminList = () => {
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState({
    curPage: 1,
    pageSize: DEFAULT_PAGE_SIZE,
    total: 0,
  });
  const [queryParams, setQueryParams] = useState({
    curPage: 1,
    pageSize: DEFAULT_PAGE_SIZE,
  });
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState(null);

  /**
   * 获取列表数据
   */
  const fetchData = useCallback(async () => {
    setLoading(true);
    const result = await getAbilityList(queryParams);
    if (result && result.code === '200') {
      setDataSource(result.data || []);
      if (result.page) {
        setPagination({
          curPage: Number(result.page.curPage) || 1,
          pageSize: Number(result.page.pageSize) || DEFAULT_PAGE_SIZE,
          total: Number(result.page.total) || 0,
        });
      }
    } else {
      message.error(result?.messageZh || result?.messageEn || '获取能力列表失败');
      setDataSource([]);
    }
    setLoading(false);
  }, [queryParams]);

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queryParams]);

  /**
   * 分页改变
   */
  const handlePageChange = (page, pageSize) => {
    setQueryParams({
      curPage: page,
      pageSize: pageSize || pagination.pageSize,
    });
  };

  /**
   * 删除确认并执行
   */
  const handleDelete = (record) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除能力「${record.nameCn || record.abilityType}」吗？此操作不可撤销。`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const result = await deleteAbility(record.id);
        if (result && result.code === '200') {
          message.success('删除成功');
          fetchData();
        } else if (result && result.code === '400' && result.data?.subscribeCount) {
          message.error(`该能力已被 ${result.data.subscribeCount} 个应用订阅，无法删除`);
        } else {
          message.error(result?.messageZh || result?.messageEn || '删除失败');
        }
      },
    });
  };

  /** 表格列定义 */
  const columns = [
    {
      title: '排序',
      dataIndex: 'orderNum',
      key: 'orderNum',
      width: 60,
      render: (text) => text ?? '-',
    },
    {
      title: '能力名称',
      key: 'capabilityName',
      width: 280,
      render: (_, record) => (
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {record.iconUrl ? (
            <img
              src={record.iconUrl}
              alt=""
              style={{ width: 32, height: 32, borderRadius: 6, objectFit: 'cover', flexShrink: 0 }}
            />
          ) : (
            <div style={{
              width: 32, height: 32, borderRadius: 6, background: '#e6f7ff',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              color: '#1890ff', fontSize: 16, flexShrink: 0,
            }}>
              ⚡
            </div>
          )}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 2, overflow: 'hidden' }}>
            <span style={{ fontWeight: 500, lineHeight: '20px' }}>
              {record.nameCn || '-'}
            </span>
            <span style={{
              fontSize: 12, color: '#999', lineHeight: '18px',
              maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }} title={record.descCn}>
              {record.descCn || '-'}
            </span>
          </div>
        </div>
      ),
    },
    {
      title: '访问地址',
      dataIndex: 'entryUrl',
      key: 'entryUrl',
      width: 200,
      ellipsis: true,
      render: (text) => text ? <a href={text} target="_blank" rel="noopener noreferrer">{text}</a> : '-',
    },
    {
      title: '示意图',
      dataIndex: 'exampleDiagramUrl',
      key: 'exampleDiagramUrl',
      width: 100,
      render: (url) => url
        ? <img src={url} alt="示意图" style={{ width: 65, height: 36, borderRadius: 4, objectFit: 'cover', border: '1px solid #e8e8e8' }} />
        : <div style={{ width: 65, height: 36, borderRadius: 4, background: '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#ccc', fontSize: 12, border: '1px dashed #d9d9d9' }}>无</div>,
    },
    {
      title: '更新时间',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: 160,
      render: (text) => text || '-',
    },
    {
      title: '操作账号',
      dataIndex: 'updateBy',
      key: 'updateBy',
      width: 120,
      ellipsis: true,
      render: (text) => text || '-',
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <a style={{ cursor: 'pointer' }} onClick={() => setEditingRecord(record)}>编辑</a>
          <a style={{ color: '#ff4d4f', cursor: 'pointer' }} onClick={() => handleDelete(record)}>删除</a>
        </div>
      ),
    },
  ];

  return (
    <div className={less.container}>
      <div className={less.page}>
        {/* 页面头部 */}
        <div className={less.pageHead}>
          <div className={less.pageHeadLeft}>
            <span className={less.pageHeadTitle}>能力目录管理</span>
          </div>
          <div>
            <button
              className={`${less.btn} ${less.btnPrimary}`}
              onClick={() => setCreateModalOpen(true)}
            >
              <PlusOutlined />
              添加能力
            </button>
          </div>
        </div>

        {/* 表格 */}
        <div className={less.tableWrap}>
          <Table
            columns={columns}
            dataSource={dataSource}
            rowKey="abilityType"
            loading={loading}
            pagination={false}
            scroll={{ x: 1200 }}
            size="middle"
          />
        </div>

        {/* 分页 */}
        <div className={less.paginationWrapper}>
          <Pagination
            total={pagination.total}
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>

      {/* 新增能力弹窗 */}
      <CreateForm
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={() => {
          fetchData();
        }}
      />

      {/* 编辑能力弹窗 */}
      <EditForm
        open={!!editingRecord}
        record={editingRecord}
        onClose={() => setEditingRecord(null)}
        onSuccess={() => {
          setEditingRecord(null);
          fetchData();
        }}
      />
    </div>
  );
};

export default AbilityAdminList;
