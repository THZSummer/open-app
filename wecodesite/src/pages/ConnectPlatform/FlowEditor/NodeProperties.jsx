/**
 * ========================================
 * 节点属性配置面板组件（增强版）
 * ========================================
 *
 * 功能：
 * - 显示选中节点的配置表单
 * - 根据节点类型显示不同的配置选项
 * - 支持更丰富的配置项
 */

import React, { useEffect, useState } from 'react';
import {
  Form,
  Input,
  Select,
  InputNumber,
  Switch,
  Divider,
  Empty,
  Card,
  Typography,
  Button,
  Popconfirm,
  Tag,
  message,
} from 'antd';
import { useForm } from 'antd/es/form/Form';
import { NODE_TYPE_META, SCHEMA_EDITOR_CONFIG, NODE_TRANSFORM_CONFIG } from './constants';
import SchemaEditor from '../../../components/SchemaEditor/SchemaEditor.jsx';
import { getUpstreamParams, transformInputMappingFromNested, transformOutputMappingFromNested } from '../../../utils/flowUtils';
import { fetchConnectorList } from '../Connector/thunk';
import { fetchConnectorConfig, transformConnectorConfigToInputMapping } from '../ConnectorEditor/thunk';
import { extractUpdatedFields } from './thunk';
import './NodeProperties.m.less';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

/**
 * 节点属性配置面板组件（增强版）
 *
 * @param {Object} props
 * @param {Object|null} props.selectedNode - 当前选中的节点
 * @param {Function} props.onUpdateNode - 更新节点回调
 * @param {Array} props.nodes - 节点列表
 * @param {Array} props.edges - 连线列表
 */
