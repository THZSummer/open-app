/**
 * ========================================
 * 连接流编辑器 V2 - 主页面
 * ========================================
 *
 * 功能：
 * - 面包屑返回列表，无返回按钮
 * - 版本下拉 + 详情按钮（右侧 Drawer）+ 状态化操作按钮
 * - 编排模式选择（单节点/串行/并行）
 * - 步骤条 + 加号插入节点 + 当前激活节点卡片
 * - 更多配置抽屉（限流 + 缓存）
 * - 调试抽屉（触发器入参 + 立即调试）
 * - 保存/发布（保存不校验，发布全量校验）
 * - 视觉对齐连接器编辑页
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, message, Modal, Empty } from 'antd';
import VersionBar from './components/VersionBar';
import VersionDetailDrawer from './components/VersionDetailDrawer';
import ModePanel from './components/ModePanel';
import FlowStepper from './components/FlowStepper';
import InsertNodeModal from './components/InsertNodeModal';
import MoreConfigDrawer from './components/MoreConfigDrawer';
import DebugDrawer from './components/DebugDrawer';
import TriggerCard from './components/NodeCards/TriggerCard';
import ConnectorCard from './components/NodeCards/ConnectorCard';
import ScriptCard from './components/NodeCards/ScriptCard';
import ParallelCard from './components/NodeCards/ParallelCard';
import OutputCard from './components/NodeCards/OutputCard';
import DeleteConfirmModal from '../../../components/DeleteConfirmModal/DeleteConfirmModal';
import {
  fetchAppConfig,
  fetchVersionList,
  fetchVersionDetail,
  fetchVersionDetailInfo,
  saveDraft,
  publishVersion,
  createDraftVersion,
  expireVersion,
  restoreVersion,
  withdrawVersion,
  deleteVersion,
  debugFlow,
} from './thunk';
import {
  VERSION_STATUS,
  FLOW_MODE,
  DEFAULT_APP_LIMITS,
  NODE_TYPE,
  FLOW_VERSION_EXPIRE_SECOND_MODAL_INFO,
  FLOW_VERSION_WITHDRAW_SECOND_MODAL_INFO,
  FLOW_VERSION_DELETE_SECOND_MODAL_INFO,
  parseFlowAppConfig,
} from './constants';
import {
  initFlowDataByMode,
  createNodeByType,
  getVisibleNodes,
  computeInsertableTypes,
  canDeleteNode,
  validateForPublish,
  buildNodeTitles,
} from './utils';
import { queryParams, getSecondModalInfo, getVersionObjectName } from '../../../utils/common';
import './FlowEditorV2.m.less';

/**
 * 连接流编辑器 V2 主组件
 */
