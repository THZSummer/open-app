import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Table, Pagination } from 'antd';
import {
  getClassifyList,
  createClassify,
  updateClassify,
  deleteClassify
} from './thunk';
import {
  DEFAULT_SEARCH_VALUES,
  MODAL_TITLE_ADD,
  getTableColumns
} from './constant';
import { DEFAULT_PAGINATION, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import ConfirmModal from '../components/ConfirmModal/ConfirmModal';
import less from './index.module.less';
import SearchBar from './components/SearchBar';
import ClassifyFormModal from './components/ClassifyFormModal';
import { renderAlwaysWithTooltip } from '../../../utils/common';

/**
 * LookUp分类列表页面组件
 * 提供分类的增删改查功能
 */
const ClassifyList = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const [queryParams, setQueryParams] = useState(DEFAULT_PAGINATION);
  const [searchValues, setSearchValues] = useState(DEFAULT_SEARCH_VALUES);
  
  const [modalVisible, setModalVisible] = useState(false);
  const [modalTitle, setModalTitle] = useState(MODAL_TITLE_ADD);
  const [editingId, setEditingId] = useState(null);
  const [editingRecord, setEditingRecord] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [confirmModalConfig, setConfirmModalConfig] = useState({
    title: '',
    content: '',
    onConfirm: null
  });

  /**
   * 获取分类列表数据
   */
  const fetchData = async () => {
    setLoading(true);
    const result = await getClassifyList(queryParams);
    if (result && result.code === '200') {
      const responseData = result.data || {};
      setDataSource(responseData.list || []);
      setPagination({
        pageNum: Number(responseData.pageNum) || 1,
        pageSize: Number(responseData.pageSize) || DEFAULT_PAGINATION.pageSize,
        total: Number(responseData.total) || 0
      });
    } else {
      message.error({ content: result?.messageZh || '获取数据失败' });
      setDataSource([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

  /**
   * 搜索按钮点击处理
   */
  const handleSearch = () => {
    const newParams = {
      ...searchValues,
      ...DEFAULT_PAGINATION,
    };
    setQueryParams(newParams);
  };

  /**
   * 重置按钮点击处理
   */
  const handleReset = () => {
    setSearchValues(DEFAULT_SEARCH_VALUES);
    setQueryParams(DEFAULT_PAGINATION);
  };

  /**
   * 分页改变处理
   */
  const handlePageChange = (page, pageSize) => {
    const newParams = {
      ...queryParams,
      pageNum: page,
      pageSize: pageSize || queryParams.pageSize
    };
    setQueryParams(newParams);
  };

  /**
   * 新增按钮点击处理
   */
  const handleAdd = () => {
    setModalTitle('新增分类');
    setEditingId(null);
    setEditingRecord(null);
    setModalVisible(true);
  };

  /**
   * 跳转到分类下的项页面
   */
  const handleGoToItems = (record) => {
    navigate('/lookup-item', { state: record });
  };

  /**
   * 编辑按钮点击处理
   */
  const handleEdit = (record) => {
    setModalTitle('编辑分类');
    setEditingId(record.classifyId);
    setEditingRecord(record);
    setModalVisible(true);
  };

  /**
   * 保存表单数据
   */
  const handleSave = async (values) => {
    setSubmitting(true);
    let result;
    if (editingId) {
      result = await updateClassify(editingId, values);
    } else {
      result = await createClassify(values);
    }
    
    if (result && result.code === '200') {
      message.success(editingId ? '编辑成功' : '新增成功');
      setModalVisible(false);
      fetchData();
    } else {
      message.error({ content: result.messageZh || '保存失败' });
    }
    setSubmitting(false);
  };

  /**
   * 删除分类
   */
  const handleDelete = (record) => {
    if (record.status === 1) {
      message.warning('有效状态的数据不能删除，请先设置为失效状态');
      return;
    }
    setConfirmModalConfig({
      title: '确认删除',
      content: `确定要删除分类 "${record.classifyName}" 吗？此操作不可恢复。`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        const result = await deleteClassify(record.classifyId);
        if (result && result.code === '200') {
          message.success('删除成功');
          // 删除成功后使用默认分页重新查询
          setQueryParams({...DEFAULT_PAGINATION});
        } else {
          message.error({ content: result.messageZh || '删除失败' });
        }
      }
    });
    setConfirmModalVisible(true);
  };

  /**
   * 切换状态
   */
  const handleToggleStatus = (record) => {
    const isEffective = record.status === 1;
    const newStatus = isEffective ? 0 : 1;
    const actionText = isEffective ? '失效' : '生效';
    
    setConfirmModalConfig({
      title: `确认${actionText}`,
      content: `确定要${actionText}分类 "${record.classifyName}" 吗？`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        message.loading({ content: `正在${actionText}...`, key: 'status' });
        const result = await updateClassify(record.classifyId, {
          ...record,
          status: newStatus
        });
        if (result && result.code === '200') {
          message.success({ content: `${actionText}成功`, key: 'status' });
          fetchData();
        } else {
          message.error({ content: result.messageZh || `${actionText}失败`, key: 'status' });
        }
      }
    });
    setConfirmModalVisible(true);
  };

  /**
   * 搜索值变化处理
   */
  const handleSearchValuesChange = (newValues) => {
    setSearchValues(newValues);
  };

  /**
   * 表单弹窗关闭处理
   */
  const handleModalClose = () => {
    setModalVisible(false);
    setEditingRecord(null);
  };

  /**
   * 获取表格列配置
   */
  const columns = getTableColumns({
    renderClassifyCode: (text, record) => (
      <span
        className={less.cellLink}
        onClick={() => handleGoToItems?.(record)}
      >
        {renderAlwaysWithTooltip(text)}
      </span>
    ),
    renderStatus: (status) => {
      const statusInfo = { 1: { text: '有效' }, 0: { text: '失效' } }[status] || {};
      return <span className={status === 1 ? less.statusActive : less.statusInactive}>{statusInfo.text || '-'}</span>;
    },
    renderAction: (_, record) => (
      <div className={less.actionButtons}>
        <span
          className={less.actionButton}
          onClick={() => {
            handleEdit?.(record);
          }}
        >
          编辑
        </span>
        <span
          className={less.actionButton}
          onClick={() => {
            handleToggleStatus?.(record);
          }}
        >
          {record.status === 1 ? '失效' : '生效'}
        </span>
        <span
          className={less.actionButtonDanger}
          onClick={() => {
            handleDelete?.(record);
          }}
        >
          删除
        </span>
      </div>
    )
  });

  return (
    <div className={less.container}>
      <div className={less.page}>
        <div className={less.pageHead}>
          <div className={less.pageHeadLeft}>
            <span className={less.pageHeadTitle}>LookUp分类</span>
          </div>
        </div>
        
        <SearchBar
          searchValues={searchValues}
          onSearchValuesChange={handleSearchValuesChange}
          onSearch={handleSearch}
          onReset={handleReset}
          onAdd={handleAdd}
        />
        
        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey="classifyId"
          loading={loading}
          pagination={false}
          scroll={{ x: '100%' }}
        />
        
        <div className={less.paginationWrapper}>
          <Pagination
            total={pagination.total}
            current={pagination.pageNum}
            pageSize={pagination.pageSize}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            showSizeChanger
            showQuickJumper
            showTotal={(total) => `共 ${total} 条`}
            onChange={handlePageChange}
          />
        </div>
      </div>
      
      <ClassifyFormModal
        open={modalVisible}
        title={modalTitle}
        loading={submitting}
        editingId={editingId}
        initialValues={editingRecord}
        onClose={handleModalClose}
        onSubmit={handleSave}
      />
      
      <ConfirmModal
        open={confirmModalVisible}
        title={confirmModalConfig.title}
        content={confirmModalConfig.content}
        onConfirm={confirmModalConfig.onConfirm}
        onClose={() => setConfirmModalVisible(false)}
      />
    </div>
  );
};

export default ClassifyList;
