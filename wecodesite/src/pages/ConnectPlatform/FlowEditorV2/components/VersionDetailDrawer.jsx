/**
 * ========================================
 * 版本详情抽屉组件
 * ========================================
 *
 * 根据版本状态展示对应的版本详细信息与操作入口。
 * 草稿：基本信息
 * 已发布：基本信息 + 发布信息
 * 已失效：基本信息 + 失效时间
 * 审批中：基本信息 + 当前审批人 + 审批地址链接 + 催办按钮
 * 已驳回：基本信息 + 驳回人 + 驳回原因
 * 已撤回：基本信息 + 撤回人 + 撤回时间
 *
 * 视觉：操作控制台风格——左色条 Hero、分组卡片、KV 网格、状态胶囊。
 */

import React, { useState } from 'react';
import { Drawer, Button, message } from 'antd';
import { LinkOutlined, CopyOutlined } from '@ant-design/icons';
import { VERSION_STATUS, VERSION_STATUS_MAP } from '../constants';
import { copyToClipboard, extractEflowId } from '../../../../utils/common';
import '../FlowEditorV2.m.less';

import { remindPeople } from '../../../Admin/Approval/thunk';

/**
 * 版本状态到 Hero 色调的映射
 * @param {number} status 版本状态
 * @returns {string} hero class
 */
const getHeroClass = (status) => {
  if (status === VERSION_STATUS.PUBLISHED) return 'hero-success';
  if (status === VERSION_STATUS.APPROVING) return 'hero-warning';
  if (status === VERSION_STATUS.REJECTED) return 'hero-error';
  if (status === VERSION_STATUS.DRAFT) return '';
  return 'hero-default';
};

/**
 * 版本状态到状态胶囊变体的映射
 * @param {number} status 版本状态
 * @returns {string} pill class
 */
const getPillClass = (status) => {
  if (status === VERSION_STATUS.PUBLISHED) return 'pill-success';
  if (status === VERSION_STATUS.APPROVING) return 'pill-warning';
  if (status === VERSION_STATUS.REJECTED) return 'pill-error';
  if (status === VERSION_STATUS.DRAFT) return 'pill-processing';
  return 'pill-default';
};

/**
 * 渲染一行 KV
 *
 * @param {Object} params
 * @param {string} params.label 标签
 * @param {React.ReactNode} params.value 值
 * @returns {React.ReactNode}
 */
const KvRow = (params) => {
  // params.label / params.value
  const { label, value } = params;
  return (
    <>
      <div className="kv-label">{label}</div>
      <div className="kv-value">{value || '-'}</div>
    </>
  );
};

/**
 * 版本详细信息展示
 *
 * @param {Object} props
 * @param {boolean} props.visible 是否显示
 * @param {Object} props.versionInfo 版本详细信息（来自接口）
 * @param {Function} props.onClose 关闭回调
 * @param {Function} props.onUrge 催办回调
 */