function FlowEditorV2() {
  const navigate = useNavigate();
  const flowId = queryParams('id');
  const flowName = queryParams('name') || '未命名连接流';

  // 版本管理
  const [versionList, setVersionList] = useState([]);
  const [currentVersion, setCurrentVersion] = useState(null);
  const [actionLoading, setActionLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);

  // 应用级配置与编排模式可见性
  const [appModeVisibility, setAppModeVisibility] = useState({
    single: true, serial: true, parallel: true,
  });
  const [appLimits, setAppLimits] = useState({ ...DEFAULT_APP_LIMITS });

  // 连接流数据
  const [flowData, setFlowData] = useState(initFlowDataByMode(''));
  const [activeNodeId, setActiveNodeId] = useState('');
  // 草稿编辑态：编辑/保存互斥按钮的控制位，初始非编辑（只读查看）
  const [isEditing, setIsEditing] = useState(false);

  // 弹窗/抽屉控制
  const [insertModalVisible, setInsertModalVisible] = useState(false);
  const [insertIndex, setInsertIndex] = useState(-1);
  const [insertableTypes, setInsertableTypes] = useState([]);
  const [moreConfigVisible, setMoreConfigVisible] = useState(false);
  const [debugVisible, setDebugVisible] = useState(false);
  const [debugLoading, setDebugLoading] = useState(false);
  const [debugResult, setDebugResult] = useState(null);
  const [detailDrawerVisible, setDetailDrawerVisible] = useState(false);
  const [versionDetailInfo, setVersionDetailInfo] = useState(null);

  // 版本破坏性操作的二次确认弹窗（失效 / 撤回 / 删除 共用）
  // type 取值：'expire' | 'withdraw' | 'delete'
  const [confirmModal, setConfirmModal] = useState({ open: false, type: null });

  // 派生值
  const isDraft = currentVersion?.status === VERSION_STATUS.DRAFT;
  const isPublished = currentVersion?.status === VERSION_STATUS.PUBLISHED;
  // 已撤回 / 已驳回 版本与草稿一样支持「编辑 → 保存」流程（保存后由后端流转为草稿）
  const isWithdrawn = currentVersion?.status === VERSION_STATUS.WITHDRAWN;
  const isRejected = currentVersion?.status === VERSION_STATUS.REJECTED;
  // 只有支持编辑的版本状态（草稿 / 撤回 / 驳回）且已点击「编辑」进入编辑态时，才允许切换编排模式、增删节点、编辑节点表单
  const editable = (isDraft || isWithdrawn || isRejected) && isEditing;
  const hasVersion = versionList.length > 0;
  const hasMode = !!flowData?.flowMode;
  const visibleNodes = hasVersion && hasMode ? getVisibleNodes(flowData) : [];
  const activeNode = visibleNodes.find(n => n.id === activeNodeId) || visibleNodes[0] || null;
  const isSingleMode = flowData?.flowMode === FLOW_MODE.SINGLE;

  // ========================================
  // 初始化
  // ========================================
  useEffect(() => {
    if (!flowId) {
      message.error('缺少连接流ID参数');
      navigate('/flowList');
      return;
    }

    const initPage = async () => {
      const appConfigRes = await fetchAppConfig();
      const appConfig = parseFlowAppConfig(appConfigRes);
      setAppModeVisibility(appConfig.flowModeVisibility);
      setAppLimits({
        rateLimitMax: appConfig.rateLimitMax,
        connectorTimeoutMax: appConfig.connectorTimeoutMax,
        serialConnectorMax: appConfig.serialConnectorMax,
        parallelBranchMax: appConfig.parallelBranchMax,
        cacheTimeMax: DEFAULT_APP_LIMITS.cacheTimeMax,
      });
      await loadVersions(flowId);
    };

    initPage();
  }, [flowId]);

  /**
   * 加载版本列表
   *
   * @param {string} id 连接流 ID
   * @param {Object} options 可选参数
   * @param {string} options.preferVersionId 优先选中的版本 ID
   */
  const loadVersions = async (id, options) => {
    const { preferVersionId } = options || {};
    setDetailLoading(true);

    const res = await fetchVersionList(id);
    if (res?.code !== '200') {
      message.error(res?.messageZh || '加载版本列表失败');
      setDetailLoading(false);
      return;
    }

    const list = res.data || [];
    setVersionList(list);

    if (list.length === 0) {
      setCurrentVersion(null);
      setFlowData(initFlowDataByMode(''));
      setActiveNodeId('');
      setDetailLoading(false);
      return;
    }

    let target = null;
    if (preferVersionId) {
      target = list.find(v => v.versionId === preferVersionId);
    }
    if (!target) target = list[0];

    await loadVersionDetail(id, target.versionId);
    setDetailLoading(false);
  };

  /**
   * 加载版本详情
   *
   * @param {string} fid 连接流 ID
   * @param {string} versionId 版本 ID
   */
  const loadVersionDetail = async (fid, versionId) => {
    // 切换/加载版本详情时强制退出编辑态，确保进入页面后为只读初始状态
    setIsEditing(false);
    const res = await fetchVersionDetail({ flowId: fid, versionId });
    if (res?.code !== '200') {
      message.error(res?.messageZh || '加载版本详情失败');
      return;
    }

    const detail = res.data;
    setCurrentVersion({
      versionId: detail.versionId,
      status: detail.status,
      name: detail.name,
      createTime: detail.createTime,
    });

    const mode = detail.flowMode || '';
    const data = mode ? initFlowDataByMode(mode) : initFlowDataByMode('');
    if (mode) {
      if (detail.trigger) data.trigger = detail.trigger;
      if (detail.steps) data.steps = detail.steps;
      if (detail.output) data.output = detail.output;
      if (detail.rateLimit !== undefined) data.rateLimit = detail.rateLimit;
      if (detail.cacheEnabled !== undefined) data.cacheEnabled = detail.cacheEnabled;
      if (detail.cacheTime !== undefined) data.cacheTime = detail.cacheTime;
      if (detail.cacheKeys) data.cacheKeys = detail.cacheKeys;
    }
    setFlowData(data);

    const nodes = mode ? getVisibleNodes(data) : [];
    setActiveNodeId(nodes.length > 0 ? nodes[0].id : '');
  };

  // ========================================
  // 版本操作
  // ========================================

  /**
   * 版本切换
   * @param {string} versionId 版本 ID
   */
  const handleVersionChange = (versionId) => {
    loadVersionDetail(flowId, versionId);
  };

  /**
   * 根据操作标识分发
   * @param {string} action 操作标识
   */
  const handleVersionAction = async (action) => {
    switch (action) {
      case 'addVersion': return handleAddVersion();
      case 'edit': return setIsEditing(true);
      case 'save': return handleSave();
      case 'publish': return handlePublish();
      case 'newDraft': return handleNewDraft();
      case 'expire': return handleExpire();
      case 'restore': return doRestore();
      case 'withdraw': return handleWithdraw();
      case 'delete': return handleDelete();
      case 'detail': return handleOpenDetail();
      case 'moreConfig': return setMoreConfigVisible(true);
      case 'debug': return handleOpenDebug();
      default: break;
    }
  };

  /**
   * 打开版本详情抽屉
   */
  const handleOpenDetail = async () => {
    if (!currentVersion?.versionId) return;
    setDetailDrawerVisible(true);
    const res = await fetchVersionDetailInfo({
      flowId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      setVersionDetailInfo(res.data);
    }
  };

  /**
   * 添加版本
   */
  const handleAddVersion = async () => {
    setActionLoading(true);
    const res = await createDraftVersion({ flowId, baseVersionId: null });
    if (res?.code === '200') {
      message.success('已创建草稿版本');
      await loadVersions(flowId, { preferVersionId: res.data?.versionId });
    } else {
      message.error(res?.messageZh || '创建失败');
    }
    setActionLoading(false);
  };

  /**
   * 取消编辑并还原当前版本详情
   */
  const handleCancelEdit = async () => {
    if (!currentVersion?.versionId) return;
    setIsEditing(false);
    await loadVersionDetail(flowId, currentVersion.versionId);
  };

  /**
   * 保存草稿（不做完整校验）
   * 已撤回 / 已驳回 状态保存后由后端流转为草稿，需要重新拉取版本列表刷新状态
   */
  const handleSave = async () => {
    if (!currentVersion?.versionId) return;
    setActionLoading(true);
    // 深拷贝当前流程数据，避免直接传引用导致后续编辑影响请求体
    const config = JSON.parse(JSON.stringify(flowData));
    // 记录保存前是否为「撤回 / 驳回」态，用于决定保存后是否刷新版本列表
    const needReloadAfterSave = isWithdrawn || isRejected;
    const res = await saveDraft({
      flowId, versionId: currentVersion.versionId, config,
    });
    if (res?.code === '200') {
      message.success('保存成功');
      // 保存成功后退出编辑态，按钮回到「编辑」展示
      setIsEditing(false);
      // 撤回 / 驳回 → 草稿 的状态流转需刷新版本列表
      if (needReloadAfterSave) {
        await loadVersions(flowId, { preferVersionId: currentVersion.versionId });
      }
    } else {
      message.error(res?.messageZh || '保存失败');
    }
    setActionLoading(false);
  };

  /**
   * 发布版本（全量校验）
   */
  const handlePublish = async () => {
    if (!currentVersion?.versionId) return;
    const err = validateForPublish({ flowData, appLimits });
    if (err) {
      message.error(err.message);
      return;
    }
    Modal.confirm({
      title: '确认发布',
      content: '发布后将进入审批中状态，是否继续？',
      onOk: async () => {
        setActionLoading(true);
        const res = await publishVersion({
          flowId,
          versionId: currentVersion.versionId,
        });
        if (res?.code === '200') {
          message.success('发布成功');
          await loadVersions(flowId, { preferVersionId: currentVersion.versionId });
        } else {
          message.error(res?.messageZh || '发布失败');
        }
        setActionLoading(false);
      },
    });
  };

  /**
   * 新增草稿
   */
  const handleNewDraft = async () => {
    Modal.confirm({
      title: '新增草稿',
      content: '基于当前版本创建新的草稿版本？',
      onOk: async () => {
        setActionLoading(true);
        const res = await createDraftVersion({
          flowId, baseVersionId: currentVersion?.versionId,
        });
        if (res?.code === '200') {
          message.success('已创建草稿');
          await loadVersions(flowId, { preferVersionId: res.data?.versionId });
        } else {
          message.error(res?.messageZh || '创建失败');
        }
        setActionLoading(false);
      },
    });
  };

  /**
   * 失效（触发二次确认弹窗）
   */
  const handleExpire = () => {
    if (!currentVersion?.versionId) return;
    setConfirmModal({ open: true, type: 'expire' });
  };

  /**
   * 执行失效操作
   */
  const doExpire = async () => {
    setActionLoading(true);
    const res = await expireVersion({
      flowId, versionId: currentVersion?.versionId,
    });
    if (res?.code === '200') {
      message.success('已失效');
      await loadVersions(flowId, { preferVersionId: currentVersion?.versionId });
    } else {
      message.error(res?.messageZh || '失效失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 执行恢复操作
   */
  const doRestore = async () => {
    if (!currentVersion?.versionId) return;

    setActionLoading(true);
    const res = await restoreVersion({
      flowId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已恢复');
      await loadVersions(flowId, { preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '恢复失败');
    }
    setActionLoading(false);
  };

  /**
   * 撤回（触发二次确认弹窗）
   */
  const handleWithdraw = () => {
    if (!currentVersion?.versionId) return;
    setConfirmModal({ open: true, type: 'withdraw' });
  };

  /**
   * 执行撤回操作
   */
  const doWithdraw = async () => {
    setActionLoading(true);
    const res = await withdrawVersion({
      flowId, versionId: currentVersion?.versionId,
    });
    if (res?.code === '200') {
      message.success('已撤回');
      await loadVersions(flowId, { preferVersionId: currentVersion?.versionId });
    } else {
      message.error(res?.messageZh || '撤回失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 删除（触发二次确认弹窗）
   */
  const handleDelete = () => {
    if (!currentVersion?.versionId) return;
    setConfirmModal({ open: true, type: 'delete' });
  };

  /**
   * 执行删除操作
   */
  const doDelete = async () => {
    setActionLoading(true);
    const res = await deleteVersion({
      flowId, versionId: currentVersion?.versionId,
    });
    if (res?.code === '200') {
      message.success('已删除');
      await loadVersions(flowId);
    } else {
      message.error(res?.messageZh || '删除失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 获取二次确认弹窗配置（含 onConfirm 回调）
   */
  const getConfirmModalInfo = () => {
    // 版本对象名（用于确认文案拼接）
    const objectName = getVersionObjectName(currentVersion);

    if (confirmModal.type === 'expire') {
      return {
        modalInfo: getSecondModalInfo({
          ...FLOW_VERSION_EXPIRE_SECOND_MODAL_INFO,
          objectName,
        }),
        onConfirm: doExpire,
      };
    }
    if (confirmModal.type === 'withdraw') {
      return {
        modalInfo: getSecondModalInfo({
          ...FLOW_VERSION_WITHDRAW_SECOND_MODAL_INFO,
          objectName,
        }),
        onConfirm: doWithdraw,
      };
    }
    // 默认 delete
    return {
      modalInfo: getSecondModalInfo({
        ...FLOW_VERSION_DELETE_SECOND_MODAL_INFO,
        objectName,
      }),
      onConfirm: doDelete,
    };
  };

  const confirmInfo = getConfirmModalInfo();

  // ========================================
  // 编排模式
  // ========================================

  /**
   * 选择编排模式
   * @param {string} mode 编排模式
   */
  const handleSelectMode = (mode) => {
    const newData = initFlowDataByMode(mode);
    setFlowData(newData);
    const nodes = getVisibleNodes(newData);
    setActiveNodeId(nodes.length > 0 ? nodes[0].id : '');
  };

  // ========================================
  // 节点操作
  // ========================================

  /**
   * 切换激活节点
   * @param {string} nodeId 节点 ID
   */
  const handleSelectNode = (nodeId) => {
    setActiveNodeId(nodeId);
  };

  /**
   * 更新当前节点数据
   * @param {Object} updatedNode 更新后的节点对象
   */
  const handleNodeChange = (updatedNode) => {
    const next = { ...flowData };
    if (updatedNode.id === flowData.trigger.id) {
      next.trigger = updatedNode;
    } else if (updatedNode.id === flowData.output.id) {
      next.output = updatedNode;
    } else {
      next.steps = flowData.steps.map(s => (s.id === updatedNode.id ? updatedNode : s));
    }
    setFlowData(next);
  };

  /**
   * 加号点击
   * @param {number} index 插入位置
   */
  const handleAddNodeClick = (index) => {
    const types = computeInsertableTypes({
      flowMode: flowData.flowMode, steps: flowData.steps, insertIndex: index, appLimits,
    });
    if (types.length === 0) return;
    setInsertIndex(index);
    if (types.length === 1) {
      doInsertNode(types[0], index);
    } else {
      setInsertableTypes(types);
      setInsertModalVisible(true);
    }
  };

  /**
   * 弹窗选择后插入节点
   * @param {string} type 节点类型
   */
  const handleInsertNodeSelect = (type) => {
    doInsertNode(type, insertIndex);
  };

  /**
   * 执行插入
   *
   * @param {string} type 节点类型
   * @param {number} index 插入位置
   */
  const doInsertNode = (type, index) => {
    const newNode = createNodeByType(type);
    if (!newNode) return;
    const nextSteps = [...flowData.steps];
    nextSteps.splice(index, 0, newNode);
    setFlowData({ ...flowData, steps: nextSteps });
    setActiveNodeId(newNode.id);
    setInsertModalVisible(false);
  };

  /**
   * 删除节点
   * @param {Object} node 要删除的节点
   */
  const handleRemoveNode = (node) => {
    const removable = canDeleteNode({
      node, flowMode: flowData.flowMode, steps: flowData.steps,
    });
    if (!removable) {
      message.warning('该节点不可删除');
      return;
    }
    const nextSteps = flowData.steps.filter(s => s.id !== node.id);
    setFlowData({ ...flowData, steps: nextSteps });
    const nodes = getVisibleNodes({ ...flowData, steps: nextSteps });
    if (activeNodeId === node.id || !nodes.find(n => n.id === activeNodeId)) {
      setActiveNodeId(nodes.length > 0 ? nodes[0].id : '');
    }
  };

  // ========================================
  // 更多配置
  // ========================================

  /**
   * 保存更多配置
   *
   * @param {Object} config 更多配置
   */
  const handleSaveMoreConfig = async (config) => {
    if (!editable || !currentVersion?.versionId) {
      message.warning('请先进入编辑状态');
      return;
    }

    const nextFlowData = {
      ...flowData,
      rateLimit: config.rateLimit,
      cacheEnabled: config.cacheEnabled,
      cacheTime: config.cacheTime,
      cacheKeys: config.cacheKeys,
    };
    const requestConfig = JSON.parse(JSON.stringify(nextFlowData));
    const needReloadAfterSave = isWithdrawn || isRejected;

    setActionLoading(true);
    const res = await saveDraft({
      flowId,
      versionId: currentVersion.versionId,
      config: requestConfig,
    });
    setActionLoading(false);

    if (res?.code === '200') {
      setFlowData(nextFlowData);
      message.success('更多配置已保存');
      setMoreConfigVisible(false);
      if (needReloadAfterSave) {
        await loadVersions(flowId, { preferVersionId: currentVersion.versionId });
      }
    } else {
      message.error(res?.messageZh || '更多配置保存失败');
    }
  };

  // ========================================
  // 调试
  // ========================================

  /**
   * 打开调试抽屉
   */
  const handleOpenDebug = () => {
    if (!isDraft && !isPublished) {
      message.warning('仅草稿和已发布版本支持调试');
      return;
    }
    setDebugResult(null);
    setDebugVisible(true);
  };

  /**
   * 执行调试
   *
   * @param {Object} paramValues 入参值
   */
  const handleDebug = async (paramValues) => {
    setDebugLoading(true);
    // 传入当前版本 ID，用于调试指定版本。
    const res = await debugFlow({
      flowId,
      versionId: currentVersion?.versionId,
      inputParams: paramValues,
    });
    setDebugLoading(false);
    if (res?.code === '200') {
      const data = res.data || {};

      setDebugResult({
        success: data.status === 'success',
        duration: data.totalDurationMs,
        steps: data.steps || [],
        output: data.resultData,
        error: data.errorMessage || data.errorInfo?.messageZh || data.errorInfo?.messageEn,
      });
    } else {
      setDebugResult({
        success: false, duration: 0, steps: [], output: null,
        error: res?.messageZh || '调试失败',
      });
    }
  };

  // ========================================
  // 渲染
  // ========================================

  const nodeMeta = visibleNodes.map((node, index) => {
    const titles = buildNodeTitles({ nodes: visibleNodes, flowMode: flowData.flowMode });
    return { node, title: titles.get(node.id) || node.type, index };
  });

  /**
   * 节点类型展示标题
   * @param {string} type 节点类型
   * @returns {string} 节点中文标题
   */
  const getNodeTitleText = (type) => {
    if (type === NODE_TYPE.TRIGGER) return '触发器节点';
    if (type === NODE_TYPE.CONNECTOR) return '连接器节点';
    if (type === NODE_TYPE.SCRIPT) return '脚本处理节点';
    if (type === NODE_TYPE.PARALLEL) return '并行节点';
    if (type === NODE_TYPE.OUTPUT) return '数据输出节点';
    return '';
  };

  /**
   * 节点类型 tag 文案
   * @param {string} type 节点类型
   * @returns {string} 节点 tag
   */
  const getNodeTagText = (type) => {
    if (type === NODE_TYPE.TRIGGER) return '入口';
    if (type === NODE_TYPE.OUTPUT) return '出口';
    return '工序';
  };

  /**
   * 按类型渲染激活节点卡片
   * @returns {React.ReactNode}
   */
  const renderActiveNodeCard = () => {
    if (!activeNode) return null;
    const cardProps = {
      node: activeNode, editable, flowData, appLimits, onChange: handleNodeChange,
    };
    if (activeNode.type === NODE_TYPE.TRIGGER) return <TriggerCard {...cardProps} />;
    if (activeNode.type === NODE_TYPE.CONNECTOR) return <ConnectorCard {...cardProps} />;
    if (activeNode.type === NODE_TYPE.SCRIPT) return <ScriptCard {...cardProps} />;
    if (activeNode.type === NODE_TYPE.PARALLEL) return <ParallelCard {...cardProps} />;
    if (activeNode.type === NODE_TYPE.OUTPUT) return <OutputCard {...cardProps} />;
    return null;
  };

  return (
    <div className="flow-editor-v2-page">
      {/* 面包屑 */}
      <div className="content-card">
        <div className="page-breadcrumb">
          <span className="page-breadcrumb-link" onClick={() => navigate(`/flowList?appId=${queryParams('appId')}`)}>连接流列表</span>
          <span className="page-breadcrumb-sep">&gt;</span>
          <span>编排配置</span>
        </div>

        {/* 页面标题 */}
        <div className="page-header">
          <div>
            <h2>{flowName}</h2>
          </div>
        </div>

        {/* 版本选择条 */}
        <VersionBar
          versionList={versionList}
          currentVersion={currentVersion}
          actionLoading={actionLoading}
          isEditing={isEditing}
          onVersionChange={handleVersionChange}
          onCancelEdit={handleCancelEdit}
          onAction={handleVersionAction}
        />
      </div>

      {/* 主体 */}
      {detailLoading ? (
        <div style={{ textAlign: 'center', padding: 100, color: '#8f959e' }}>加载中...</div>
      ) : !hasVersion ? (
        <div className="content-card no-version-state">
          <Empty description="暂无版本，请先创建草稿">
            {/* 空态下创建草稿入口，点击新增一个草稿版本 */}
            <Button
              type="primary"
              className="primary-btn"
              loading={actionLoading}
              onClick={() => handleVersionAction('addVersion')}
            >
              创建草稿
            </Button>
          </Empty>
        </div>
      ) : (
        <>
          {/* 编排模式选择 */}
          <ModePanel
            flowMode={flowData.flowMode}
            editable={editable}
            appModeVisibility={appModeVisibility}
            onSelect={handleSelectMode}
          />

          {/* 步骤条 + 激活节点卡片 */}
          {hasMode && visibleNodes.length > 0 && (
            <>
              <FlowStepper
                nodeMeta={nodeMeta}
                activeId={activeNodeId}
                editable={editable}
                showAddBtn={!isSingleMode}
                onSelect={handleSelectNode}
                onAddNode={handleAddNodeClick}
                onRemoveNode={handleRemoveNode}
                canDeleteNode={(node) => canDeleteNode({
                  node, flowMode: flowData.flowMode, steps: flowData.steps,
                })}
                canInsertAt={(insertIndex) => {
                  // 计算指定加号位置是否有可插入的节点类型
                  const types = computeInsertableTypes({
                    flowMode: flowData.flowMode,
                    steps: flowData.steps,
                    insertIndex,
                    appLimits,
                  });
                  return types.length > 0;
                }}
              />

              {activeNode && (
                <div className="node-card">
                  <div className="node-card-header">
                    <div className="node-card-title">
                      <span className="node-card-tag">{getNodeTagText(activeNode.type)}</span>
                      <span>{getNodeTitleText(activeNode.type)}</span>
                      <span className="node-card-id">{activeNode.id}</span>
                    </div>
                  </div>
                  {renderActiveNodeCard()}
                </div>
              )}
            </>
          )}

          {!hasMode && (
            <div style={{ textAlign: 'center', padding: 60, color: '#8f959e' }}>
              请在上方选择编排类型后开始配置节点
            </div>
          )}
        </>
      )}

      {/* 插入节点弹窗 */}
      <InsertNodeModal
        visible={insertModalVisible}
        insertableTypes={insertableTypes}
        onSelect={handleInsertNodeSelect}
        onCancel={() => setInsertModalVisible(false)}
      />

      {/* 版本详情抽屉 */}
      <VersionDetailDrawer
        visible={detailDrawerVisible}
        versionInfo={versionDetailInfo}
        onClose={() => { setDetailDrawerVisible(false); setVersionDetailInfo(null); }}
      />

      {/* 更多配置抽屉 */}
      <MoreConfigDrawer
        visible={moreConfigVisible}
        editable={editable}
        rateLimit={flowData.rateLimit}
        cacheEnabled={flowData.cacheEnabled}
        cacheTime={flowData.cacheTime}
        cacheKeys={flowData.cacheKeys}
        triggerNodeId={flowData.trigger?.id}
        triggerInputParams={flowData.trigger?.inputParams}
        rateLimitMax={appLimits.rateLimitMax}
        cacheTimeMax={appLimits.cacheTimeMax}
        onClose={() => setMoreConfigVisible(false)}
        onSave={handleSaveMoreConfig}
      />

      {/* 调试抽屉 */}
      <DebugDrawer
        visible={debugVisible}
        flowName={flowName}
        triggerInputParams={flowData.trigger?.inputParams}
        debugResult={debugResult}
        debugLoading={debugLoading}
        onClose={() => setDebugVisible(false)}
        onDebug={handleDebug}
      />

      {/* 版本破坏性操作的二次确认弹窗（失效 / 撤回 / 删除 共用） */}
      <DeleteConfirmModal
        open={confirmModal.open}
        onClose={() => setConfirmModal({ open: false, type: null })}
        onConfirm={confirmInfo.onConfirm}
        modalInfo={confirmInfo.modalInfo}
        loading={actionLoading}
      />
    </div>
  );
}

export default FlowEditorV2;