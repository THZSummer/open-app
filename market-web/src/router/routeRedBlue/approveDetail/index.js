import React from 'react';
import AppChatbotBindTab from './components/chatbot-bindtab';
import less from './index.module.less';

/**
 * 机器人绑定 Tab 测试页面
 *
 * <p>用于独立测试 chatbot-bindtab 组件，无需依赖外部应用详情页</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
const TestChatbotBindTab = () => {
  // 使用已存在的测试应用 ID
  const testAppId = 'app_page_test_001';

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ marginBottom: '16px', padding: '12px', background: '#f0f5ff', borderRadius: '6px' }}>
        <strong>测试页面</strong> — 应用 ID: <code>{testAppId}</code>
      </div>
      <AppChatbotBindTab appId={testAppId} />
    </div>
  );
};

export default TestChatbotBindTab;
