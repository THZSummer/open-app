/**
 * ========================================
 * 连接器编辑页 - 版本选择条组件
 * ========================================
 *
 * 包含：
 * - 左侧版本下拉选择
 * - 右侧按版本状态显示的操作按钮组（编辑/保存/发布/创建草稿/复制到草稿/失效/恢复/删除）
 * - 二次确认弹窗（失效 / 恢复 / 删除复用）
 * 内部维护：版本动作流程 / 弹窗状态 / actionLoading
 */

import React, { useState } from 'react';
import { Button, Select, Tag, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import DeleteConfirmModal from '../../../../components/DeleteConfirmModal/DeleteConfirmModal';
import { getSecondModalInfo } from '../../../../utils/common';
import {
  VERSION_STATUS,
  VERSION_STATUS_MAP,
  CONNECTOR_VERSION_EXPIRE_SECOND_MODAL_INFO,
  CONNECTOR_VERSION_DELETE_SECOND_MODAL_INFO,
} from '../constants';
import {
  createDraftVersion,
  saveDraft,
  publishVersion,
  expireVersion,
  restoreVersion,
  deleteVersion,
  validateForPublish,
} from '../thunk';

const { Option } = Select;

/**
 * 版本选择条组件
 * @param {Object} props
 * props.form antd Form 实例
 * props.connectorId 连接器 ID
 * props.versionList 版本列表
 * props.currentVersion 当前版本对象
 * props.apiConfig 当前 API 配置（用于保存 / 发布时拿兜底数据）
 * props.isEditing 是否处于编辑态
 * props.detailLoading 详情加载态
 * props.onVersionChange 版本切换回调（versionId => void）
 * props.onEnterEdit 进入编辑态回调
 * props.onExitEdit 退出编辑态回调（保存 / 发布成功后调用）
 * props.onReloadVersions 重新加载版本列表回调，参数 { keepCurrent, preferVersionId }
 * props.onScrollToSection 校验失败时滚动到指定 section，参数 sectionKey
 */
const VersionBar = (props) => {
  // 解构 props 中需要使用的字段
  const {
    form,
    connectorId,
    versionList,
    currentVersion,
    apiConfig,
    isEditing,
    detailLoading,
    onVersionChange,
    onEnterEdit,
    onExitEdit,
    onReloadVersions,
    onScrollToSection,
  } = props;

  // 动作 loading（保存/发布/版本操作，仅本组件按钮使用）
  const [actionLoading, setActionLoading] = useState(false);
  // 二次确认弹窗（统一管理 expire / restore / delete 三种）
  const [confirmModal, setConfirmModal] = useState({ open: false, type: null });

  // 版本状态便捷判断
  const isDraft = currentVersion?.status === VERSION_STATUS.DRAFT;
  const isPublished = currentVersion?.status === VERSION_STATUS.PUBLISHED;
  const isExpired = currentVersion?.status === VERSION_STATUS.EXPIRED;

  /**
   * 保存（保存草稿后退出编辑态，不做发布校验）
   */
  const handleSave = async () => {
    if (!currentVersion?.versionId) return;
    setActionLoading(true);
    const res = await saveDraft({
      connectorId,
      versionId: currentVersion.versionId,
      config: form.getFieldValue('apiConfig') || apiConfig,
    });
    if (res?.code === '200') {
      message.success('保存成功');
      onExitEdit?.();
    } else {
      message.error(res?.messageZh || '保存失败');
    }
    setActionLoading(false);
  };

  /**
   * 发布草稿（前置校验）
   */
  const handlePublish = async () => {
    if (!currentVersion?.versionId) return;
    const cfg = form.getFieldValue('apiConfig') || apiConfig;

    // 发布前校验：失败时滚动到对应 section
    const err = validateForPublish(cfg);
    if (err) {
      message.error(err.message);
      onScrollToSection?.(err.section);
      return;
    }

    setActionLoading(true);
    const res = await publishVersion({
      connectorId,
      versionId: currentVersion.versionId,
      config: cfg,
    });
    if (res?.code === '200') {
      message.success('发布成功');
      onExitEdit?.();
      await onReloadVersions?.({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '发布失败');
    }
    setActionLoading(false);
  };

  /**
   * 创建草稿 / 复制到草稿
   */
  const handleCreateDraft = async () => {
    setActionLoading(true);
    const res = await createDraftVersion({
      connectorId,
      baseVersionId: currentVersion?.versionId || null,
    });
    if (res?.code === '200') {
      message.success('已创建草稿');
      await onReloadVersions?.({ preferVersionId: res.data?.versionId });
    } else {
      message.error(res?.messageZh || '创建失败');
    }
    setActionLoading(false);
  };

  /**
   * 失效当前版本
   */
  const doExpire = async () => {
    setActionLoading(true);
    const res = await expireVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已失效');
      await onReloadVersions?.({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '失效失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 恢复当前版本（已失效 -> 已发布）
   * 非破坏性操作，直接执行，无需二次确认
   */
  const doRestore = async () => {
    // 当前版本不存在，直接返回
    if (!currentVersion?.versionId) return;

    setActionLoading(true);
    const res = await restoreVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已恢复');
      await onReloadVersions?.({ preferVersionId: currentVersion.versionId });
    } else {
      message.error(res?.messageZh || '恢复失败');
    }
    setActionLoading(false);
  };

  /**
   * 删除当前版本（草稿 / 已失效）
   */
  const doDelete = async () => {
    setActionLoading(true);
    const res = await deleteVersion({
      connectorId,
      versionId: currentVersion.versionId,
    });
    if (res?.code === '200') {
      message.success('已删除');
      await onReloadVersions?.({ keepCurrent: false });
    } else {
      message.error(res?.messageZh || '删除失败');
    }
    setActionLoading(false);
    setConfirmModal({ open: false, type: null });
  };

  /**
   * 拼接版本对象名
   * 优先 "v{versionNo} ({versionName})"，缺失则回退 versionId
   */
  const getVersionObjectName = () => {
    // 当前选中版本
    const version = currentVersion;
    if (!version) return '';

    // 版本号字段（兼容多种命名）
    const versionNo = version.versionNo || version.versionNumber;
    // 版本名称字段（兼容多种命名）
    const versionName = version.versionName || version.name;

    if (versionNo && versionName) {
      return `v${versionNo} (${versionName})`;
    }
    if (versionNo) {
      return `v${versionNo}`;
    }
    if (versionName) {
      return versionName;
    }
    return version.versionId || '';
  };

  /**
   * 二次确认弹窗内容
   * - 失效 / 删除：走新版 getSecondModalInfo 范式，包含确认文案 + 操作影响双段
   * - 恢复操作非破坏性，已改为直接执行，不进入此方法
   */
  const getConfirmModalInfo = () => {
    if (confirmModal.type === 'expire') {
      // 失效版本：使用版本对象名拼接确认文案
      return {
        ...getSecondModalInfo({
          ...CONNECTOR_VERSION_EXPIRE_SECOND_MODAL_INFO,
          objectName: getVersionObjectName(),
        }),
        onConfirm: doExpire,
      };
    }
    // 删除版本：使用版本对象名拼接确认文案
    return {
      ...getSecondModalInfo({
        ...CONNECTOR_VERSION_DELETE_SECOND_MODAL_INFO,
        objectName: getVersionObjectName(),
      }),
      onConfirm: doDelete,
    };
  };

  /**
   * 打开二次确认弹窗
   * @param {string} type 弹窗类型 'expire' | 'delete'
   */
  const openConfirm = (type) => {
    setConfirmModal({ open: true, type });
  };

  /**
   * 渲染版本栏右侧操作按钮（按版本状态显隐）
   */
  const renderVersionActions = () => {
    // 无版本：仅展示"创建草稿"
    if (!currentVersion) {
      return (
        <Button
          type="primary"
          icon={<PlusOutlined />}
          className="primary-btn"
          loading={actionLoading}
          onClick={handleCreateDraft}
        >
          创建草稿
        </Button>
      );
    }

    return (
      <>
        {isDraft && (
          <>
            {/* 编辑 / 保存 切换 */}
            {!isEditing ? (
              <Button
                type="primary"
                className="primary-btn"
                onClick={onEnterEdit}
              >
                编辑
              </Button>
            ) : (
              <Button
                type="primary"
                className="primary-btn"
                loading={actionLoading}
                onClick={handleSave}
              >
                保存
              </Button>
            )}
            <Button loading={actionLoading} onClick={handlePublish}>
              发布
            </Button>
            <Button
              className="danger-btn"
              onClick={() => openConfirm('delete')}
            >
              删除
            </Button>
          </>
        )}

        {isPublished && (
          <>
            <Button onClick={handleCreateDraft} loading={actionLoading}>
              复制到草稿
            </Button>
            <Button
              className="danger-btn"
              onClick={() => openConfirm('expire')}
            >
              失效
            </Button>
          </>
        )}

        {isExpired && (
          <>
            <Button onClick={handleCreateDraft} loading={actionLoading}>
              复制到草稿
            </Button>
            <Button
              type="primary"
              className="primary-btn"
              loading={actionLoading}
              onClick={doRestore}
            >
              恢复
            </Button>
            <Button
              className="danger-btn"
              onClick={() => openConfirm('delete')}
            >
              删除
            </Button>
          </>
        )}
      </>
    );
  };

  // 当前弹窗内容
  const confirmInfo = getConfirmModalInfo();

  return (
    <>
      <div className="version-bar">
        {/* 左侧：版本下拉或空提示 */}
        <div className="version-bar-left">
          <span className="version-bar-label">当前版本</span>
          {versionList.length === 0 ? (
            <span className="version-empty">暂无版本，点击右侧"创建草稿"开始配置</span>
          ) : (
            <Select
              className="version-select"
              value={currentVersion?.versionId}
              onChange={onVersionChange}
              disabled={actionLoading || detailLoading}
            >
              {versionList.map(v => (
                <Option key={v.versionId} value={v.versionId} label={v.name}>
                  <div className="version-option">
                    <span className="version-option-name">{v.name}</span>
                    <span className="version-option-time">{v.createTime}</span>
                    <Tag color={VERSION_STATUS_MAP[v.status]?.color}>
                      {VERSION_STATUS_MAP[v.status]?.text}
                    </Tag>
                  </div>
                </Option>
              ))}
            </Select>
          )}
        </div>

        {/* 右侧：操作按钮组 */}
        <div className="version-bar-actions">
          {renderVersionActions()}
        </div>
      </div>

      {/* 二次确认弹窗（失效 / 恢复 / 删除复用） */}
      <DeleteConfirmModal
        open={confirmModal.open}
        onClose={() => setConfirmModal({ open: false, type: null })}
        onConfirm={confirmInfo.onConfirm}
        modalInfo={{
          title: confirmInfo.title,
          content: confirmInfo.content,
          confirmButtonText: confirmInfo.confirmButtonText,
          loadingText: confirmInfo.loadingText,
          dangerColor: confirmInfo.dangerColor,
        }}
      />
    </>
  );
};

export default VersionBar;
