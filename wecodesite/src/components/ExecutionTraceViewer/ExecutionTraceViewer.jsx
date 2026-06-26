/**
 * ========================================
 * 公共执行链路展示组件
 * ========================================
 *
 * 用于展示连接流运行详情与调试结果中的节点执行时间线。
 */
import React, { useState } from 'react';
import {
  ApiOutlined,
  CheckOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  ExportOutlined,
  NodeIndexOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import './ExecutionTraceViewer.m.less';

/**
 * 步骤状态展示配置。
 */
const STEP_STATUS_MAP = {
  success: { text: '成功', pillClass: 'pill-success' },
  failed: { text: '失败', pillClass: 'pill-error' },
  fail: { text: '失败', pillClass: 'pill-error' },
  error: { text: '失败', pillClass: 'pill-error' },
  timeout: { text: '超时', pillClass: 'pill-error' },
  not_executed: { text: '未执行', pillClass: '' },
  0: { text: '成功', pillClass: 'pill-success' },
  1: { text: '失败', pillClass: 'pill-error' },
  2: { text: '超时', pillClass: 'pill-error' },
  3: { text: '未执行', pillClass: '' },
};

/**
 * 节点类型展示配置。
 */
const NODE_TYPE_MAP = {
  trigger: { text: '触发器', className: 'node-trigger', icon: <ThunderboltOutlined /> },
  connector: { text: '连接器', className: 'node-connector', icon: <ApiOutlined /> },
  script: { text: '数据处理', className: 'node-processor', icon: <NodeIndexOutlined /> },
  processor: { text: '数据处理', className: 'node-processor', icon: <NodeIndexOutlined /> },
  output: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
  exit: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
  1: { text: '触发器', className: 'node-trigger', icon: <ThunderboltOutlined /> },
  2: { text: '连接器', className: 'node-connector', icon: <ApiOutlined /> },
  3: { text: '数据处理', className: 'node-processor', icon: <NodeIndexOutlined /> },
  4: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
  5: { text: '出口', className: 'node-exit', icon: <ExportOutlined /> },
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
 * 尝试解析 JSON 字符串。
 *
 * @param {any} data 待解析数据
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
 * 格式化 JSON 数据。
 *
 * @param {any} data 输入或输出数据
 * @returns {string} 展示用字符串
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
 * 判断是否只有成功状态输出。
 *
 * @param {any} data 输出数据
 * @returns {boolean} 是否为仅成功状态输出
 */
const isOnlySuccessStatus = (data) => {
  const parsedData = parseJsonValue(data);
  if (!parsedData || typeof parsedData !== 'object' || Array.isArray(parsedData)) return false;
  const keys = Object.keys(parsedData);
  return keys.length === 1 && parsedData.__status === 'success';
};

/**
 * 获取步骤状态展示配置。
 *
 * @param {string|number} status 步骤状态
 * @returns {Object} 状态展示配置
 */
const getStepStatusConfig = (status) => {
  return STEP_STATUS_MAP[status] || { text: status || '-', pillClass: '' };
};

/**
 * 根据节点类型返回展示配置。
 *
 * @param {string|number} nodeType 节点类型
 * @returns {Object} 节点类型展示配置
 */
const getNodeTypeConfig = (nodeType) => {
  return NODE_TYPE_MAP[nodeType] || { text: '节点', className: 'node-default', icon: <NodeIndexOutlined /> };
};

/**
 * 判断步骤是否失败。
 *
 * @param {string|number} status 步骤状态
 * @returns {boolean} 是否失败
 */
const isFailedStatus = (status) => {
  return status === 'failed' || status === 'fail' || status === 'error' || status === 'timeout' || status === 1 || status === 2;
};

/**
 * 根据步骤状态返回时间线项 class。
 *
 * @param {string|number} status 步骤状态
 * @returns {string} class 名称
 */
const getStepItemClass = (status) => {
  if (status === 'success' || status === 0) return 'timeline-success';
  if (status === 'not_executed' || status === 3) return 'timeline-muted';
  return 'timeline-fail';
};

/**
 * 计算节点耗时条占比。
 *
 * @param {Object} params 参数对象
 * @returns {number} 百分比数值
 */
const getDurationRatio = (params) => {
  const { durationMs, maxDuration } = params;
  if (!durationMs || !maxDuration) return 4;
  return Math.max(4, Math.min(100, Math.round((durationMs / maxDuration) * 100)));
};

/**
 * 渲染 JSON 高亮信息。
 *
 * @param {string} text 格式化后的 JSON 文本
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

    // 根据 token 类型设置高亮 class。
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
 * 渲染代码面板。
 *
 * @param {Object} params 参数对象
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
 * 渲染节点时间线卡片。
 *
 * @param {Object} params 参数对象
 * @returns {React.ReactNode} 时间线节点卡片
 */
const TimelineNode = (params) => {
  const { step, idx, maxDuration, copiedKey, onCopy } = params;
  const stepConfig = getStepStatusConfig(step.status);
  const nodeTypeConfig = getNodeTypeConfig(step.nodeType);
  const failed = isFailedStatus(step.status);
  const itemClass = getStepItemClass(step.status);
  const durationRatio = getDurationRatio({ durationMs: step.durationMs, maxDuration });
  const outputData = failed ? (step.errorMessage || step.outputData) : step.outputData;
  const nodeName = step.nodeLabelCn || step.labelCn || step.nodeLabelEn || step.labelEn || step.nodeId || '-';
  const nodeTypeText = step.nodeTypeDesc || nodeTypeConfig.text;

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
              <div className="timeline-name">{nodeName}</div>
              <div className="timeline-meta">
                <span className="node-type-chip">{nodeTypeText}</span>
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
            label={failed ? 'ERROR' : 'OUTPUT'}
            data={outputData}
            isError={failed}
            copyKey={`${step.id || step.nodeId}-output`}
            copiedKey={copiedKey}
            onCopy={onCopy}
            showSuccessEmpty={!failed}
          />
        </div>
      </div>
    </div>
  );
};

/**
 * 公共执行链路展示组件。
 *
 * @param {Object} props 组件属性
 * @returns {React.ReactNode} 执行链路展示
 */
const ExecutionTraceViewer = (props) => {
  const { steps = [] } = props;
  const [copiedKey, setCopiedKey] = useState('');
  const maxDuration = Math.max(...steps.map((step) => step.durationMs || 0), 0);

  /**
   * 复制代码面板内容。
   *
   * @param {Object} params 参数对象
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

  if (!steps.length) {
    return <div className="execution-trace-empty">暂无节点执行数据</div>;
  }

  return (
    <div className="execution-trace-viewer">
      <div className="timeline">
        {steps.map((step, idx) => (
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
  );
};

export default ExecutionTraceViewer;
