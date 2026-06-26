/**
 * ========================================
 * 连接流管理 - 列表页面主组件
 * ========================================
 *
 * 功能：
 * - 展示连接流列表（分页、搜索）
 * - 创建/编辑连接流
 * - 操作列：编辑、配置、更多（按状态展示菜单项）
 * - 启动（仅切换状态，前置校验已部署版本）
 * - 部署（独立动作，仅更新版本绑定，不修改状态）
 * - 复制流、复制 ID、停止、失效、恢复、删除
 *
 * 整改依据：连接流列表需求设计说明书 V1.3
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { message, Button, Table, Spin, Pagination } from 'antd';
import {
  fetchFlowList,
  deleteFlow,
  createFlow,
  updateFlow,
  stopFlow,
  startFlow,
  deployFlow,
  copyFlow,
  disableFlow,
  restoreFlow,
} from './thunk';
import ConnectorSearchForm from '../../../components/ConnectorSearchForm/ConnectorSearchForm';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import ConnectorFormModal from '../../../components/ConnectorFormModal/ConnectorFormModal';
import DeployFlowModal from '../../../components/DeployFlowModal/DeployFlowModal';
import {
  pageInfo,
  flowSearchConfig,
  flowStatusOptions,
  getFlowColumns,
  FLOW_STOP_SECOND_MODAL_INFO,
  FLOW_DELETE_SECOND_MODAL_INFO,
  FLOW_DISABLE_SECOND_MODAL_INFO,
} from './constants';
import { INIT_PAGECONFIG, PAGE_SIZE_OPTIONS } from '../../../utils/constants';
import { queryParams, getSecondModalInfo, copyToClipboard } from '../../../utils/common';
import './Flow.m.less';

/**
 * 二次确认动作配置表
 * key: 'stop' | 'disable' | 'delete'
 * 字段说明：
 * - api：调用的后端接口方法
 * - successMsg：成功提示文案
 * - errorMsg：失败兜底提示文案
 * - modalInfo：二次确认弹窗基础配置
 * - resetToFirstPage：成功后是否回到第一页
 */
