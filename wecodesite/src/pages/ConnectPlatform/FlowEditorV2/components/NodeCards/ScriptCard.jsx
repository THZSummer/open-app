import React, { useState, useEffect, useRef } from 'react';
import Editor from '@monaco-editor/react';
import SchemaEditorV2 from '../../../../../components/SchemaEditor/SchemaEditorV2';
import './NodeCards.m.less';

/**
 * 脚本处理节点卡片
 *
 * @param {Object} props
 *   props.node      脚本节点数据
 *   props.editable  是否可编辑
 *   props.flowData  整个连接流数据（保留接口一致性）
 *   props.appLimits 应用级上限（保留接口一致性）
 *   props.onChange  (updatedNode) => void
 */
const ScriptCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, onChange } = props;

  // Monaco Editor 加载状态：true=正常加载中/已加载, false=加载失败
  const [monacoReady, setMonacoReady] = useState(true);
  // Monaco 初始化是否完成（用于 loading 态判断）
  const [monacoLoading, setMonacoLoading] = useState(true);
  // 编辑器是否已挂载，用于决定是否在首次渲染前显示 fallback
  const editorMounted = useRef(false);

  /**
   * 检查 Monaco loader 是否可用
   */
  useEffect(() => {
    let cancelled = false;
    // 动态 import 并尝试初始化 Monaco loader
    import('@monaco-editor/react')
      .then((mod) => {
        if (cancelled) return;
        // 如果模块加载成功但 loader.init 失败，则标记回退
        return mod.loader.init().catch(() => {
          if (!cancelled) {
            setMonacoReady(false);
            setMonacoLoading(false);
          }
        });
      })
      .then(() => {
        if (!cancelled) {
          setMonacoLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setMonacoReady(false);
          setMonacoLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  /**
   * Monaco Editor 挂载完成回调
   */
  const handleEditorMount = () => {
    editorMounted.current = true;
  };

  /**
   * 更新脚本内容
   * @param {string} value 新的脚本内容
   */
  const handleScriptChange = (value) => {
    onChange({ ...node, script: value });
  };

  /**
   * 出参 schema 变更
   * @param {Array} value SchemaEditorV2 回传的最新数组
   */
  const handleOutputParamsChange = (value) => {
    onChange({ ...node, outputParams: value || [] });
  };

  /**
   * 渲染 Monaco Editor 或 textarea 回退
   * @returns {JSX.Element}
   */
  const renderEditor = () => {
    // 加载失败：展示 textarea 回退
    if (!monacoReady) {
      return (
        <div>
          <div className="monaco-fallback-tip">
            Monaco Editor 加载失败，请使用备用输入框
          </div>
          <textarea
            className="monaco-fallback-textarea"
            value={node.script || ''}
            disabled={!editable}
            onChange={(e) => handleScriptChange(e.target.value)}
          />
        </div>
      );
    }

    // 加载中
    if (monacoLoading) {
      return <div className="section-desc">编辑器加载中...</div>;
    }

    // 正常渲染 Monaco Editor
    return (
      <div className="monaco-editor-wrap">
        <Editor
          language="typescript"
          theme="vs"
          value={node.script || ''}
          height={320}
          options={{
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            readOnly: !editable,
            fontSize: 13,
            lineNumbers: 'on',
            tabSize: 2,
          }}
          onChange={handleScriptChange}
          onMount={handleEditorMount}
          loading={<div className="section-desc">编辑器加载中...</div>}
        />
      </div>
    );
  };

  return (
    <div>
      {/* 脚本编辑器 */}
      <div className="node-card-section">
        <div className="section-title">脚本内容</div>
        {renderEditor()}
      </div>

      {/* 出参 schema */}
      <div className="node-card-section">
        <div className="section-title">出参 schema</div>
        <div className="section-desc">脚本执行后的输出参数定义</div>
        <SchemaEditorV2
          value={node.outputParams || []}
          editable={editable}
          hideCarrier
          onChange={handleOutputParamsChange}
        />
      </div>
    </div>
  );
};

export default ScriptCard;
