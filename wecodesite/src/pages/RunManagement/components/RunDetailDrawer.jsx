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
import {
  ApiOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  ExportOutlined,
  NodeIndexOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
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
 * 节点类型展示配置
 */
const NODE_TYPE_MAP = {
  1: { text: '触发器', className: 'node-trigger', icon: <ThunderboltOutlined /> },
  2: { text: '连接器', className: 'node-connector', icon: <ApiOutlined /> },
  3: { text: '数据处理', className: 'node-processor', icon: <NodeIndexOutlined /> },
  4: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
  5: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
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
 * 根据节点类型返回展示配置
 *
 * @param {number} nodeType - 节点类型枚举值
 * @returns {Object} 节点类型展示配置
 */
const getNodeTypeConfig = (nodeType) => {
  return NODE_TYPE_MAP[nodeType] || { text: '节点', className: 'node-default', icon: <NodeIndexOutlined /> };
};

/**
 * 根据步骤状态返回时间线项 class
 *
 * @param {number} status - 步骤状态枚举值
 * @returns {string} class 名称
 */
const getStepItemClass = (status) => {
  if (status === 0) return 'timeline-success';
  if (status === 3) return 'timeline-muted';
  return 'timeline-fail';
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
 * 尝试解析 JSON 字符串
 *
 * @param {any} data - 待解析数据
 * @returns {any} 解析后的数据或原始数据
 */
const parseJsonValue = (data) => {
  if (typeof data !== 'string') return data;
  try {
    return JSON.parse(data);
  } catch (err) {
    return data;
  }
};

/**
 * 渲染 JSON 数据（保持兼容字符串与对象两种入参）
 *
 * @param {any} data - 输入/输出数据
 * @returns {string} 展示用的字符串
 */
const formatJson = (data) => {
  if (data == null || data === '') return '-';
  const parsedData = parseJsonValue(data);
  if (typeof parsedData === 'string') return parsedData;
  try {
    return JSON.stringify(parsedData, null, 2);
  } catch (err) {
    return String(parsedData);
  }
};

/**
 * 判断是否只有成功状态输出
 *
 * @param {any} data - 输出数据
 * @returns {boolean} 是否为仅成功状态输出
 */
const isOnlySuccessStatus = (data) => {
  const parsedData = parseJsonValue(data);
  if (!parsedData || typeof parsedData !== 'object' || Array.isArray(parsedData)) return false;
  const keys = Object.keys(parsedData);
  return keys.length === 1 && parsedData.__status === 'success';
};

/**
 * 计算节点耗时条占比
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - durationMs: 当前节点耗时
 * - maxDuration: 最大节点耗时
 *
 * @returns {number} 百分比数值
 */
const getDurationRatio = (params) => {
  const { durationMs, maxDuration } = params;
  if (!durationMs || !maxDuration) return 4;
  return Math.max(4, Math.min(100, Math.round((durationMs / maxDuration) * 100)));
};

/**
 * 渲染 JSON 高亮信息
 *
 * @param {string} text - 格式化后的 JSON 文本
 * @returns {React.ReactNode} 高亮后的节点
 */
const renderHighlightedJson = (text) => {
  if (!text || text === '-') return text;
  const tokenReg = /("(?:\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(?=\s*:))|("(?:\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*")|\b(true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?/g;
  const nodes = [];
  let lastIndex = 0;
  let match;
  let index = 0;

  while ((match = tokenReg.exec(text)) !== null) {
    if (match.index > lastIndex) {
      nodes.push(text.slice(lastIndex, match.index));
    }

    // 根据 token 类型设置高亮 class
    const token = match[0];
    let className = 'json-number';
    if (match[1]) className = 'json-key';
    if (match[2]) className = 'json-string';
    if (match[3]) className = 'json-literal';

    nodes.push(<span key={`${token}-${index}`} className={className}>{token}</span>);
    lastIndex = tokenReg.lastIndex;
    index += 1;
  }

  if (lastIndex < text.length) {
    nodes.push(text.slice(lastIndex));
  }

  return nodes;
};

/**
 * 渲染代码面板
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - label: 面板标题
 * - data: 面板数据
 * - isError: 是否错误面板
 * - copyKey: 复制状态 key
 * - copiedKey: 当前已复制 key
 * - onCopy: 复制回调
 * - showSuccessEmpty: 是否展示成功空态
 *
 * @returns {React.ReactNode} 代码面板
 */
const CodePanel = (params) => {
  const {
    label,
    data,
    isError,
    copyKey,
    copiedKey,
    onCopy,
    showSuccessEmpty,
  } = params;
  const text = formatJson(data);
  const isCopied = copiedKey === copyKey;

  if (showSuccessEmpty && isOnlySuccessStatus(data)) {
    return (
      <div className="code-shell code-shell-empty">
        <div className="code-toolbar">
          <span>{label}</span>
        </div>
        <div className="code-empty-state">
          <CheckOutlined />
          <span>执行成功，无额外输出</span>
        </div>
      </div>
    );
  }

  return (
    <div className={`code-shell ${isError ? 'code-shell-error' : ''}`}>
      <div className="code-toolbar">
        <span>{label}</span>
        <button
          type="button"
          className="code-copy-btn"
          onClick={() => onCopy({ copyKey, text })}
        >
          {isCopied ? <CheckOutlined /> : <CopyOutlined />}
          {isCopied ? '已复制' : '复制'}
        </button>
      </div>
      <pre className="code-panel">{renderHighlightedJson(text)}</pre>
    </div>
  );
};

/**
 * 渲染节点时间线卡片
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - step: 当前节点数据
 * - idx: 节点序号
 * - maxDuration: 最大节点耗时
 * - copiedKey: 当前已复制 key
 * - onCopy: 复制回调
 *
 * @returns {React.ReactNode} 时间线节点卡片
 */
const TimelineNode = (params) => {
  const { step, idx, maxDuration, copiedKey, onCopy } = params;
  // 步骤状态枚举映射
  const stepConfig = STEP_STATUS_MAP[step.status] || { text: '-', pillClass: '' };
  // 节点类型展示配置
  const nodeTypeConfig = getNodeTypeConfig(step.nodeType);
  const isFailed = step.status === 1 || step.status === 2;
  const itemClass = getStepItemClass(step.status);
  const durationRatio = getDurationRatio({ durationMs: step.durationMs, maxDuration });
  const outputData = isFailed ? (step.errorMessage || step.outputData) : step.outputData;

  return (
    <div className={`timeline-item ${itemClass} ${nodeTypeConfig.className}`}>
      <div className="timeline-dot">
        <span className="timeline-dot-icon">{nodeTypeConfig.icon}</span>
      </div>
      <div className="timeline-card">
        <div className="timeline-head">
          <div className="timeline-title-group">
            <div className="timeline-index">#{idx + 1}</div>
            <div>
              <div className="timeline-name">{step.nodeLabelCn || step.nodeId}</div>
              <div className="timeline-meta">
                <span className="node-type-chip">{step.nodeTypeDesc || nodeTypeConfig.text}</span>
                <span className="mono">{step.nodeId}</span>
                {step.iteration > 0 && <span className="iteration-chip">第 {step.iteration} 次迭代</span>}
              </div>
            </div>
          </div>
          <div className="timeline-stat-group">
            <span className={`status-pill ${stepConfig.pillClass}`}>{stepConfig.text}</span>
            <span className="timeline-duration">
              <ClockCircleOutlined />
              {formatDuration(step.durationMs)}
            </span>
          </div>
        </div>
        <div className="duration-meter" aria-hidden="true">
          <span style={{ width: `${durationRatio}%` }} />
        </div>
        <div className="timeline-io">
          <CodePanel
            label="INPUT"
            data={step.inputData}
            copyKey={`${step.id || step.nodeId}-input`}
            copiedKey={copiedKey}
            onCopy={onCopy}
          />
          <CodePanel
            label={isFailed ? 'ERROR' : 'OUTPUT'}
            data={outputData}
            isError={isFailed}
            copyKey={`${step.id || step.nodeId}-output`}
            copiedKey={copiedKey}
            onCopy={onCopy}
            showSuccessEmpty={!isFailed}
          />
        </div>
      </div>
    </div>
  );
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
  // 当前已复制的代码面板 key
  const [copiedKey, setCopiedKey] = useState('');

  // 抽屉打开拉取详情；关闭清空
  useEffect(() => {
    if (open && executionId) {
      loadDetail();
    }
    if (!open) {
      setDetail(null);
      setCopiedKey('');
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

  /**
   * 复制代码面板内容
   *
   * @param {Object} params - 参数对象
   * 包含以下字段：
   * - copyKey: 面板唯一 key
   * - text: 待复制文本
   */
  const handleCopyCode = async (params) => {
    const { copyKey, text } = params;
    if (!text || text === '-') return;
    try {
      await navigator.clipboard.writeText(text);
      setCopiedKey(copyKey);
      window.setTimeout(() => setCopiedKey(''), 1200);
    } catch (err) {
      setCopiedKey('');
    }
  };

  // 渲染抽屉内容
  const renderContent = () => {
    if (!detail || !detail.base) return <Empty description="暂无数据" />;
    const { base, steps = [] } = detail;
    const stepCount = steps.length || 0;
    // 节点展示顺序：接口数组后面的节点优先展示
    const displaySteps = steps.slice().reverse();
    const maxDuration = Math.max(...steps.map((step) => step.durationMs || 0), 0);

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
          <div className="timeline">
            {displaySteps.map((step, idx) => (
              <TimelineNode
                key={step.id || step.nodeId || idx}
                step={step}
                idx={idx}
                maxDuration={maxDuration}
                copiedKey={copiedKey}
                onCopy={handleCopyCode}
              />
            ))}
          </div>
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