const CONFIRM_ACTION_CONFIG = {
  stop: {
    api: stopFlow,
    successMsg: '停止成功',
    errorMsg: '停止失败',
    modalInfo: FLOW_STOP_SECOND_MODAL_INFO,
    resetToFirstPage: false,
  },
  disable: {
    api: disableFlow,
    successMsg: '已失效',
    errorMsg: '失效操作失败',
    modalInfo: FLOW_DISABLE_SECOND_MODAL_INFO,
    resetToFirstPage: false,
  },
  delete: {
    api: deleteFlow,
    successMsg: '删除成功',
    errorMsg: '删除失败',
    modalInfo: FLOW_DELETE_SECOND_MODAL_INFO,
    resetToFirstPage: true,
  },
};

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

  // 二次确认弹窗相关状态（覆盖删除/停止/失效）
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [confirmActionItem, setConfirmActionItem] = useState(null);
  const [confirmActionType, setConfirmActionType] = useState(null);
  const [confirmActionLoading, setConfirmActionLoading] = useState(false);

  // 创建/编辑弹窗相关状态
  const [modalVisible, setModalVisible] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [modalLoading, setModalLoading] = useState(false);

  // 部署版本选择弹窗相关状态
  const [deployModalVisible, setDeployModalVisible] = useState(false);
  const [deployFlowItem, setDeployFlowItem] = useState(null);
  const [deployLoading, setDeployLoading] = useState(false);

  /**
   * 数据加载
   * @param {Object} params - 请求参数
   */
  const loadData = async (params = {}) => {
    setLoading(true);

    // 调用列表接口
    const result = await fetchFlowList({
      curPage: params.curPage ?? pagination.curPage,
      keyword: params.keyword ?? keyword,
      pageSize: params.pageSize ?? pagination.pageSize,
    });

    if (result && result.code === '200') {
      // 使用接口返回的 page 数据更新分页信息
      const pageData = result.page || INIT_PAGECONFIG;
      setPagination(pageData);
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
   * 进入连接流配置页（V2 编辑器）
   * 跳转时携带 appId，确保编排页可回传应用上下文
   * @param {Object} record - 连接流记录
   */
  const handleConfig = (record) => {
    const appId = queryParams('appId');
    navigate(`/connect/flows/editor?id=${record.id}&name=${encodeURIComponent(record.nameCn)}&appId=${appId}`);
  };

  /**
   * 通用：执行简单异步动作（调用 API → 成功提示 → 刷新列表）
   * 适用于：复制流、启动、恢复 等无需二次确认的操作
   */
  const executeFlowAction = async (params) => {
    // 接口方法
    // 连接流 ID
    // 成功提示文案
    // 失败兜底提示文案
    // 成功后是否回到第一页
    const { apiFn, id, successMsg, errorMsg, resetToFirstPage = false } = params;

    const res = await apiFn(id);
    if (res && res.code === '200') {
      message.success(successMsg);
      loadData(resetToFirstPage ? INIT_PAGECONFIG : {});
    } else {
      message.error(res?.messageZh || errorMsg);
    }
  };

  /**
   * 复制流
   * @param {Object} record - 连接流记录
   */
  const handleCopyFlow = (record) => {
    executeFlowAction({
      apiFn: copyFlow,
      id: record.id,
      successMsg: '复制成功',
      errorMsg: '复制失败',
      resetToFirstPage: true,
    });
  };

  /**
   * 复制 ID 到剪贴板
   * @param {Object} record - 连接流记录
   */
  const handleCopyId = async (record) => {
    // 调用通用复制方法（含降级方案）
    const success = await copyToClipboard(record.id);
    if (success) {
      message.success('复制成功');
    } else {
      message.error('复制失败，请手动复制');
    }
  };

  /**
   * 启动连接流（已停止 → 运行中）
   * 调用后端独立 start 接口
   *
   * @param {Object} record - 连接流记录
   */
  const handleStart = (record) => {
    executeFlowAction({
      apiFn: startFlow,
      id: record.id,
      successMsg: '启动成功',
      errorMsg: '启动失败',
    });
  };

  /**
   * 打开部署版本选择弹窗
   * 已停止 / 运行中状态均可触发
   *
   * @param {Object} record - 连接流记录
   */
  const handleOpenDeployModal = (record) => {
    setDeployFlowItem(record);
    setDeployModalVisible(true);
  };

  /**
   * 关闭部署弹窗
   */
  const handleDeployModalCancel = () => {
    setDeployModalVisible(false);
    setDeployFlowItem(null);
  };

  /**
   * 确认部署
   * 仅更新连接流-版本绑定关系，连接流状态保持不变
   *
   * @param {string} versionId - 选中的版本ID
   */
  const handleDeployConfirm = async (versionId) => {
    if (!deployFlowItem) return;

    setDeployLoading(true);
    const res = await deployFlow({
      flowId: deployFlowItem.id,
      versionId,
    });
    setDeployLoading(false);

    if (res && res.code === '200') {
      message.success('部署成功');
      handleDeployModalCancel();
      loadData();
    } else {
      message.error(res?.messageZh || '部署失败');
    }
  };

  /**
   * 恢复连接流（已失效 → 已停止）
   *
   * @param {Object} record - 连接流记录
   */
  const handleRestore = (record) => {
    executeFlowAction({
      apiFn: restoreFlow,
      id: record.id,
      successMsg: '恢复成功',
      errorMsg: '恢复失败',
    });
  };

  /**
   * 触发需要二次确认的操作（停止/失效/删除）
   *
   * @param {Object} record - 连接流记录
   * @param {string} actionType - 操作类型：'stop' | 'disable' | 'delete'
   */
  const triggerConfirmAction = (record, actionType) => {
    setConfirmActionItem(record);
    setConfirmActionType(actionType);
    setConfirmModalVisible(true);
  };

  /**
   * 关闭二次确认弹窗
   */
  const handleConfirmCancel = () => {
    setConfirmModalVisible(false);
    setConfirmActionItem(null);
    setConfirmActionType(null);
  };

  /**
   * 执行二次确认后的操作（停止/失效/删除）
   * 通过 CONFIRM_ACTION_CONFIG 统一分发，避免重复的 switch
   */
  const handleConfirmExecute = async () => {
    if (!confirmActionItem || !confirmActionType) return;

    // 当前操作类型对应的配置项
    const config = CONFIRM_ACTION_CONFIG[confirmActionType];
    if (!config) return;

    setConfirmActionLoading(true);

    const res = await config.api(confirmActionItem.id);

    if (res && res.code === '200') {
      message.success(config.successMsg);
      handleConfirmCancel();
      loadData(config.resetToFirstPage ? INIT_PAGECONFIG : {});
    } else {
      message.error(res?.messageZh || config.errorMsg);
    }

    setConfirmActionLoading(false);
  };

  /**
   * 更多菜单点击处理
   * 通过映射表分发到对应的处理函数，避免冗长的 switch
   *
   * @param {string} key - 菜单 key：copy / copyId / start / deploy / stop / disable / restore / delete
   * @param {Object} record - 连接流记录
   */
  const handleMoreMenuClick = (key, record) => {
    // 菜单 key 到处理函数的映射表
    const handlerMap = {
      copy: handleCopyFlow,
      copyId: handleCopyId,
      start: handleStart,
      deploy: handleOpenDeployModal,
      stop: (item) => triggerConfirmAction(item, 'stop'),
      disable: (item) => triggerConfirmAction(item, 'disable'),
      restore: handleRestore,
      delete: (item) => triggerConfirmAction(item, 'delete'),
    };

    const handler = handlerMap[key];
    if (handler) {
      handler(record);
    }
  };

  /**
   * 提交表单（创建/编辑）
   *
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
   * 关闭创建/编辑弹窗
   */
  const handleModalCancel = () => {
    setEditItem(null);
    setModalVisible(false);
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
  const columns = getFlowColumns({
    handleEdit,
    handleConfig,
    handleMoreMenuClick,
  });

  /**
   * 根据当前操作类型获取二次确认弹窗的配置
   * 直接从 CONFIRM_ACTION_CONFIG 中读取，避免重复的 switch
   */
  const getConfirmModalInfo = () => {
    // 当前操作的连接流名称（用于弹窗内确认文案）
    const objectName = confirmActionItem?.nameCn;

    // 取对应配置；类型异常时兜底使用删除配置
    const baseInfo =
      CONFIRM_ACTION_CONFIG[confirmActionType]?.modalInfo
      || FLOW_DELETE_SECOND_MODAL_INFO;

    return getSecondModalInfo({ ...baseInfo, objectName });
  };

  /**
   * 渲染
   */
  return (
    <div className="flow-management-page">
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
          placeholder={flowSearchConfig.placeholder}
          statusOptions={flowStatusOptions}
        />

        {/* 表格列表 */}
        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            pagination={false}
            scroll={{ x: 1700 }}
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

      {/* 二次确认弹窗（停止/失效/删除） */}
      <DeleteConfirmModal
        open={confirmModalVisible}
        onClose={handleConfirmCancel}
        onConfirm={handleConfirmExecute}
        modalInfo={getConfirmModalInfo()}
        loading={confirmActionLoading}
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

      {/* 部署版本选择弹窗 */}
      <DeployFlowModal
        open={deployModalVisible}
        flow={deployFlowItem}
        loading={deployLoading}
        onCancel={handleDeployModalCancel}
        onOk={handleDeployConfirm}
      />
    </div>
  );
}

export default FlowList;