function NodeProperties({ selectedNode, onUpdateNode, nodes = [], edges = [] }) {
  const [form] = useForm();
  const [connectors, setConnectors] = useState([]);
  const [connectorConfig, setConnectorConfig] = useState(null);
  const [flowInputSchema, setFlowInputSchema] = useState([]);
  const [transformedInputMapping, setTransformedInputMapping] = useState([]);
  const [transformedOutputMapping, setTransformedOutputMapping] = useState([]);

  /** stateKey到setter函数的映射 */
  const stateSetters = {
    flowInputSchema: setFlowInputSchema,
    transformedInputMapping: setTransformedInputMapping,
    transformedOutputMapping: setTransformedOutputMapping,
  };

  /** 转换函数映射 */
  const transformFuncs = {
    transformInputMappingFromNested,
    transformOutputMappingFromNested,
  };

  /**
   * 获取连接器列表
   */
  const loadConnectors = async () => {
    const result = await fetchConnectorList({});
    if (result && result.code === '200') {
      setConnectors(result.data || []);
    } else {
      message.error(result?.messageZh || '加载连接器列表失败');
    }
  };

  /**
   * 获取连接器配置
   */
  const loadConnectorConfig = async (connectorId, currentNode) => {
    if (!connectorId) {
      setConnectorConfig(null);
      return;
    }

    const config = await fetchConnectorConfig(connectorId);

    if (config && config.code === '200') {
      let parsedConnectionConfig = {};

      if (config.data?.connectionConfig) {
        try {
          parsedConnectionConfig = typeof config.data.connectionConfig === 'string'
            ? JSON.parse(config.data.connectionConfig)
            : config.data.connectionConfig;
        } catch (err) {
          message.error('解析连接器配置失败');
          setConnectorConfig(null);
          return;
        }
      }

      setConnectorConfig(parsedConnectionConfig);

      const updatedFields = extractUpdatedFields({
        parsedConnectionConfig,
        currentNode,
      });

      // 如果有需要更新的字段，统一调用一次 onUpdateNode
      if (Object.keys(updatedFields).length > 0) {
        onUpdateNode({
          ...currentNode,
          data: {
            ...currentNode.data,
            ...updatedFields,
          },
        });
      }
    } else {
      message.error(config?.messageZh || '获取连接器配置失败');
      setConnectorConfig(null);
    }
  };

  /**
   * 处理映射数据转换
   * @param {*} mapping - 映射数据
   * @param {Function} transformFunc - 转换函数
   * @returns {Array} 处理后的数组
   */
  const processMapping = (mapping, transformFunc) => {
    if (!mapping) return [];
    if (Array.isArray(mapping)) return mapping;
    return transformFunc(mapping);
  };

  /**
   * 处理连接器切换
   * 切换连接器时清空旧的inputMapping和outputParams，重新根据新连接器生成
   *
   * @param {string} connectorId - 新选择的连接器ID
   * @param {Object} selectedNode - 当前选中的节点
   * @param {Object} data - 节点数据
   * @param {Array} connectors - 连接器列表
   * @param {Function} onUpdateNode - 更新节点回调
   * @param {Function} setConnectorConfig - 设置连接器配置的回调
   */
  const handleConnectorChange = async (
    connectorId,
    selectedNode,
    data,
    connectors,
    onUpdateNode,
    setConnectorConfig
  ) => {
    const connector = connectors.find(c => c.id === connectorId);

    /** 切换连接器时，需要清空旧的inputMapping和outputParams，重新根据新连接器生成 */
    const updatedData = {
      connectorVersionId: connectorId,
      connector: connector || null,
    };

    /** 清空当前的connectorConfig，触发重新加载 */
    setConnectorConfig(null);

    /** 先更新节点数据，清空旧的mapping配置 */
    onUpdateNode({
      ...selectedNode,
      data: {
        ...data,
        ...updatedData,
        inputMapping: null,
        outputParams: null,
      },
    });

    /** 手动加载新的connector配置 */
    if (connectorId) {
      const config = await fetchConnectorConfig(connectorId);

      if (config && config.code === '200') {
        let parsedConnectionConfig = {};

        /** 优先从connectionConfig字段获取配置 */
        if (config.data?.connectionConfig) {
          try {
            parsedConnectionConfig = typeof config.data.connectionConfig === 'string'
              ? JSON.parse(config.data.connectionConfig)
              : config.data.connectionConfig;
          } catch (err) {
            message.error('解析连接器配置失败');
            setConnectorConfig(null);
            return;
          }
        }

        setConnectorConfig(parsedConnectionConfig);

        /** 从连接器配置生成新的inputMapping */
        if (parsedConnectionConfig.inputContract) {
          const inputMapping = transformConnectorConfigToInputMapping(parsedConnectionConfig);
          const outputParams = parsedConnectionConfig.outputContract || [];

          /** 更新节点，添加新的inputMapping和outputParams */
          onUpdateNode({
            ...selectedNode,
            data: {
              ...data,
              ...updatedData,
              inputMapping,
              outputParams,
            },
          });
        }
      } else {
        const errorMsg = config?.messageZh || '获取连接器配置失败';
        message.error(errorMsg);
        setConnectorConfig(null);
      }
    }
  };

  /**
   * 当 selectedNode 是 connector 类型时获取连接器列表和配置
   */
  useEffect(() => {
    if (!selectedNode || selectedNode.type !== 'connector') {
      return;
    }

    const connectorId = selectedNode.data.connectorVersionId || selectedNode.data.config?.connectorId;

    loadConnectors();
    loadConnectorConfig(connectorId, selectedNode);
  }, [selectedNode]);

  /**
   * 当 selectedNode 变化时，初始化 Form 数据
   */
  useEffect(() => {
    if (!selectedNode || !selectedNode.data) {
      return;
    }

    const nodeType = selectedNode.type;
    const transformConfig = NODE_TRANSFORM_CONFIG[nodeType];

    if (!transformConfig) {
      return;
    }

    const nodeData = selectedNode.data;
    const fieldValue = nodeData[transformConfig.fieldName];
    const processedData = processMapping(fieldValue, transformFuncs[transformConfig.transformFuncName]);

    /** 更新对应的状态 */
    stateSetters[transformConfig.stateKey](processedData);

    /** 更新表单数据 */
    form.setFieldsValue({
      [transformConfig.apiConfigKey]: processedData,
    });
  }, [selectedNode, form]);

  // 如果没有选中节点，显示提示
  if (!selectedNode) {
    return (
      <div className="node-properties-panel empty">
        <Empty
          description={
            <Text type="secondary">
              选择节点进行配置
            </Text>
          }
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </div>
    );
  }

  const { type, data, id } = selectedNode;
  const nodeMeta = NODE_TYPE_META[type] || { name: type, color: '#999' };
  const isTriggerNode = selectedNode.type === 'trigger';

  /**
   * 处理字段变化
   * @param {string} configField - 字段名
   * @param {*} value - 字段值
   */
  const handleConfigChange = (configField, value) => {
    const updatedData = { ...data };

    if (configField === 'connectorId') {
      updatedData.connectorVersionId = value;
    } else {
      updatedData[configField] = value;
    }

    onUpdateNode({
      ...selectedNode,
      data: updatedData,
    });
  };

  /**
   * 处理节点删除
   */
  const handleDeleteNode = () => {
    onUpdateNode({
      ...selectedNode,
      _delete: true,
    });
  };

  /**
   * 渲染触发器节点配置
   * - 触发类型目前仅提供HTTP选项，但需要用户手动选择
   * - 包含认证入参配置和连接流入参配置两部分
   */
  const renderTriggerConfig = () => {
    const upstreamParams = getUpstreamParams(id, nodes, edges);

    return (
      <>
        <Form.Item
          label="触发类型"
        >
          <Select
            value={data.type}
            onChange={(val) => handleConfigChange('type', val)}
            placeholder="请选择触发类型"
          >
            <Select.Option value="http">HTTP</Select.Option>
          </Select>
          <Text type="secondary" className="help-text" style={{ marginTop: 4 }}>
            当前仅支持HTTP触发方式
          </Text>
        </Form.Item>

        <Divider style={{ margin: '12px 0' }}>连接流入参配置</Divider>

        <Text type="secondary" className="help-text" style={{ marginBottom: 12 }}>
          配置连接流的输入参数，供后续节点引用
        </Text>

        <SchemaEditor
          form={form}
          {...SCHEMA_EDITOR_CONFIG.inputContract}
          upstreamParams={upstreamParams}
          value={flowInputSchema}
          onChange={(newSchema) => {
            handleConfigChange('inputContract', newSchema || []);
          }}
        />
      </>)
  };

  /**
   * 渲染连接器节点配置
   * - 入参配置展示Carrier（header、body、query）
   * - 只验证连接器选择，不验证执行动作
   */
  const renderActionConfig = () => {
    const upstreamParams = getUpstreamParams(id, nodes, edges);

    const connectorId = data.connectorVersionId || data.config?.connectorId;

    return (
      <>
        <Form.Item
          label="选择连接器"
        >
          <Select
            value={connectorId}
            onChange={(val) => handleConnectorChange(
              val,
              selectedNode,
              data,
              connectors,
              onUpdateNode,
              setConnectorConfig
            )}
            placeholder="请选择连接器"
          >
            {connectors.map(connector => (
              <Select.Option key={connector.id} value={connector.id}>
                {connector.nameCn}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        {connectorConfig && (
          <>
            <Divider style={{ margin: '12px 0' }}>入参配置</Divider>

            <Text type="secondary" className="help-text" style={{ marginBottom: 12 }}>
              配置调用连接器时传入的参数，可使用上游节点的输出参数
            </Text>

            <SchemaEditor
              form={form}
              {...SCHEMA_EDITOR_CONFIG.inputMapping}
              upstreamParams={upstreamParams}
              value={transformedInputMapping}
              onChange={(newSchema) => {
                handleConfigChange('inputMapping', newSchema || []);
                setTransformedInputMapping(newSchema || []);
              }}
            />
          </>
        )}

        {!connectorConfig && connectorId && (
          <Text type="secondary" className="secondary-text">
            正在加载连接器配置...
          </Text>
        )}

        {!connectorId && (
          <Text type="secondary" className="secondary-text">
            请先选择连接器
          </Text>
        )}
      </>
    );
  };

  /**
   * 渲染数据输出节点配置
   * - 移除超时时间配置
   * - 数据组件展示Carrier（header、body、query）
   */
  const renderDataOutputConfig = () => {
    const upstreamParams = getUpstreamParams(id, nodes, edges);

    return (
      <>
        <Divider style={{ margin: '12px 0' }}>数据组装</Divider>

        <Text type="secondary" className="help-text" style={{ marginBottom: 12 }}>
          当前数据输出节点到触发器连线上各节点的数据将作为本节点的输入数据
        </Text>

        <SchemaEditor
          form={form}
          {...SCHEMA_EDITOR_CONFIG.outputMapping}
          upstreamParams={upstreamParams}
          value={transformedOutputMapping}
          onChange={(newSchema) => {
            handleConfigChange('outputMapping', newSchema || []);
            setTransformedOutputMapping(newSchema || []);
          }}
        />
      </>
    );
  };

  /**
   * 根据节点类型渲染配置表单
   */
  const renderNodeConfig = () => {
    switch (type) {
      case 'trigger':
        return renderTriggerConfig();
      case 'connector':
        return renderActionConfig();
      case 'exit':
        return renderDataOutputConfig();
      default:
        return <Text type="secondary">暂不支持此节点类型的配置</Text>;
    }
  };

  return (
    <div className="node-properties-panel content">
      {/* 节点信息卡片 */}
      <div className="node-info-card">
        <div className="node-info-content">
          <div
            className="node-icon"
            style={{
              backgroundColor: nodeMeta.color,
              boxShadow: `0 2px 8px ${nodeMeta.color}40`,
            }}
          >
            {(data.labelCn)?.charAt(0) || type.charAt(0)}
          </div>
          <div className="node-info-text">
            <Text strong className="node-name">{data.labelCne}</Text>
            <div className="node-id">
              ID: {id}
            </div>
          </div>
        </div>
      </div>

      {/* 表单内容 */}
      <div className="form-content">
        <Form layout="vertical" size="small">
          <Form.Item
            label={<span className="form-label">节点名称</span>}
            className="form-item"
          >
            <Input
              value={data.labelCn}
              onChange={(e) => handleConfigChange('labelCn', e.target.value)}
              placeholder="请输入节点名称"
            />
          </Form.Item>

          {(type === 'trigger' || type === 'connector' || type === 'action' || type === 'exit' || type === 'dataOutput' || type === 'data_processor') && (
            <Form.Item
              label={<span className="form-label">节点英文名称</span>}
              className="form-item"
            >
              <Input
                value={data.labelEn}
                onChange={(e) => handleConfigChange('labelEn', e.target.value)}
                placeholder="请输入节点英文名称"
              />
            </Form.Item>
          )}

          <Divider style={{ margin: '16px 0', borderColor: '#f0f0f0' }} />

          {/* 节点类型配置 */}
          {renderNodeConfig()}
        </Form>
      </div>

      {/* 删除按钮区域（触发器节点不可删除） */}
      {!isTriggerNode && (
        <div className="delete-area">
          <Popconfirm
            title="确认删除"
            description="确定要删除此节点吗？此操作不可撤销"
            onConfirm={handleDeleteNode}
            okText="删除"
            cancelText="取消"
            okButtonProps={{ danger: true }}
          >
            <Button type="text" danger block className="delete-btn">
              删除节点
            </Button>
          </Popconfirm>
        </div>
      )}
    </div>
  );
}

export default NodeProperties;
