/**
 * ========================================
 * 连接器管理 - 列表页面主组件
 * ========================================
 *
 * 功能：
 * - 展示连接器列表（分页、搜索）
 * - 创建新的连接器（弹窗表单）
 * - 编辑已有连接器（弹窗表单）
 * - 删除连接器
 * - 点击配置按钮跳转到配置页面
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Button, Table, Pagination } from 'antd';
import { fetchConnectorList, deleteConnector, createConnector, updateConnector } from './thunk';
import ConnectorSearchForm from '../../../components/ConnectorSearchForm/ConnectorSearchForm';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import ConnectorFormModal from '../../../components/ConnectorFormModal/ConnectorFormModal';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import { pageInfo, searchConfig, getConnectorColumns, deleteConnectorModalInfo } from './constants';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './Connector.m.less';

function ConnectorList() {
  const navigate = useNavigate();

  /**
   * State定义
   */

  // 连接器列表数据
  const [data, setData] = useState([]);

  // 加载状态
  const [loading, setLoading] = useState(false);

  // 分页配置
  const [pagination, setPagination] = useState(INIT_PAGECONFIG);

  // 搜索关键词
  const [keyword, setKeyword] = useState('');

  // 删除确认弹窗相关状态
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteItemId, setDeleteItemId] = useState(null);
  const [deleteLoading, setDeleteLoading] = useState(false);

  // 连接器表单弹窗相关状态
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalLoading, setFormModalLoading] = useState(false);
  const [editRecord, setEditRecord] = useState(null);

  /**
   * 数据加载
   * @param {Object} params - 请求参数
   */
  const loadData = async (params = {}) => {
    setLoading(true);

    // 调用API
    const result = await fetchConnectorList({
      curPage: params.curPage ?? pagination.curPage,
      keyword: params.keyword ?? keyword,
      pageSize: params.pageSize ?? pagination.pageSize,
    });

    if (result && result.code === '200') {
      // 更新分页信息，使用API返回的分页配置
      setPagination(prev => ({
        ...prev,
        curPage: result.page?.curPage || 1,
        pageSize: result.page?.pageSize || 10,
        total: Number(result.page?.total) || 0,
      }));
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
   * 编辑连接器（弹窗）
   * @param {Object} record - 连接器记录
   */
  const handleEdit = (record) => {
    setFormModalVisible(true);
    setEditRecord(record);
  };

  /**
   * 点击新建连接器按钮
   */
  const handleAdd = () => {
    setFormModalVisible(true);
    setEditRecord(null);
  };

  /**
   * 点击配置按钮，跳转到配置页面
   * @param {Object} record - 连接器记录
   */
  const handleConfigClick = (record) => {
    navigate(`/connect/connector-editor?id=${record.id}`);
  };

  /**
   * 点击删除按钮
   * @param {string} id - 连接器ID
   */
  const handleDeleteClick = (id) => {
    setDeleteItemId(id);
    setDeleteModalVisible(true);
  };

  /**
   * 确认删除连接器
   */
  const handleDeleteConfirm = async () => {
    if (!deleteItemId) return;

    setDeleteLoading(true);
    const res = await deleteConnector(deleteItemId);

    if (res && res.code === '200') {
      message.success('删除成功');
      setDeleteModalVisible(false);
      setDeleteItemId(null);
      loadData(INIT_PAGECONFIG);
    } else {
      message.error(res?.messageZh || '删除失败');
    }

    setDeleteLoading(false);
  };

  /**
   * 关闭表单弹窗
   */
  const handleFormModalCancel = () => {
    setEditRecord(null);
    setFormModalVisible(false);
  };

  /**
   * 关闭删除确认弹窗
   */
  const handleDeleteCancel = () => {
    setDeleteItemId(null);
    setDeleteModalVisible(false);
  };

  /**
   * 表单弹窗确认
   * @param {Object} values - 表单值
   */
  const handleFormModalOk = async (values) => {
    setFormModalLoading(true);

    let res;

    if (editRecord) {
      // 编辑
      res = await updateConnector(editRecord.id, values);
    } else {
      // 新增
      res = await createConnector(values);
    }

    if (res && res.code === '200') {
      message.success(editRecord ? '编辑成功' : '创建成功');
      setFormModalVisible(false);
      setEditRecord(null);

      // 编辑时保持当前页码，新增时刷新列表回到第一页
      if (editRecord) {
        loadData();
      } else {
        loadData(INIT_PAGECONFIG);
      }
    } else {
      message.error(res?.messageZh || '操作失败');
    }

    setFormModalLoading(false);
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
  const columns = getConnectorColumns({
    handleEdit,
    handleDeleteClick,
    handleConfigClick,
  });

  /**
   * 渲染
   */
  return (
    <div className="connector-page-wrapper">
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div className="main-content">
        <div className="connector-management-page">
          {/* 页面头部 */}
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">{pageInfo.title}</h4>
              <span className="page-desc">{pageInfo.description}</span>
            </div>
            <Button type="primary" onClick={handleAdd}>
              {pageInfo.addButtonText}
            </Button>
          </div>

          {/* 搜索表单 */}
          <ConnectorSearchForm
            keyword={keyword}
            onSearch={handleSearch}
            placeholder={searchConfig.placeholder}
          />

          {/* 表格列表 */}
          <Table
            columns={columns}
            dataSource={data}
            loading={loading}
            pagination={false}
            scroll={{ x: 1200 }}
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
            onClose={handleDeleteCancel}
            onConfirm={handleDeleteConfirm}
            modalInfo={deleteConnectorModalInfo}
            loading={deleteLoading}
          />

          {/* 连接器表单弹窗 */}
          <ConnectorFormModal
            visible={formModalVisible}
            onCancel={handleFormModalCancel}
            onOk={handleFormModalOk}
            initialValues={editRecord}
            loading={formModalLoading}
          />
        </div>
      </div>
    </div>
  );
}

export default ConnectorList;
