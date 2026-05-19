/**
 * ========================================
 * 连接器管理 - 列表页面主组件
 * ========================================
 *
 * 功能：
 * - 展示连接器列表（分页、搜索）
 * - 创建新的连接器
 * - 编辑已有连接器
 * - 删除连接器
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { fetchConnectorList, deleteConnector } from './thunk';
import ConnectorSearchForm from '../../../components/ConnectorSearchForm/ConnectorSearchForm';
import PageList from '../../../components/PageList/PageList';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import SimpleSidebar from '../../../components/SimpleSidebar/SimpleSidebar';
import { pageInfo, searchConfig, connectorStatusOptions, getConnectorColumns } from './constants';
import { INIT_PAGECONFIG } from '../../../utils/constants';
import './Connector.m.less';
import { getSecondModalInfo } from '../../../utils/common';

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

  /**
   * 数据加载
   * @param {Object} params - 请求参数
   */
  const loadData = async (params = {}) => {
    setLoading(true);

    // 合并参数
    const finalParams = {
      keyword: params.keyword ?? keyword,
      curPage: params.curPage ?? pagination.curPage,
      pageSize: params.pageSize ?? pagination.pageSize,
    };

    // 调用API
    const result = await fetchConnectorList(finalParams);

    if (result && result.code === '200') {
      // 更新列表数据
      setData(result.data || []);

      // 更新分页信息
      setPagination(prev => ({
        ...prev,
        total: result.page?.total || 0,
        curPage: finalParams.curPage,
        pageSize: finalParams.pageSize,
      }));
    } else {
      message.error(result?.messageZh || result?.message || '加载列表失败');
    }

    setLoading(false);
  };

  /**
   * 搜索处理
   * @param {Object} formValues - 表单值
   */
  const handleSearch = (formValues) => {
    const searchKeyword = formValues.keyword || '';
    setKeyword(searchKeyword);
    loadData({ keyword: searchKeyword, curPage: 1 });
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
   * 新建连接器
   */
  const handleAdd = () => {
    navigate('/connect/connector-editor');
  };

  /**
   * 编辑连接器
   * @param {Object} record - 连接器记录
   */
  const handleEdit = (record) => {
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
      loadData();
    } else {
      message.error(res?.message || '删除失败');
    }

    setDeleteLoading(false);
  };

  /**
   * 关闭删除确认弹窗
   */
  const handleDeleteCancel = () => {
    setDeleteModalVisible(false);
    setDeleteItemId(null);
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
  });

  /**
   * 渲染
   */
  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      {/* 左侧导航栏 */}
      <SimpleSidebar />

      {/* 主内容区 */}
      <div style={{ flex: 1, overflow: 'auto' }}>
        <div className="connector-management-page">
          {/* 页面头部 */}
          <div className="page-header">
            <div className="page-header-left">
              <h4 className="page-title">{pageInfo.title}</h4>
              <span className="page-desc">{pageInfo.description}</span>
            </div>
            <Button type="primary" onClick={handleAdd} style={{ justifyContent: 'center', borderRadius: 6 }}>
              {pageInfo.addButtonText}
            </Button>
          </div>

          {/* 搜索表单 */}
          <ConnectorSearchForm
            keyword={keyword}
            onSearch={handleSearch}
            placeholder={searchConfig.placeholder}
            statusOptions={connectorStatusOptions}
          />

          {/* 表格列表 */}
          <PageList
            columns={columns}
            dataSource={data}
            loading={loading}
            pagination={{
              curPage: pagination.curPage,
              pageSize: pagination.pageSize,
              total: pagination.total,
            }}
            onPageChange={handlePageChange}
            scroll={{ x: 1200 }}
          />

          {/* 删除确认弹窗 */}
          <DeleteConfirmModal
            open={deleteModalVisible}
            onClose={handleDeleteCancel}
            onConfirm={handleDeleteConfirm}
            modalInfo={getSecondModalInfo('连接器', 'delete', true)}
            loading={deleteLoading}
          />
        </div>
      </div>
    </div>
  );
}

export default ConnectorList;
