import React, { useState, useEffect } from 'react';
import { Form, message, Table, Pagination } from 'antd';
import { useNavigate } from 'react-router-dom';
import {
  getDictionaryList,
  createDictionary,
  updateDictionary,
  deleteDictionary
} from './thunk';
import {
  DEFAULT_SEARCH_VALUES,
  getTableColumns
} from './constant';
import { DEFAULT_PAGINATION, PAGE_SIZE_OPTIONS } from '../../../utils/constant';
import ConfirmModal from '../components/ConfirmModal/ConfirmModal';
import less from './index.module.less';
import DictionarySearchBar from './components/DictionarySearchBar';
import DictionaryDetailDrawer from './components/DictionaryDetailDrawer';
import { renderAlwaysWithTooltip } from '../../../utils/common';

/**
 * 数据字典列表页面组件
 * 提供字典的增删改查功能
 */
const DictionaryList = () => {
  const navigate = useNavigate();
  const [detailForm] = Form.useForm();
  
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState([]);
  const [pagination, setPagination] = useState(DEFAULT_PAGINATION);
  
  const [queryParams, setQueryParams] = useState(DEFAULT_PAGINATION);
  
  const [searchValues, setSearchValues] = useState(DEFAULT_SEARCH_VALUES);
  
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailMode, setDetailMode] = useState('view');
  const [currentItem, setCurrentItem] = useState(null);
  const [saving, setSaving] = useState(false);
  
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [confirmModalConfig, setConfirmModalConfig] = useState({
    title: '',
    content: '',
    onConfirm: null
  });

  /**
   * 获取字典列表数据
   */
  const fetchData = async () => {
    setLoading(true);
    const res = await getDictionaryList(queryParams);
    if (res && res.code === '200') {
      setDataSource(res.data.list || []);
      setPagination({
        pageNum: res.data.pageNum || 1,
        pageSize: res.data.pageSize || DEFAULT_PAGINATION.pageSize,
        total: res.data.total || 0
      });
    } else {
      message.error({ content: res?.messageZh || '获取数据失败' });
      setDataSource([]);
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchData();
  }, [queryParams]);

  useEffect(() => {
    if (detailVisible && currentItem && detailMode === 'edit') {
      detailForm.setFieldsValue(currentItem);
    }
  }, [detailVisible, currentItem, detailMode, detailForm]);

  /**
   * 重置按钮点击处理
   */
  const handleReset = () => {
    setSearchValues(DEFAULT_SEARCH_VALUES);
    setQueryParams(DEFAULT_PAGINATION);
  };

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
    setSaving(true);
    let result;
    if (currentItem) {
      result = await updateDictionary(currentItem.id, values);
    } else {
      result = await createDictionary(values);
    }
    
    if (result && result.code === '200') {
      message.success(currentItem ? '编辑成功' : '新增成功');
      setDetailVisible(false);
      fetchData();
    } else {
      message.error({ content: result.messageZh || '保存失败' });
    }
    setSaving(false);
  };

  /**
   * 删除字典
   */
  const handleDelete = (record) => {
    if (record.status === 1) {
      message.warning('有效状态的数据不能删除，请先设置为失效状态');
      return;
    }
    setConfirmModalConfig({
      title: '确认删除',
      content: `确定要删除字典 "${record.name}" 吗？此操作不可恢复。`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        const result = await deleteDictionary(record.id);
        if (result && result.code === '200') {
          message.success('删除成功');
          if (currentItem?.id === record.id) {
            setDetailVisible(false);
          }
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
      content: `确定要${actionText}字典 "${record.name}" 吗？`,
      onConfirm: async () => {
        setConfirmModalVisible(false);
        message.loading({ content: `正在${actionText}...`, key: 'status' });
        const result = await updateDictionary(record.id, {
          ...record,
          status: newStatus
        });
        if (result && result.code === '200') {
          message.success({ content: `${actionText}成功`, key: 'status' });
          fetchData();
          if (currentItem?.id === record.id) {
            setCurrentItem({ ...currentItem, status: newStatus });
          }
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
   * 抽屉关闭处理
   */
  const handleDrawerClose = () => {
    setDetailVisible(false);
    setCurrentItem(null);
  };

  /**
   * 获取表格列配置
   */
  const columns = getTableColumns({
    renderCode: (text, record) => (
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
            <span className={less.pageHeadTitle}>数据字典</span>
          </div>
        </div>
        
        <DictionarySearchBar
          searchValues={searchValues}
          onSearchValuesChange={handleSearchValuesChange}
          onSearch={handleSearch}
          onReset={handleReset}
          onAdd={handleAdd}
        />
        
        <Table
          columns={columns}
          dataSource={dataSource}
          rowKey="id"
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
      
      <DictionaryDetailDrawer
        open={detailVisible}
        mode={detailMode}
        title={currentItem ? '字典详情' : '新增字典'}
        loading={saving}
        currentItem={currentItem}
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

export default DictionaryList;