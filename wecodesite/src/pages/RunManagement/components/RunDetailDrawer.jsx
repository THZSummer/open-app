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
import { ClockCircleOutlined } from '@ant-design/icons';
import { fetchFlowRunDetail } from '../thunk';
import {
  EXECUTION_STATUS,
  EXECUTION_STATUS_MAP,
  TRIGGER_TYPE_MAP,
} from '../../../utils/constants';

/**
 * 步骤状态枚举（plan-api.md §1.8.7b）
 * 0=success / 1=failed / 2=timeout / 3=not_executed
 */
const STEP_STATUS_MAP = {
  0: { text: '成功', color: 'green', pillClass: 'pill-success' },
  1: { text: '失败', color: 'red', pillClass: 'pill-error' },
  2: { text: '超时', color: 'orange', pillClass: 'pill-error' },
  3: { text: '未执行', color: 'default', pillClass: '' },
};

/**
 * 根据执行状态返回 Hero 区色调 class
 *
 * @param {number} status - 执行状态枚举值
 * @returns {string} class 名称
 */
const getHeroClass = (status) => {
  if (status === EXECUTION_STATUS.SUCCESS) return 'hero-success';
  if (status === EXECUTION_STATUS.FAILED || status === EXECUTION_STATUS.TIMEOUT) return 'hero-error';
  return '';
};

/**
 * 根据执行状态返回状态胶囊 class
 *
 * @param {number} status - 执行状态枚举值
 * @returns {string} class 名称
 */
const getPillClass = (status) => {
  if (status === EXECUTION_STATUS.SUCCESS) return 'pill-success';
  if (status === EXECUTION_STATUS.FAILED || status === EXECUTION_STATUS.TIMEOUT) return 'pill-error';
  return '';
};

/**
 * 渲染 KV 单行
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - label: 字段标签
 * - value: 字段值（可为 ReactNode）
 *
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
 * 渲染执行耗时（毫秒 → "xx ms"）
 *
 * @param {number} durationMs - 执行耗时（毫秒）
 * @returns {string} 展示文案
 */
const formatDuration = (durationMs) => {
  if (durationMs == null) return '-';
  return `${durationMs} ms`;
};

/**
 * 渲染 JSON 数据（保持兼容字符串与对象两种入参）
 *
 * @param {any} data - 输入/输出数据
 * @returns {string} 展示用的字符串
 */
const formatJson = (data) => {
  if (data == null) return '-';
  if (typeof data === 'string') return data;
  try {
    return JSON.stringify(data, null, 2);
  } catch (err) {
    return String(data);
  }
};

/**
 * 连接流执行详情抽屉
 *
 * @param {Object} props - 组件属性
 * 包含以下字段：
 * - open: 是否显示抽屉
 * - executionId: 执行 ID
 * - onClose: 关闭抽屉回调
 */
function RunDetailDrawer(props) {
  const { open, executionId, onClose } = props;
  // 加载状态
  const [loading, setLoading] = useState(false);
  // 详情数据（含 base + steps）
  const [detail, setDetail] = useState(null);

  // 抽屉打开拉取详情；关闭清空
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
   * 加载运行详情
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

  // 渲染抽屉内容
  const renderContent = () => {
    if (!detail || !detail.base) return <Empty description="暂无数据" />;
    const { base, steps } = detail;
    const stepCount = steps?.length || 0;

    // 执行状态文案
    const statusConfig = EXECUTION_STATUS_MAP[base.status] || { text: '-' };
    // 触发方式文案
    const triggerTypeConfig = TRIGGER_TYPE_MAP[base.triggerType] || { text: '-' };

    return (
      <>
        {/* Hero 区：状态胶囊 + 副信息 */}
        <div className={`drawer-hero ${getHeroClass(base.status)}`}>
          <div className="drawer-hero-title">
            <span className={`status-pill ${getPillClass(base.status)}`}>{statusConfig.text}</span>
          </div>
          <div className="drawer-hero-sub">
            <span className="mono">{base.executionId}</span>
            触发于 {base.triggerTime}
          </div>
        </div>

        {/* 基础信息卡片 */}
        <div className="drawer-section">
          <div className="section-title">基础信息</div>
          <div className="kv-grid">
            <KvRow label="执行ID" value={base.executionId} />
            <KvRow label="连接流名称" value={base.flowNameCn} />
            <KvRow label="版本号" value={base.flowVersionNumber} />
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
        <div className="drawer-section">
          <div className="section-title">
            节点执行时间线
            <span className="section-title-extra">共 {stepCount} 个节点</span>
          </div>
          <div className="timeline">
            {steps.map((step, idx) => {
              // 步骤状态枚举映射
              const stepConfig = STEP_STATUS_MAP[step.status] || { text: '-', pillClass: '' };
              const isFailed = step.status === 1 || step.status === 2;
              const itemClass = step.status === 0 ? 'timeline-success' : 'timeline-fail';
              return (
                <div key={step.nodeId || idx} className={`timeline-item ${itemClass}`}>
                  <div className="timeline-card">
                    <div className="timeline-head">
                      <span className="timeline-name">{step.nodeLabelCn || step.nodeId}</span>
                      <span className={`status-pill ${stepConfig.pillClass}`}>
                        {stepConfig.text}
                      </span>
                      <span className="timeline-duration">
                        <ClockCircleOutlined />
                        {formatDuration(step.durationMs)}
                      </span>
                    </div>
                    <div className="timeline-io">
                      <pre className="code-panel" data-label="INPUT">{formatJson(step.inputData)}</pre>
                      <pre
                        className={`code-panel ${isFailed ? 'code-panel-error' : ''}`}
                        data-label={isFailed ? 'ERROR' : 'OUTPUT'}
                      >
                        {isFailed ? (step.errorMessage || formatJson(step.outputData)) : formatJson(step.outputData)}
                      </pre>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </>
    );
  };

  return (
    <Drawer
      title="运行详情"
      width={640}
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
