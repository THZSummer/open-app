/**
 * ========================================
 * 连接流管理 - 列表页面主组件
 * ========================================
 *
 * 功能：
 * - 展示连接流列表（分页、搜索）
 * - 创建新的连接流
 * - 编辑已有连接流
 * - 删除连接流
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Button, Table, Pagination } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { fetchFlowList, deleteFlow, createFlow, updateFlow, stopFlow, startFlow } from './thunk';
import ConnectorSearchForm from '../../../components/ConnectorSearchForm/ConnectorSearchForm';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import ConnectorFormModal from '../../../components/ConnectorFormModal/ConnectorFormModal';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import { pageInfo, flowSearchConfig, flowStatusOptions, getFlowColumns, stopFlowModalInfo, deleteFlowModalInfo } from './constants';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './Flow.m.less';

/**
 * 连接流列表页面主组件
 */
function FlowList() {
  const navigate = useNavigate();

  /**
   * State定义
   */

  // 连接流列表数据
  const [data, setData] = useState([]);

  // 加载状态
  const [loading, setLoading] = useState(false);

  // 分页配置
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);

  // 搜索关键词
  const [keyword, setKeyword] = useState('');

  // 删除确认弹窗相关状态
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteItem, setDeleteItem] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // 当前操作类型（delete/stop）
  const [currentActionType, setCurrentActionType] = useState(null);

  // 创建/编辑弹窗相关状态
  const [modalVisible, setModalVisible] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);

  // 停止确认弹窗相关状态
  const [stopModalVisible, setStopModalVisible] = useState(false);
  const [stopItem, setStopItem] = useState(null);
  const [stopLoading, setStopLoading] = useState(false);

  /**
   * 数据加载
   * @param {Object} params - 请求参数
   */
  const loadData = async (params = {}) => {
    setLoading(true);

    // 调用API
    const result = await fetchFlowList({
      curPage: params.curPage ?? pagination.curPage,
      keyword: params.keyword ?? keyword,
      pageSize: params.pageSize ?? pagination.pageSize,
    });

    if (result && result.code === '200') {
      // 使用接口返回的page数据更新分页信息
      const pageData = result.page || {};
      setPagination({
        curPage: pageData.curPage ?? 1,
        pageSize: pageData.pageSize ?? 10,
        total: Number(pageData.total) || 0,
        totalPages: pageData.totalPages ?? 0,
      });
      // 更新列表数据
      setData(result.data || []);
    } else {
      message.error(result?.messageZh || '加载列表失败');
    }

    setLoading(false);
  };

  /**
   * 分页变化处理
   * @param {number} page - 页码
   * @param {number} size - 每页条数
   */
  const handlePageChange = (page, size) => {
    loadData({ curPage: page, pageSize: size });
  };

  /**
   * 搜索处理
   * @param {Object} formValues - 表单值
   */
  const handleSearch = (formValues) => {
    const searchKeyword = formValues.keyword || '';
    loadData({ ...INIT_PAGECONFIG, keyword: searchKeyword });
    setKeyword(searchKeyword);
  };

  /**
   * 编辑连接流
   * @param {Object} record - 连接流记录
   */
  const handleEdit = (record) => {
    setModalVisible(true);
    setEditItem(record);
  };

  /**
   * 新建连接流
   */
  const handleAdd = () => {
    setModalVisible(true);
    setEditItem(null);
  };

  /**
   * 查看连接流详情
   * @param {string} id - 连接流ID
   */
  const handleView = (id) => {
    const record = data.find(item => item.id === id);
    if (record) {
      navigate(`/connect/flows/editor?id=${record.id}&name=${encodeURIComponent(record.nameCn)}`);
    }
  };

  /**
   * 操作按钮点击处理（统一处理删除、停止、启动）
   * @param {Object} record - 连接流记录
   * @param {string} action - 操作类型：delete/stop/start
   */
  const handleActionClick = (record, action) => {
    if (action === 'start') {
      // 启动操作直接执行
      executeAction(record, 'start');
    } else if (action === 'delete') {
      // 删除操作显示确认弹窗
      setCurrentActionType('delete');
      setDeleteItem(record);
      setDeleteModalVisible(true);
    } else if (action === 'stop') {
      // 停止操作显示确认弹窗
      setCurrentActionType('stop');
      setStopItem(record);
      setStopModalVisible(true);
    }
  };

  /**
   * 执行操作（统一的方法）
   * @param {Object} record - 连接流记录
   * @param {string} action - 操作类型：delete/stop/start
   */
  const executeAction = async (record, action) => {
    let apiFunc;
    let successMsg;
    let errorMsg;
    let setLoading;

    switch (action) {
      case 'delete':
        apiFunc = deleteFlow;
        successMsg = '删除成功';
        errorMsg = '删除失败';
        setLoading = setDeleteLoading;
        break;
      case 'stop':
        apiFunc = stopFlow;
        successMsg = '停止成功';
        errorMsg = '停止失败';
        setLoading = setStopLoading;
        break;
      case 'start':
        apiFunc = startFlow;
        successMsg = '启动成功';
        errorMsg = '启动失败';
        setLoading = null;
        break;
      default:
        return;
    }

    setLoading(true);

    const res = await apiFunc(record.id);

    if (res && res.code === '200') {
       message.success(successMsg);
       // 关闭弹窗并清理状态
       handleActionCancel();
       loadData(INIT_PAGECONFIG);
     } else {
       message.error(res?.messageZh || errorMsg);
     }

     if (setLoading) setLoading(false);
  };

  /**
   * 确认删除/停止操作
   */
  const handleActionConfirm = () => {
    if (currentActionType === 'delete' && deleteItem) {
      executeAction(deleteItem, 'delete');
    } else if (currentActionType === 'stop' && stopItem) {
      executeAction(stopItem, 'stop');
    }
  };

  /**
   * 关闭删除/停止确认弹窗
   */
  const handleActionCancel = () => {
    if (currentActionType === 'delete') {
      setDeleteModalVisible(false);
      setDeleteItem(null);
    } else if (currentActionType === 'stop') {
      setStopModalVisible(false);
      setStopItem(null);
    }
    setCurrentActionType(null);
  };

  /**
   * 提交表单
   * @param {Object} values - 表单值
   */
  const handleModalSubmit = async (values) => {
    setModalLoading(true);

    // 判断是编辑还是创建
    const isEdit = !!editItem?.id;
    const result = isEdit
      ? await updateFlow({ flowId: editItem.id, data: values })
      : await createFlow(values);

    if (result && result.code === '200') {
      message.success(isEdit ? '编辑成功' : '创建成功');
      setModalVisible(false);
      setEditItem(null);
      // 新增回到第一页，编辑留在当前页
      if (isEdit) {
        loadData();
      } else {
        loadData(INIT_PAGECONFIG);
      }
    } else {
      message.error(result?.messageZh || (isEdit ? '编辑失败' : '创建失败'));
    }

    setModalLoading(false);
  };

  /**
   * 关闭弹窗
   */
  const handleModalCancel = () => {
    setEditItem(null);
    setModalVisible(false);
  };

  /**
   * 副作用
   */
  useEffect(() => {
    loadData();
  }, []);

  /**
   * 获取表格列配置
   */
  const columns = getFlowColumns({
    handleEdit,
    handleView,
    handleActionClick,
  });

  /**
   * 渲染
   */
  return (
    <div className="page-container">
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="flow-management-page">
          {/* 页面头部 */}
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">{pageInfo.title}</h4>
              <span className="page-desc">{pageInfo.description}</span>
            </div>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
              style={{ justifyContent: 'center', borderRadius: 6 }}
            >
              {pageInfo.addButtonText}
            </Button>
          </div>

          {/* 搜索表单 */}
          <ConnectorSearchForm
            keyword={keyword}
            onSearch={handleSearch}
            placeholder={flowSearchConfig.placeholder}
            statusOptions={flowStatusOptions}
          />

          {/* 表格列表 */}
          <Table
            columns={columns}
            dataSource={data}
            loading={loading}
            pagination={false}
            scroll={{ x: 1000 }}
          />
          <div style={{ marginTop: 16, textAlign: 'right' }}>
            <Pagination
              current={pagination.curPage}
              pageSize={pagination.pageSize}
              total={pagination.total}
              onChange={handlePageChange}
              showSizeChanger
              pageSizeOptions={PAGE_SIZE_OPTIONS}
              showQuickJumper
              showTotal={(total) => `共 ${total} 条`}
            />
          </div>

          {/* 删除确认弹窗 */}
          <DeleteConfirmModal
            open={deleteModalVisible}
            onClose={handleActionCancel}
            onConfirm={handleActionConfirm}
            modalInfo={deleteFlowModalInfo}
            loading={deleteLoading}
          />

          {/* 停止确认弹窗 */}
          <DeleteConfirmModal
            open={stopModalVisible}
            onClose={handleActionCancel}
            onConfirm={handleActionConfirm}
            modalInfo={stopFlowModalInfo}
            loading={stopLoading}
          />

          {/* 创建/编辑弹窗 */}
          <ConnectorFormModal
            type="flow"
            visible={modalVisible}
            onCancel={handleModalCancel}
            onOk={handleModalSubmit}
            initialValues={editItem}
            loading={modalLoading}
          />
        </div>
      </div>
    </div>
  );
}

export default FlowList;
