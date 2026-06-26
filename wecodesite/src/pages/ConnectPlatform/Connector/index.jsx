/**
 * ========================================
 * 连接器管理 - 列表页面主组件
 * ========================================
 *
 * 功能：
 * - 展示连接器列表（分页、搜索）
 * - 创建新的连接器（弹窗表单）
 * - 编辑已有连接器（弹窗表单）
 * - 失效/恢复/删除连接器（按状态展示操作）
 * - 点击配置按钮跳转到配置页面
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Button, Table, Spin, Pagination } from 'antd';
import {
  fetchConnectorList,
  deleteConnector,
  createConnector,
  updateConnector,
  disableConnector,
  restoreConnector,
} from './thunk';
import ConnectorSearchForm from '../../../components/ConnectorSearchForm/ConnectorSearchForm';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import ConnectorFormModal from '../../../components/ConnectorFormModal/ConnectorFormModal';
import {
  pageInfo,
  searchConfig,
  getConnectorColumns,
  CONNECTOR_DELETE_SECOND_MODAL_INFO,
  CONNECTOR_DISABLE_SECOND_MODAL_INFO,
} from './constants';
import { getSecondModalInfo, queryParams } from '../../../utils/common';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import './Connector.m.less';

/**
 * 连接器列表页面主组件
 */
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

  // 操作确认弹窗（删除/失效）相关状态
  const [actionModalVisible, setActionModalVisible] = useState(false);
  const [actionItem, setActionItem] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);

  // 当前操作类型：'delete' | 'disable'
  const [currentActionType, setCurrentActionType] = useState(null);

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
      setPagination((prev) => ({
        ...prev,
        ...(result.page ? result.page : INIT_PAGECONFIG),
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
   * 编辑连接器
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
   * 点击配置按钮，跳转到配置页面（携带 appId）
   * @param {Object} record - 连接器记录
   */
  const handleConfigClick = (record) => {
    // 连接器配置页标题展示优先使用中文名称，缺省时使用英文名称
    const connectorName = record.nameCn || record.nameEn || '';
    navigate(`/connect/connector-editor?id=${record.connectorId}&appId=${queryParams('appId')}&name=${encodeURIComponent(connectorName)}`);
  };

  /**
   * 点击删除按钮（仅已失效连接器可见）
   * @param {Object} record - 连接器记录
   */
  const handleDeleteClick = (record) => {
    setActionItem(record);
    setCurrentActionType('delete');
    setActionModalVisible(true);
  };

  /**
   * 点击失效按钮（仅正常连接器可见）
   * @param {Object} record - 连接器记录
   */
  const handleDisableClick = (record) => {
    setActionItem(record);
    setCurrentActionType('disable');
    setActionModalVisible(true);
  };

  /**
   * 点击恢复按钮（仅已失效连接器可见，直接执行，不需要二次确认）
   * @param {Object} record - 连接器记录
   */
  const handleRestoreClick = async (record) => {
    // 仅传入连接器 ID，后端会将状态由「已失效」改为「有效可用/有效不可用」
    const res = await restoreConnector(record.connectorId);
    if (res && res.code === '200') {
      message.success('恢复成功');
      loadData();
    } else {
      message.error(res?.messageZh || '恢复失败');
    }
  };

  /**
   * 确认执行删除/失效操作
   */
  const handleActionConfirm = async () => {
    if (!actionItem || !currentActionType) return;

    setActionLoading(true);

    let res;
    let successMsg;
    let errorMsg;

    if (currentActionType === 'delete') {
      // 删除接口只需 connectorId
      res = await deleteConnector(actionItem.connectorId);
      successMsg = '删除成功';
      errorMsg = '删除失败';
    } else if (currentActionType === 'disable') {
      // 失效接口只需 connectorId（PUT /connectors/{connectorId}/invalidate）
      res = await disableConnector(actionItem.connectorId);
      successMsg = '已失效';
      errorMsg = '失效操作失败';
    }

    if (res && res.code === '200') {
      message.success(successMsg);
      handleActionCancel();
      // 删除回到第一页，失效保留当前页码
      if (currentActionType === 'delete') {
        loadData(INIT_PAGECONFIG);
      } else {
        loadData();
      }
    } else {
      message.error(res?.messageZh || errorMsg);
    }

    setActionLoading(false);
  };

  /**
   * 关闭操作确认弹窗
   */
  const handleActionCancel = () => {
    setActionItem(null);
    setCurrentActionType(null);
    setActionModalVisible(false);
  };

  /**
   * 关闭表单弹窗
   */
  const handleFormModalCancel = () => {
    setEditRecord(null);
    setFormModalVisible(false);
  };

  /**
   * 表单弹窗确认（创建/编辑）
   * @param {Object} values - 表单值
   */
  const handleFormModalOk = async (values) => {
    setFormModalLoading(true);

    let res;

    if (editRecord) {
      // 编辑（仅基本信息）
      res = await updateConnector(editRecord.connectorId, values);
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
   * 副作用：首次加载列表
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
    handleDisableClick,
    handleRestoreClick,
  });

  /**
   * 渲染
   */
  return (
    <div className="connector-management-page">
      <div className="content-card">
        {/* 页面头部 */}
        <div className="page-header">
          <div className="page-header-left">
            <h2 className="page-title">{pageInfo.title}</h2>
            <p className="page-desc">{pageInfo.description}</p>
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
        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="connectorId"
            pagination={false}
            scroll={{ x: 1600 }}
          />
          <div className="page-pagination">
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
        </Spin>
      </div>

      {/* 操作确认弹窗（删除/失效） */}
      <DeleteConfirmModal
        open={actionModalVisible}
        onClose={handleActionCancel}
        onConfirm={handleActionConfirm}
        modalInfo={getSecondModalInfo({
          ...(currentActionType === 'delete'
            ? CONNECTOR_DELETE_SECOND_MODAL_INFO
            : CONNECTOR_DISABLE_SECOND_MODAL_INFO),
          objectName: actionItem?.nameCn,
        })}
        loading={actionLoading}
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
  );
}

export default ConnectorList;