const VersionDetailDrawer = (props) => {
  const { visible, versionInfo, onClose } = props;
  const [remindLoading, setRemindLoading] = useState(false);

  /**
   * 复制审批地址链接到剪贴板
   *
   * @param {string} url 审批地址
   */
  const handleCopyApprovalUrl = async (url) => {
    // 调用通用复制方法（含降级方案）
    const success = await copyToClipboard(url);
    if (success) {
      message.success('审批链接已复制');
    } else {
      message.error('复制失败，请检查浏览器权限');
    }
  };

  /**
   * 处理催办按钮点击事件
   * 从审批链接中提取 eflowId 并调用催办接口
   */
  const handleRemind = async () => {
    const eflowId = extractEflowId(versionInfo.approvalUrl);

    if (!eflowId) {
      message.error('无法提取审批ID');
      return;
    }

    const params = {
      businessId: eflowId,
      businessType: 'FLOW',
    }
    setRemindLoading(true);

    const result = await remindPeople(params);

    if (result && result.code === '200') {
      message.success('催办成功');
    } else {
      message.error(result.messageZh || '催办失败');
    }

    setRemindLoading(false);
  };

  if (!versionInfo) return null;

  const status = versionInfo.status || '';
  const statusInfo = VERSION_STATUS_MAP[status] || {};

  return (
    <Drawer
      title={null}
      placement="right"
      width={520}
      open={visible}
      onClose={onClose}
      destroyOnClose
    >
      {/* Hero 区：版本号 + 状态 + 创建时间 */}
      <div className={`drawer-hero ${getHeroClass(status)}`}>
        <div className="drawer-hero-title">
          <span>版本详情</span>
          <span className={`status-pill ${getPillClass(status)}`}>{statusInfo.text || status}</span>
        </div>
        <div className="drawer-hero-sub">
          {versionInfo.name || '-'} · 创建于 {versionInfo.createTime || '-'}
        </div>
      </div>

      {/* 基础信息卡片 */}
      <div className="drawer-section">
        <div className="section-title">基础信息</div>
        <div className="kv-grid">
          <KvRow label="版本名称" value={versionInfo.name} />
          <KvRow label="创建人" value={versionInfo.createBy} />
          <KvRow label="创建时间" value={versionInfo.createTime} />
          <KvRow label="更新人" value={versionInfo.lastUpdateBy} />
          <KvRow label="更新时间" value={versionInfo.lastUpdateTime} />
        </div>
      </div>

      {/* 已发布 */}
      {status === VERSION_STATUS.PUBLISHED && (
        <div className="drawer-section">
          <div className="section-title">发布信息</div>
          <div className="kv-grid">
            <KvRow label="发布人" value={versionInfo.publishedBy} />
            <KvRow label="发布时间" value={versionInfo.publishedTime} />
          </div>
        </div>
      )}

      {/* 已失效 */}
      {status === VERSION_STATUS.EXPIRED && (
        <div className="drawer-section">
          <div className="section-title">失效信息</div>
          <div className="kv-grid">
            <KvRow label="失效人" value={versionInfo.expireBy} />
            <KvRow label="失效时间" value={versionInfo.expireTime} />
          </div>
        </div>
      )}

      {/* 审批中 */}
      {status === VERSION_STATUS.APPROVING && (
        <div className="drawer-section">
          <div className="section-title">
            审批信息
            <span className="section-title-extra">点击催办可提醒审批人</span>
          </div>
          <div className="kv-grid" style={{ marginBottom: 12 }}>
            <KvRow label="当前审批人" value={versionInfo.approver.userId} />
          </div>

          {versionInfo.approvalUrl && (
            <div className="link-card">
              <div className="link-card-text">
                <div className="link-card-title">
                  <LinkOutlined style={{ marginRight: 6 }} />
                  审批链接
                </div>
                <div className="link-card-sub">{versionInfo.approvalUrl}</div>
              </div>
              {/* 仅图标可点击：复制审批链接到剪贴板 */}
              <span
                className="link-card-arrow"
                title="复制审批链接"
                onClick={(e) => { handleCopyApprovalUrl(versionInfo.approvalUrl) }}
              >
                <CopyOutlined />
              </span>
            </div>
          )}

          <div style={{ marginTop: 12, textAlign: 'right' }}>
            <Button
              type="primary"
              loading={remindLoading}
              onClick={handleRemind}
            >
              催办审批
            </Button>
          </div>
        </div>
      )}

      {/* 已驳回 */}
      {status === VERSION_STATUS.REJECTED && (
        <div className="drawer-section">
          <div className="section-title">驳回信息</div>
          <div className="kv-grid" style={{ marginBottom: 12 }}>
            <KvRow label="驳回人" value={versionInfo.rejector} />
          </div>
          {versionInfo.rejectReason && (
            <div className="callout-error">{versionInfo.rejectReason}</div>
          )}
        </div>
      )}

      {/* 已撤回 */}
      {status === VERSION_STATUS.WITHDRAWN && (
        <div className="drawer-section">
          <div className="section-title">撤回信息</div>
          <div className="kv-grid">
            <KvRow label="撤回人" value={versionInfo.lastUpdateBy} />
            <KvRow label="撤回时间" value={versionInfo.lastUpdateTime} />
          </div>
        </div>
      )}
    </Drawer>
  );
};

export default VersionDetailDrawer;
