import React, { useState, useEffect } from 'react';
import {
  Button,
  Table,
  Tabs,
  Empty,
  Spin,
  Pagination,
  message,
} from 'antd';
const { TabPane } = Tabs;
import {
  CheckOutlined,
  CloseOutlined,
  EyeOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalList,
  fetchMyApprovals,
  fetchApprovalDetail,
  approveApplication,
  rejectApplication,
} from './thunk';
import { getApprovalColumns, getMyApprovalColumns, getAllApprovalColumns } from './constants';
import ApprovalFlowConfig from './ApprovalFlowConfig';
import ApprovalDetailModalWrapper from '../../../components/ApprovalDetailModal/ApprovalDetailModalWrapper';
import ApprovalOpinionModal from '../../../components/ApprovalOpinionModal/ApprovalOpinionModal';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import './ApprovalCenter.m.less';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { isInAdminWhitelist } from '../../../utils/common';

function ApprovalCenter() {
  const [loading, setLoading] = useState(false); 
  const [approvalList, setApprovalList] = useState([]);
  const [myApprovals, setMyApprovals] = useState([]);
  const [activeTab, setActiveTab] = useState('pending');
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentDetailId, setCurrentDetailId] = useState(null);
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);
  const [rejectModalVisible, setRejectModalVisible] = useState(false);
  const [rejectingId, setRejectingId] = useState(null);
  const [approveModalVisible, setApproveModalVisible] = useState(false);
  const [approvingId, setApprovingId] = useState(null);
  const [canViewFlowConfig, setCanViewFlowConfig] = useState(false);

  useEffect(() => {
    const hasPermission = isInAdminWhitelist();
    setCanViewFlowConfig(hasPermission);
  }, []);

  useEffect(() => {
    loadData(INIT_PAGECONFIG);
  }, [activeTab]);

  const loadData = async (params = {}) => {
    setLoading(true);
    const finalPage = 'curPage' in params ? params.curPage : pagination.curPage;
    const finalSize = 'pageSize' in params ? params.pageSize : pagination.pageSize;
    let result;

    if (activeTab === 'pending') {
      result = await fetchApprovalList({ status: 0, ...params });
      if (result && result.code === '200') {
        setApprovalList(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      } else {
        message.error(result?.message || '加载待审批列表失败');
      }
    } else if (activeTab === 'mine') {
      result = await fetchMyApprovals(params);
      if (result && result.code === '200') {
        setMyApprovals(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      } else {
        message.error(result?.message || '加载我发起的审批列表失败');
      }
    } else if (activeTab === 'all') {
      result = await fetchApprovalList(params);
      if (result && result.code === '200') {
        setApprovalList(result.data);
        setPagination(prev => ({ ...prev, total: result.page?.total || 0, curPage: finalPage, pageSize: finalSize }));
      } else {
        message.error(result?.message || '加载全部审批列表失败');
      }
    }

    setLoading(false);
  };

  const handlePageChange = (page, size) => {
    const params = {
      curPage: page,
      pageSize: size
    }
    loadData(params);
  };

  const handleApprove = (id) => {
    setApprovingId(id);
    setApproveModalVisible(true);
  };

  const handleConfirmApprove = async (id, comment) => {
    const res = await approveApplication(id, { comment });
    if (res && res.code === '200') {
      message.success('审批通过');
      setApproveModalVisible(false);
      loadData();
    } else {
      message.error(res?.messageZh || res?.message || '审批失败');
    }
  };

  const handleReject = (id) => {
    setRejectingId(id);
    setRejectModalVisible(true);
  };

  const handleConfirmReject = async (id, comment) => {
    const res = await rejectApplication(id, { comment });
    if (res && res.code === '200') {
      message.success('审批已拒绝');
      setRejectModalVisible(false);
      loadData();
    } else {
      message.error(res?.messageZh || res?.message || '审批失败');
    }
  };

  const handleViewDetail = async (record) => {
    setCurrentDetailId(record.id);
    setDetailVisible(true);
  };

  const columns = getApprovalColumns({
    handleViewDetail,
    handleApprove,
    handleReject,
  });

  const myColumns = getMyApprovalColumns({
    handleViewDetail,
  });

  const allColumns = getAllApprovalColumns({
    handleViewDetail,
  });

  const dataSource = activeTab === 'mine' ? myApprovals : approvalList;
  let cols = columns;
  if (activeTab === 'mine') {
    cols = myColumns;
  } else if (activeTab === 'all') {
    cols = allColumns;
  }

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <SimpleSidebar />
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="approval-center">
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">审批中心</h4>
              <span className="page-desc">审批权限申请，处理待办事项</span>
            </div>
          </div>

          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
          >
            <TabPane key="pending" tab="我的待审" />
            <TabPane key="mine" tab="我发起的" />
            {canViewFlowConfig && <TabPane key="all" tab="全部" />}
            {canViewFlowConfig && <TabPane key="flowConfig" tab="审批流程配置" />}
          </Tabs>

          {activeTab === 'flowConfig' ? (
            canViewFlowConfig ? <ApprovalFlowConfig /> : <Empty description="您没有权限访问审批流程配置" />
          ) : (
            <Spin spinning={loading}>
              {dataSource.length > 0 ? (
                <>
                  <div className="table-wrapper">
                    <Table
                      columns={cols}
                      dataSource={dataSource}
                      rowKey="id"
                      pagination={false}
                    />
                  </div>
                  <div style={{ marginTop: 16, textAlign: 'right' }}>
                    <Pagination
                      total={pagination.total}
                      current={pagination.curPage}
                      pageSize={pagination.pageSize}
                      pageSizeOptions={PAGE_SIZE_OPTIONS}
                      showSizeChanger
                      showQuickJumper
                      showTotal={(total) => `共 ${pagination.total} 条`}
                      onChange={handlePageChange}
                    />
                  </div>
                </>
              ) : (
                <Empty description="暂无数据" />
              )}
            </Spin>
          )}

          <ApprovalDetailModalWrapper
            visible={detailVisible}
            approvalId={currentDetailId}
            onClose={() => setDetailVisible(false)}
            fetchDetail={fetchApprovalDetail}
          />

          <ApprovalOpinionModal
            visible={approveModalVisible}
            title="审批通过"
            okText="确认通过"
            isRequired={false}
            approvalId={approvingId}
            onClose={() => setApproveModalVisible(false)}
            onConfirm={handleConfirmApprove}
          />

          <ApprovalOpinionModal
            visible={rejectModalVisible}
            title="驳回审批意见"
            okText="确认驳回"
            okButtonProps={{ danger: true }}
            isRequired={true}
            approvalId={rejectingId}
            onClose={() => setRejectModalVisible(false)}
            onConfirm={handleConfirmReject}
          />
        </div>
      </div>
    </div>
  );
}

export default ApprovalCenter;
