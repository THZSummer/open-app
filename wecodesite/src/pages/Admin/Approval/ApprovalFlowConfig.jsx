import React, { useState, useEffect } from 'react';
import {
  Card,
  Button,
  Table,
  Space,
  Empty,
  Spin,
  Input,
  message,
  Pagination,
} from 'antd';
import {
  PlusOutlined,
} from '@ant-design/icons';
import {
  fetchApprovalFlowList,
  fetchApprovalFlowDetail,
  createApprovalFlow,
  updateApprovalFlow,
  deleteApprovalFlow,
} from './thunk';
import './ApprovalCenter.m.less';
import { getApprovalFlowColumns } from './constants';
import ApprovalFlowFormModal from '../../../components/ApprovalFlowFormModal/ApprovalFlowFormModal';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';

function ApprovalFlowConfig() {
  const [loading, setLoading] = useState(false);
  const [flowList, setFlowList] = useState([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingFlow, setEditingFlow] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteFlowId, setDeleteFlowId] = useState(null);
  const [deleteFlowCode, setDeleteFlowCode] = useState('');

  useEffect(() => {
    loadFlowList();
  }, []);

  const loadFlowList = async (page = currentPage, size = pageSize) => {
    setLoading(true);
    const result = await fetchApprovalFlowList({ 
      keyword,
      curPage: page,
      pageSize: size 
    });
    if (result && result.code === '200') {
      setFlowList(result.data);
      setTotal(result.page?.total || 0);
    } else {
      message.error(result?.message || '加载流程列表失败');
    }
    setLoading(false);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    loadFlowList(1, pageSize);
  };

  const handlePageChange = (page, size) => {
    setCurrentPage(page);
    setPageSize(size);
    loadFlowList(page, size);
  };

  const handleCreate = () => {
    setEditingFlow(null);
    setModalVisible(true);
  };

  const handleEdit = async (record) => {
    setLoading(true);
    const result = await fetchApprovalFlowDetail(record.id);
    setLoading(false);
    
    if (result.code === '200' && result.data) {
      setEditingFlow(result.data);
      setModalVisible(true);
    } else {
      message.error('获取流程详情失败');
    }
  };

  const handleDelete = (record) => {
    setDeleteFlowId(record.id);
    setDeleteFlowCode(record.code);
    setDeleteModalVisible(true);
  };

  const handleConfirmDelete = async () => {
    const result = await deleteApprovalFlow(deleteFlowId);

    if (result.code === '200') {
      message.success('删除成功');
      setDeleteModalVisible(false);
      loadFlowList();
    } else {
      message.error(result.messageZh || '删除失败');
    }
  };

  const handleFormSubmit = async (data) => {
    let result;
    if (editingFlow) {
      result = await updateApprovalFlow(editingFlow.id, data);
    } else {
      result = await createApprovalFlow(data);
    }

    if (result.code === '200') {
      message.success(editingFlow ? '更新成功' : '创建成功');
      setModalVisible(false);
      setEditingFlow(null);
      loadFlowList();
    } else {
      message.error(result.messageZh || '操作失败');
    }
  };

  const handleFormClose = () => {
    setModalVisible(false);
    setEditingFlow(null);
  };

  const columns = getApprovalFlowColumns({
    handleEdit,
    handleDelete,
  });

  return (
    <div className="approval-flow-config">
      <div className="page-header" style={{ marginBottom: 16 }}>
        <div className="page-header-left">
          <h4 className="page-title">审批流程配置</h4>
          <span className="page-desc">配置不同审批类型的审批流程模板</span>
        </div>
      </div>

      <Card>
        <div className="table-toolbar" style={{ marginBottom: 16 }}>
          <Space>
            <Input.Search
              placeholder="搜索流程名称或代码"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onSearch={handleSearch}
              style={{ width: 300 }}
              enterButton
            />
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleCreate}
            >
              新建流程
            </Button>
          </Space>
        </div>

        <Spin spinning={loading}>
          {flowList.length > 0 ? (
            <>
              <Table
                columns={columns}
                dataSource={flowList}
                rowKey="id"
                pagination={false}
              />
              <div style={{ marginTop: 16, textAlign: 'right' }}>
                <Pagination
                  total={total}
                  current={currentPage}
                  pageSize={pageSize}
                  pageSizeOptions={[10, 20, 50]}
                  showSizeChanger
                  showQuickJumper
                  showTotal={(total) => `共 ${total} 条`}
                  onChange={handlePageChange}
                />
              </div>
            </>
          ) : (
            <Empty description="暂无审批流程" />
          )}
        </Spin>
      </Card>

      <ApprovalFlowFormModal
        visible={modalVisible}
        isEditing={!!editingFlow}
        editingFlow={editingFlow}
        onClose={handleFormClose}
        onSubmit={handleFormSubmit}
      />

      <DeleteConfirmModal
        open={deleteModalVisible}
        onClose={() => setDeleteModalVisible(false)}
        onConfirm={handleConfirmDelete}
        requireConfirmText={deleteFlowCode}
        title="删除审批流程"
        content="此操作将永久删除该审批流程，无法恢复！"
      />
    </div>
  );
}

export default ApprovalFlowConfig;
