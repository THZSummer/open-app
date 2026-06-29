/**
 * ========================================
 * 运行管理 - 运行详情抽屉
 * ========================================
 *
 * 字段命名对齐 plan-api.md §3.7 #50 响应：
 *   - executionId / flowNameCn / flowVersionNumber / triggerTime / status / durationMs
 *   - steps[]：nodeId / nodeType / nodeLabelCn / status / durationMs / inputData / outputData / errorMessage
 */
import React, { useEffect, useState } from 'react';
import { Drawer, Spin, Empty } from 'antd';
import ExecutionTraceViewer from '../../../components/ExecutionTraceViewer/ExecutionTraceViewer';
import { fetchFlowRunDetail } from '../thunk';
import {
  EXECUTION_STATUS,
  EXECUTION_STATUS_MAP,
  TRIGGER_TYPE_MAP,
} from '../../../utils/constants';

/**
 * 根据执行状态返回 Hero 区色调 class。
 *
 * @param {number} status 执行状态枚举值
 * @returns {string} class 名称
 */
const getHeroClass = (status) => {
  if (status === EXECUTION_STATUS.SUCCESS) return 'hero-success';
  if (status === EXECUTION_STATUS.FAILED || status === EXECUTION_STATUS.TIMEOUT) return 'hero-error';
  return '';
};

/**
 * 根据执行状态返回状态胶囊 class。
 *
 * @param {number} status 执行状态枚举值
 * @returns {string} class 名称
 */
const getPillClass = (status) => {
  if (status === EXECUTION_STATUS.SUCCESS) return 'pill-success';
  if (status === EXECUTION_STATUS.FAILED || status === EXECUTION_STATUS.TIMEOUT) return 'pill-error';
  return '';
};

/**
 * 渲染 KV 单行。
 *
 * @param {Object} params 参数对象
 * @returns {React.ReactNode} KV 单行
 */
const KvRow = (params) => {
  const { label, value } = params;
  return (
    <>
      <div className="kv-label">{label}</div>
      <div className="kv-value">{value || '-'}</div>
    </>
  );
};

/**
 * 渲染执行耗时。
 *
 * @param {number} durationMs 执行耗时
 * @returns {string} 展示文案
 */
const formatDuration = (durationMs) => {
  if (durationMs == null) return '-';
  return `${durationMs} ms`;
};

/**
 * 连接流执行详情抽屉。
 *
 * @param {Object} props 组件属性
 * @returns {React.ReactNode} 运行详情抽屉
 */
function RunDetailDrawer(props) {
  const { open, executionId, onClose } = props;
  // 加载状态。
  const [loading, setLoading] = useState(false);
  // 详情数据（含 base + steps）。
  const [detail, setDetail] = useState(null);

  // 抽屉打开拉取详情，关闭时清空详情。
  useEffect(() => {
    if (open && executionId) {
      loadDetail();
    }
    if (!open) {
      setDetail(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, executionId]);

  /**
   * 加载运行详情。
   */
  const loadDetail = async () => {
    setLoading(true);
    try {
      const res = await fetchFlowRunDetail({ executionId });
      setDetail(res);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 渲染抽屉内容。
   *
   * @returns {React.ReactNode} 抽屉内容
   */
  const renderContent = () => {
    if (!detail || !detail.base) return <Empty description="暂无数据" />;
    const { base, steps = [] } = detail;
    const stepCount = steps.length || 0;
    // 节点展示顺序：接口数组后面的节点优先展示。
    const displaySteps = steps.slice().reverse();
    // 执行状态文案。
    const statusConfig = EXECUTION_STATUS_MAP[base.status] || { text: '-' };
    // 触发方式文案。
    const triggerTypeConfig = TRIGGER_TYPE_MAP[base.triggerType] || { text: '-' };

    return (
      <>
        {/* Hero 区：状态胶囊 + 副信息 */}
        <div className={`drawer-hero ${getHeroClass(base.status)}`}>
          <div className="drawer-hero-title">
            <span className={`status-pill ${getPillClass(base.status)}`}>{statusConfig.text}</span>
          </div>
          <div className="drawer-hero-sub">
            <span className="mono">{base.executionId || base.id}</span>
            触发于 {base.triggerTime}
          </div>
        </div>

        {/* 基础信息卡片 */}
        <div className="drawer-section">
          <div className="section-title">基础信息</div>
          <div className="kv-grid">
            <KvRow label="执行ID" value={base.id} />
            <KvRow label="连接流名称" value={base.flowNameCn} />
            <KvRow label="版本号" value={`v${base.flowVersionNumber}`} />
            <KvRow label="触发时间" value={base.triggerTime} />
            <KvRow label="触发方式" value={triggerTypeConfig.text} />
            <KvRow label="触发账号" value={base.triggerAccount} />
            <KvRow label="执行耗时" value={formatDuration(base.durationMs)} />
            <KvRow
              label="执行状态"
              value={
                <span className={`status-pill ${getPillClass(base.status)}`}>{statusConfig.text}</span>
              }
            />
            {base.errorMessage && (
              <KvRow label="错误信息" value={base.errorMessage} />
            )}
          </div>
        </div>

        {/* 节点执行时间线 */}
        <div className="drawer-section drawer-section-trace">
          <div className="section-title">
            节点执行时间线
            <span className="section-title-extra">共 {stepCount} 个节点</span>
          </div>
          <ExecutionTraceViewer steps={displaySteps} />
        </div>
      </>
    );
  };

  return (
    <Drawer
      title="运行详情"
      width={760}
      open={open}
      onClose={onClose}
      destroyOnClose
      className="run-detail-drawer"
    >
      <Spin spinning={loading}>{renderContent()}</Spin>
    </Drawer>
  );
}

export default RunDetailDrawer;
