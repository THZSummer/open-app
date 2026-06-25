/**
 * 连接流编排配置超时时间测试
 * 验证连接器节点超时时间按毫秒直存直取
 */
import {
  buildOrchestrationConfigForTest,
  transformOrchestrationConfigToFlowDataForTest,
} from '../thunk';

/**
 * 获取测试编排配置中的第一个连接器节点数据
 * @param {Object} orchestrationConfig 编排配置
 * @returns {Object} 连接器节点 data
 */
const getFirstConnectorData = (orchestrationConfig) => {
  // orchestrationConfig.nodes: 编排节点列表
  return orchestrationConfig.nodes.find(node => node.type === 'connector').data;
};

describe('FlowEditorV2 连接器超时时间', () => {
  /**
   * 连接器节点超时时间使用毫秒直存直取
   */
  test('连接器节点超时时间按毫秒保存和回显', () => {
    const config = buildOrchestrationConfigForTest({
      flowMode: 'single',
      trigger: { id: 'trigger', type: 'trigger', triggerType: 'http', systokens: [], inputParams: {} },
      steps: [
        {
          id: 'connector-1',
          type: 'connector',
          connectorId: 'connector-id',
          versionId: 'version-id',
          timeout: 300000,
          connectorVersionConfig: {
            protocol: 'HTTP',
            protocolConfig: {
              url: 'https://api.example.com/user/detail',
              method: 'POST',
            },
          },
          inputMappings: {},
        },
      ],
      output: { id: 'output', type: 'output', assembleParams: { header: [], body: [] } },
    });

    const connectorData = getFirstConnectorData(config);
    const flowData = transformOrchestrationConfigToFlowDataForTest(config);

    expect(connectorData.timeoutMs).toBe(300000);
    expect(flowData.steps[0].timeout).toBe(300000);
  });
});
