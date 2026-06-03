import React, { useState, useEffect } from 'react';
import { Form, Input, message, Table, Pagination, Tooltip } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  getItemList,
  createItem,
  updateItem,
  deleteItem
} from './thunk';
import {
  DEFAULT_SEARCH_VALUES,
  DETAIL_MODE_VIEW,
  DETAIL_MODE_EDIT,
  getTableColumns
} from './constant';
import { DEFAULT_PAGINATION, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import ConfirmModal from '../components/ConfirmModal/ConfirmModal';
import less from './index.module.less';
import ItemSearchBar from './components/ItemSearchBar';
import ItemDetailDrawer from './components/ItemDetailDrawer';
import { renderAlwaysWithTooltip } from '../../../utils/common';

/**
 * LookUp项列表页面组件
 * 提供项的增删改查功能
 */
const ItemList = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [detailForm] = Form.useForm();
  
  const classifyInfo = location.state || {};
  const classifyId = classifyInfo.classifyId || '';
  const classifyCode = classifyInfo.classifyCode || '';
  const classifyName = classifyInfo.classifyName || '';
  
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  const [queryParams, setQueryParams] = useState(DEFAULT_PAGINATION);
  
  const [searchItemCode, setSearchItemCode] = useState(DEFAULT_SEARCH_VALUES.itemCode);
  const [searchItemName, setSearchItemName] = useState(DEFAULT_SEARCH_VALUES.itemName);
  const [searchStatus, setSearchStatus] = useState(DEFAULT_SEARCH_VALUES.status);
  
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailMode, setDetailMode] = useState(DETAIL_MODE_VIEW);
  const [currentItem, setCurrentItem] = useState(null);
  const [saving, setSaving] = useState(false);
  
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [confirmModalConfig, setConfirmModalConfig] = useState({
    title: '',
    content: '',
    onConfirm: null
  });

  /**
   * 获取项列表数据
   */
  const fetchData = async () => {
    if (!classifyId) {
      message.warning('请先选择分类');
      navigate('/lookup-classify');
      return;
    }
    
    setLoading(true);
    const result = await getItemList(classifyId, queryParams);
    
    // 判断接口返回结果是否成功
    if (result && result.code === '200') {
      const responseData = result.data || {};
      setDataSource(responseData.list || []);
      setPagination({
        pageNum: responseData.pageNum || 1,
        pageSize: responseData.pageSize || DEFAULT_PAGINATION.pageSize,
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

  useEffect(() => {
    if (detailVisible && currentItem && detailMode === DETAIL_MODE_EDIT) {
      detailForm.setFieldsValue(currentItem);
    }
  }, [detailVisible, currentItem, detailMode, detailForm]);

  /**
   * 搜索按钮点击处理
   */
  const handleSearch = () => {
    const newParams = {
      itemCode: searchItemCode,
      itemName: searchItemName,
      status: searchStatus,
      DEFAULT_PAGINATION
    };
    setQueryParams(newParams);
  };

  /**
   * 重置按钮点击处理
   */
  const handleReset = () => {
    setSearchItemCode(DEFAULT_SEARCH_VALUES.itemCode);
    setSearchItemName(DEFAULT_SEARCH_VALUES.itemName);
    setSearchStatus(DEFAULT_SEARCH_VALUES.status);
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
    setCurrentItem(null);
    setDetailMode('edit');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  /**
   * 查看按钮点击处理
   */
  const handleView = (record) => {
    setCurrentItem(record);
    setDetailMode('view');
    detailForm.resetFields();
    setDetailVisible(true);
  };

  /**
   * 编辑按钮点击处理
   */
  const handleEdit = (record) => {
    setCurrentItem(record);
    setDetailMode('edit');
    setDetailVisible(true);
  };

  /**
   * 保存表单数据
   */
  const handleSave = async (values) => {
    if (!classifyId) return;
    
    setSaving(true);
    let result;
    if (currentItem) {
      result = await updateItem(currentItem.itemId, values);
    } else {
      result = await createItem(classifyId, values);
    }
    
    if (result && result.code === '200') {
      message.success(currentItem ? '编辑成功' : '新增成功');
      setDetailVisible(false);
      fetchData();
    } else {
      message.error({ content: result?.messageZh || '保存失败' });
    }
    setSaving(false);
  };

  /**
   * 删除项
   */
  const handleDelete = (record) => {
    if (record.status === 1) {
      message.warning('有效状态的项不能删除，请先设置为失效状态');
      return;
    }
    setConfirmModalConfig({
      title: '确认删除',
      content: `确定要删除项 "${record.itemName}" 吗？此操作不可恢复。`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        const result = await deleteItem(record.itemId);
        if (result && result.code === '200') {
          message.success('删除成功');
          if (currentItem?.itemId === record.itemId) {
            setDetailVisible(false);
          }
          setQueryParams({...DEFAULT_PAGINATION});
        } else {
          message.error({ content: result?.messageZh || '删除失败' });
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
      content: `确定要${actionText}项 "${record.itemName}" 吗？`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        message.loading({ content: `正在${actionText}...`, key: 'status' });
        const result = await updateItem(record.itemId, {
          ...record,
          status: newStatus
        });
        if (result && result.code === '200') {
          message.success({ content: `${actionText}成功`, key: 'status' });
          fetchData();
          if (currentItem?.itemId === record.itemId) {
            setCurrentItem({ ...currentItem, status: newStatus });
          }
        } else {
          message.error({ content: result?.messageZh || `${actionText}失败`, key: 'status' });
        }
      }
    });
    setConfirmModalVisible(true);
  };

  /**
   * 返回按钮点击处理
   */
  const handleBack = () => {
    navigate('/lookup-classify');
  };

  /**
   * 抽屉关闭处理
   */
  const handleDrawerClose = () => {
    setDetailVisible(false);
    setCurrentItem(null);
  };

  /**
   * 搜索值对象
   */
  const searchValues = {
    itemCode: searchItemCode,
    itemName: searchItemName,
    status: searchStatus
  };

  /**
   * 获取表格列配置
   */
  const columns = getTableColumns({
    renderItemCode: (text, record) => (
      <span
        className={less.cellLink}
        onClick={() => handleView?.(record)}
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
            <button className={less.backBtn} onClick={handleBack}>← 返回</button>
            <h2 className={less.pageHeadTitle}>LookUp项列表</h2>
          </div>
          <div className={less.classifyInfo}>
            <span className={less.infoTag}>
              <span className={less.infoLabel}>分类编码:</span>
              <Tooltip title={classifyCode} placement="topLeft">
                <span className={less.infoValue}>{classifyCode}</span>
              </Tooltip>
            </span>
            <span className={less.infoTag}>
              <span className={less.infoLabel}>分类名称:</span>
              <Tooltip title={classifyName} placement="topLeft">
                <span className={less.infoValue}>{classifyName}</span>
              </Tooltip>
            </span>
          </div>
        </div>
        
        <ItemSearchBar
          searchValues={searchValues}
          onSearchItemCodeChange={setSearchItemCode}
          onSearchItemNameChange={setSearchItemName}
          onSearchStatusChange={setSearchStatus}
          onSearch={handleSearch}
          onReset={handleReset}
          onAdd={handleAdd}
        />
        
        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey="itemId"
          loading={loading}
          scroll={{ x: '100%' }}
          pagination={false}
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
      
      <ItemDetailDrawer
        open={detailVisible}
        mode={detailMode}
        title={currentItem ? 'LookUp项详情' : '新增LookUp项'}
        loading={saving}
        currentItem={currentItem}
        classifyName={classifyName}
        onClose={handleDrawerClose}
        onSubmit={handleSave}
        detailForm={detailForm}
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

export default ItemList;
