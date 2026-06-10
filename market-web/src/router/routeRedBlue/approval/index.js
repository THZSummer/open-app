import React, { useState, useEffect } from 'react';
import { message, Table, Pagination, Tabs, Modal } from 'antd';
import { fetchPendingList, fetchPublishedList, processApproval } from './thunk';
import {
  API_CONFIG, APPROVAL_ACTION, PAGE_SIZE_OPTIONS,
  getPendingColumns, getPublishedColumns
} from './constant';
import less from './index.module.less';

const { TabPane } = Tabs;
const { confirm } = Modal;

const Approval = () => {
  const [activeTab, setActiveTab] = useState('pending');
  const [dataSource, setDataSource] = useState([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ curPage: 1, pageSize: 10, total: 0 });
  const [currentLang, setCurrentLang] = useState(localStorage.getItem('lang') || 'zh');

  const fetchData = async (tab = activeTab, page = pagination) => {
    setLoading(true);
    try {
      const fetchFn = tab === 'pending' ? fetchPendingList : fetchPublishedList;
      const result = await fetchFn({ curPage: page.curPage, pageSize: page.pageSize });
      if (result && result.code === '200') {
        setDataSource(result.data || []);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0 }));
      } else {
        message.error(result?.messageZh || '获取数据失败');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [activeTab, pagination.curPage, pagination.pageSize]);

  const handleTabChange = (key) => {
    setActiveTab(key);
    setPagination({ curPage: 1, pageSize: 10, total: 0 });
    setDataSource([]);
  };

  const handlePageChange = (page, pageSize) => {
    setPagination(prev => ({ ...prev, curPage: page, pageSize }));
  };

  const handleView = (record) => {
    window.open('/app-detail/' + record.appId, '_blank');
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

  // Language switch
  const switchLang = (lang) => {
    setCurrentLang(lang);
    localStorage.setItem('lang', lang);
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
          <div className={less.langSwitch}>
            <span className={currentLang === 'zh' ? less.langActive : ''} onClick={() => switchLang('zh')}>中文</span>
            <span className={currentLang === 'en' ? less.langActive : ''} onClick={() => switchLang('en')}>EN</span>
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
            pageSizeOptions={['10', '20', '50']}
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
