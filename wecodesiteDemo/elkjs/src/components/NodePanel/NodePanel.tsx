import './NodePanel.css';

/**
 * 节点面板组件
 * 提供使用说明和提示
 */
const NodePanel = () => {
  return (
    <div className="node-panel">
      <div className="panel-header">
        <h2>📋 使用说明</h2>
        <p className="panel-subtitle">点击连线添加节点</p>
      </div>

      <div className="node-list">
        <div className="usage-item">
          <div className="usage-icon">🔗</div>
          <div className="usage-content">
            <div className="usage-title">连线操作</div>
            <div className="usage-desc">从节点底部拖拽到其他节点顶部创建连线</div>
          </div>
        </div>

        <div className="usage-item">
          <div className="usage-icon">➕</div>
          <div className="usage-content">
            <div className="usage-title">插入节点</div>
            <div className="usage-desc">点击连线上的加号按钮，选择节点类型插入</div>
          </div>
        </div>

        <div className="usage-item">
          <div className="usage-icon">🎯</div>
          <div className="usage-content">
            <div className="usage-title">自动布局</div>
            <div className="usage-desc">插入节点后自动重新排列布局</div>
          </div>
        </div>
      </div>

      <div className="panel-footer">
        <div className="usage-tip">
          💡 提示：初始包含触发器和结束节点，通过点击连线添加中间节点
        </div>
      </div>
    </div>
  );
};

export default NodePanel;
