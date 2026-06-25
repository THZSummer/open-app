import React, { useState, useEffect } from 'react';
import { message, Table, Pagination, Tabs, Modal } from 'antd';
import { fetchPendingList, fetchPublishedList, processApproval } from './thunk';
import {
  API_CONFIG, APPROVAL_ACTION, getPendingColumns, getPublishedColumns
} from './constant';
import { DEFAULT_PAGINATION, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import { openWindow } from '../../../utils/common';
import less from './index.module.less';

const { TabPane } = Tabs;
const { confirm } = Modal;

// const Contextroot = processApproval.env.NODE_ENV === 'development' ? '' : '/appstore-market-admin';
const Contextroot = '';

const Approval = () => {
  const [activeTab, setActiveTab] = useState('pending');
  const [dataSource, setDataSource] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const [currentLang, setCurrentLang] = useState(localStorage.getItem('lang') || 'zh');

  const fetchData = async (tab = activeTab, page = pagination) => {
    setLoading(true);
    const fetchFn = tab === 'pending' ? fetchPendingList : fetchPublishedList;
    const result = await fetchFn({ pageNum: page.pageNum, pageSize: page.pageSize });
    if (result && result.code === '200') {
      setDataSource(result.data || []);
      setPagination(prev => ({ ...prev, pageNum: result.page?.curPage || 1, total: result.page?.total || 0 }));
    } else {
      message.error(result?.messageZh || '获取数据失败');
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, [activeTab, pagination.pageNum, pagination.pageSize]);

  const handleTabChange = (key) => {
    setActiveTab(key);
    setPagination(DEFAULT_PAGINATION);
    setDataSource([]);
  };

  const handlePageChange = (page, pageSize) => {
    setPagination(prev => ({ ...prev, pageNum: page, pageSize }));
  };

  const handleView = (record) => {
    // openWindow(`#${Contextroot}/approveDetail?appId=${record.appId}`);
    openWindow(`/market-web/approveDetail?appId=${record.appId}`);
  };

  const handleApprove = (record) => {
    confirm({
      title: '确认审批通过',
      content: `应用：${currentLang === 'en' ? record.appNameEn : record.appNameCn}，版本：${record.versionNo}，申请账号：${record.applicantId}`,
      okText: '确认通过',
      cancelText: '取消',
      onOk: async () => {
        const result = await processApproval({ id: String(record.id), action: APPROVAL_ACTION.APPROVE });
        if (result && result.code === '200') {
          message.success('审批通过');
          fetchData();
        } else {
          message.error(result?.messageZh || '操作失败');
        }
      },
    });
  };

  const handleReject = (record) => {
    confirm({
      title: '确认审批拒绝',
      content: `应用：${currentLang === 'en' ? record.appNameEn : record.appNameCn}，版本：${record.versionNo}，申请账号：${record.applicantId}`,
      okText: '确认拒绝',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const result = await processApproval({ id: String(record.id), action: APPROVAL_ACTION.REJECT });
        if (result && result.code === '200') {
          message.success('已拒绝');
          fetchData();
        } else {
          message.error(result?.messageZh || '操作失败');
        }
      },
    });
  };

  // App name renderer
  const renderAppName = (text, record) => {
    return currentLang === 'en' ? record.appNameEn : record.appNameCn;
  };

  // Pending tab action renderer
  const renderPendingAction = (_, record) => (
    <div className={less.actionButtons}>
      <span className={less.actionButton} onClick={() => handleView(record)}>查看</span>
      <span className={less.actionButtonSuccess} onClick={() => handleApprove(record)}>同意</span>
      <span className={less.actionButtonDanger} onClick={() => handleReject(record)}>拒绝</span>
    </div>
  );

  // Published tab action renderer
  const renderPublishedAction = (_, record) => (
    <div className={less.actionButtons}>
      <span className={less.actionButton} onClick={() => handleView(record)}>查看</span>
    </div>
  );

  const pendingColumns = getPendingColumns({ renderAppName, renderAction: renderPendingAction });
  const publishedColumns = getPublishedColumns({ renderAppName, renderAction: renderPublishedAction });

  return (
    <div className={less.container}>
      <div className={less.page}>
        <div className={less.pageHead}>
          <div className={less.pageHeadLeft}>
            <span className={less.pageHeadTitle}>审批管理</span>
          </div>
        </div>
        <Tabs activeKey={activeTab} onChange={handleTabChange}>
          <TabPane tab="待审批应用" key="pending">
            <Table columns={pendingColumns} dataSource={dataSource} rowKey="id"
              loading={loading} pagination={false} scroll={{ x: '100%' }} />
          </TabPane>
          <TabPane tab="已上架应用" key="published">
            <Table columns={publishedColumns} dataSource={dataSource} rowKey="id"
              loading={loading} pagination={false} scroll={{ x: '100%' }} />
          </TabPane>
        </Tabs>
        <div className={less.paginationWrapper}>
          <Pagination
            total={pagination.total}
            current={pagination.curPage}
            pageSize={pagination.pageSize}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showSizeChanger
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>
    </div>
  );
};

export default Approval;
