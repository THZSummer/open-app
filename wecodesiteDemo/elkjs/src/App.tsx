import FlowCanvas from './components/FlowCanvas';
import NodePanel from './components/NodePanel';
import './App.css';

/**
 * 主应用组件
 * 布局：左侧节点面板 + 右侧流程画布
 */
function App() {
  return (
    <div className="app">
      {/* 左侧节点面板 */}
      <NodePanel />

      {/* 右侧流程画布 */}
      <FlowCanvas />
    </div>
  );
}

export default App;
