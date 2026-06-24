/**
 * ========================================
 * 运行管理页面
 * ========================================
 *
 * 仅承载页面标题与 Tabs 容器，具体内容拆分到子组件中：
 * - 订阅群：SubscribeTab
 * - 连接流：FlowRunTab
 */
import React, { useState } from 'react';
import { Tabs } from 'antd';
import SubscribeTab from './components/SubscribeTab';
import FlowRunTab from './components/FlowRunTab';
import { TAB_KEYS } from './constants';
import './RunManagement.m.less';

const { TabPane } = Tabs;

function RunManagement() {
  // 当前选中的 Tab，默认订阅群
  const [activeTab, setActiveTab] = useState(TAB_KEYS.SUBSCRIBE);

  return (
    <div className="run-management-page">
      <div className="content-card">
        {/* 页面标题 */}
        <div className="page-header">
          <div>
            <h2>运行管理</h2>
          </div>
        </div>

        {/* Tabs：使用 TabPane 子节点形式渲染两个 Tab */}
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane tab="订阅群" key={TAB_KEYS.SUBSCRIBE}>
            <SubscribeTab />
          </TabPane>
          <TabPane tab="连接流" key={TAB_KEYS.FLOW_RUN}>
            <FlowRunTab />
          </TabPane>
        </Tabs>
      </div>
    </div>
  );
}

export default RunManagement;
