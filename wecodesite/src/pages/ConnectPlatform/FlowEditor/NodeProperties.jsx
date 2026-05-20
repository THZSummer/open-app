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

import React from 'react';
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
  Space,
  Popconfirm,
} from 'antd';
import { NODE_TYPE_META } from './customNodes';

const { Title, Text, Paragraph } = Typography;
const { TextArea } = Input;

/**
 * 节点属性配置面板组件（增强版）
 *
 * @param {Object} props
 * @param {Object|null} props.selectedNode - 当前选中的节点
 * @param {Function} props.onUpdateNode - 更新节点回调
 */
function NodeProperties({ selectedNode, onUpdateNode }) {
  /**
   * 判断是否为触发器节点（触发器节点不可删除）
   */
  const isTriggerNode = selectedNode?.type === 'trigger';
  // 如果没有选中节点，显示提示
  if (!selectedNode) {
    return (
      <div
        className="node-properties-panel"
        style={{
          width: 320,
          height: '100%',
          backgroundColor: '#fafafa',
          borderLeft: '1px solid #e8e8e8',
          padding: 16,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
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

  /**
   * 处理字段变化
   */
  const handleFieldChange = (field, value) => {
    if (!onUpdateNode) return;

    onUpdateNode({
      ...selectedNode,
      data: {
        ...data,
        [field]: value,
      },
    });
  };

  /**
   * 处理配置字段变化
   */
  const handleConfigChange = (configField, value) => {
    if (!onUpdateNode) return;

    onUpdateNode({
      ...selectedNode,
      data: {
        ...data,
        config: {
          ...(data.config || {}),
          [configField]: value,
        },
      },
    });
  };

  /**
   * 处理节点删除
   */
  const handleDeleteNode = () => {
    if (onUpdateNode) {
      onUpdateNode({
        ...selectedNode,
        _delete: true,
      });
    }
  };

  /**
   * 渲染触发器节点配置
   */
  const renderTriggerConfig = () => (
    <>
      <Form.Item label="触发类型">
        <Select
          value={data.config?.triggerType}
          onChange={(val) => handleConfigChange('triggerType', val)}
          placeholder="请选择触发类型"
        >
          <Select.Option value="schedule">定时触发</Select.Option>
          <Select.Option value="webhook">Webhook</Select.Option>
          <Select.Option value="api">API触发</Select.Option>
        </Select>
      </Form.Item>

      {data.config?.triggerType === 'schedule' && (
        <>
          <Form.Item label="Cron表达式">
            <Input
              value={data.config?.cronExpression}
              onChange={(e) => handleConfigChange('cronExpression', e.target.value)}
              placeholder="0 0 * * * ?"
            />
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              格式：秒 分 时 日 月 周（年可选）
            </Text>
          </Form.Item>

          <Form.Item label="时区">
            <Select
              value={data.config?.timezone || 'Asia/Shanghai'}
              onChange={(val) => handleConfigChange('timezone', val)}
            >
              <Select.Option value="Asia/Shanghai">Asia/Shanghai (UTC+8)</Select.Option>
              <Select.Option value="UTC">UTC</Select.Option>
              <Select.Option value="America/New_York">America/New_York (UTC-5)</Select.Option>
              <Select.Option value="Europe/London">Europe/London (UTC+0)</Select.Option>
            </Select>
          </Form.Item>
        </>
      )}

      {data.config?.triggerType === 'webhook' && (
        <>
          <Form.Item label="Webhook路径">
            <Input
              value={data.config?.webhookPath}
              onChange={(e) => handleConfigChange('webhookPath', e.target.value)}
              placeholder="/webhook/xxx"
            />
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              支持GET/POST方法
            </Text>
          </Form.Item>

          <Form.Item label="签名密钥">
            <Input.Password
              value={data.config?.secretKey || ''}
              onChange={(e) => handleConfigChange('secretKey', e.target.value)}
              placeholder="用于签名验证"
            />
          </Form.Item>
        </>
      )}

      {data.config?.triggerType === 'api' && (
        <>
          <Form.Item label="轮询间隔（秒）">
            <InputNumber
              value={data.config?.interval || 60}
              onChange={(val) => handleConfigChange('interval', val)}
              min={10}
              max={3600}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item label="API端点">
            <Input
              value={data.config?.apiEndpoint || ''}
              onChange={(e) => handleConfigChange('apiEndpoint', e.target.value)}
              placeholder="https://api.example.com/polling"
            />
          </Form.Item>
        </>
      )}
    </>
  );

  /**
   * 渲染执行动作节点配置
   */
  const renderActionConfig = () => (
    <>
      <Form.Item label="选择连接器">
        <Select
          value={data.config?.connectorId}
          onChange={async (val) => {
            handleConfigChange('connectorId', val);
            handleConfigChange('actionId', undefined);
          }}
          placeholder="请选择连接器"
          showSearch
          filterOption={(input, option) =>
            option.children.props.children[1].props.children
              .toLowerCase()
              .includes(input.toLowerCase())
          }
        >
          <Select.Option value="mock">模拟连接器</Select.Option>
        </Select>
      </Form.Item>

      {data.config?.connectorId && (
        <>
          <Form.Item label="选择执行动作">
            <Select
              value={data.config?.actionId}
              onChange={(val) => handleConfigChange('actionId', val)}
              placeholder="请选择执行动作"
            >
              <Select.Option value="send_msg">发送消息</Select.Option>
              <Select.Option value="create_record">创建记录</Select.Option>
              <Select.Option value="update_record">更新记录</Select.Option>
              <Select.Option value="delete_record">删除记录</Select.Option>
              <Select.Option value="query_data">查询数据</Select.Option>
            </Select>
          </Form.Item>

          {data.config?.actionId && (
            <>
              <Divider style={{ margin: '12px 0' }}>参数配置</Divider>

              <Form.Item label="输入参数映射">
                <TextArea
                  value={data.config?.inputMapping ? JSON.stringify(data.config.inputMapping, null, 2) : ''}
                  onChange={(e) => {
                    try {
                      const mapping = JSON.parse(e.target.value || '{}');
                      handleConfigChange('inputMapping', mapping);
                    } catch (err) {
                      console.error('JSON格式错误');
                    }
                  }}
                  placeholder={`{"field1": "value1", "field2": "{{node.output.field}}"}`}
                  rows={4}
                  style={{ fontFamily: 'monospace', fontSize: 12 }}
                />
                <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
                  支持使用 {`{{变量}}`} 引用其他节点的输出
                </Text>
              </Form.Item>

              <Form.Item label="超时时间（毫秒）">
                <InputNumber
                  value={data.config?.timeout || 30000}
                  onChange={(val) => handleConfigChange('timeout', val)}
                  min={1000}
                  max={300000}
                  step={1000}
                  style={{ width: '100%' }}
                />
              </Form.Item>

              <Form.Item label="错误处理">
                <Select
                  value={data.config?.errorHandling || 'throw'}
                  onChange={(val) => handleConfigChange('errorHandling', val)}
                >
                  <Select.Option value="throw">抛出错误，中断流程</Select.Option>
                  <Select.Option value="continue">继续执行下一个节点</Select.Option>
                  <Select.Option value="retry">重试（3次）</Select.Option>
                  <Select.Option value="skip">跳过此节点</Select.Option>
                </Select>
              </Form.Item>
            </>
          )}
        </>
      )}
    </>
  );

  /**
   * 渲染条件分支节点配置
   */
  const renderConditionConfig = () => (
    <>
      <Form.Item label="条件组合方式">
        <Select
          value={data.config?.conditionMode || 'and'}
          onChange={(val) => handleConfigChange('conditionMode', val)}
        >
          <Select.Option value="and">所有条件都满足（AND）</Select.Option>
          <Select.Option value="or">任一条件满足（OR）</Select.Option>
        </Select>
      </Form.Item>

      <Divider style={{ margin: '12px 0' }}>条件列表</Divider>

      {(data.config?.conditions || []).map((condition, index) => (
        <Card key={index} size="small" style={{ marginBottom: 12 }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Form.Item label="字段" style={{ marginBottom: 0, flex: 1 }}>
                <Input
                  value={condition.field}
                  onChange={(e) => {
                    const newConditions = [...(data.config?.conditions || [])];
                    newConditions[index] = { ...condition, field: e.target.value };
                    handleConfigChange('conditions', newConditions);
                  }}
                  placeholder="例如: status"
                  size="small"
                />
              </Form.Item>
              <Button
                type="text"
                danger
                size="small"
                onClick={() => {
                  const newConditions = (data.config?.conditions || []).filter((_, i) => i !== index);
                  handleConfigChange('conditions', newConditions);
                }}
              >
                删除
              </Button>
            </div>

            <Form.Item label="操作符" style={{ marginBottom: 0 }}>
              <Select
                value={condition.operator}
                onChange={(val) => {
                  const newConditions = [...(data.config?.conditions || [])];
                  newConditions[index] = { ...condition, operator: val };
                  handleConfigChange('conditions', newConditions);
                }}
                size="small"
                style={{ width: '100%' }}
              >
                <Select.Option value="eq">等于</Select.Option>
                <Select.Option value="neq">不等于</Select.Option>
                <Select.Option value="gt">大于</Select.Option>
                <Select.Option value="gte">大于等于</Select.Option>
                <Select.Option value="lt">小于</Select.Option>
                <Select.Option value="lte">小于等于</Select.Option>
                <Select.Option value="contains">包含</Select.Option>
                <Select.Option value="notContains">不包含</Select.Option>
                <Select.Option value="startsWith">开头是</Select.Option>
                <Select.Option value="endsWith">结尾是</Select.Option>
                <Select.Option value="isEmpty">为空</Select.Option>
                <Select.Option value="isNotEmpty">不为空</Select.Option>
              </Select>
            </Form.Item>

            {!['isEmpty', 'isNotEmpty'].includes(condition.operator) && (
              <Form.Item label="比较值" style={{ marginBottom: 0 }}>
                <Input
                  value={condition.value}
                  onChange={(e) => {
                    const newConditions = [...(data.config?.conditions || [])];
                    newConditions[index] = { ...condition, value: e.target.value };
                    handleConfigChange('conditions', newConditions);
                  }}
                  placeholder="请输入比较值"
                  size="small"
                />
              </Form.Item>
            )}
          </Space>
        </Card>
      ))}

      <Button
        type="dashed"
        block
        onClick={() => {
          const newConditions = [
            ...(data.config?.conditions || []),
            { id: `condition_${Date.now()}`, field: '', operator: 'eq', value: '' }
          ];
          handleConfigChange('conditions', newConditions);
        }}
      >
        + 添加条件
      </Button>

      <Divider style={{ margin: '12px 0' }} />

      <Form.Item label="条件满足时输出">
        <Input
          value={data.config?.trueOutput || ''}
          onChange={(e) => handleConfigChange('trueOutput', e.target.value)}
          placeholder="输出变量名"
        />
      </Form.Item>

      <Form.Item label="条件不满足时输出">
        <Input
          value={data.config?.falseOutput || ''}
          onChange={(e) => handleConfigChange('falseOutput', e.target.value)}
          placeholder="输出变量名"
        />
      </Form.Item>
    </>
  );

  /**
   * 渲染延时节点配置
   */
  const renderDelayConfig = () => (
    <>
      <Form.Item label="延时类型">
        <Select
          value={data.config?.delayType || 'fixed'}
          onChange={(val) => handleConfigChange('delayType', val)}
        >
          <Select.Option value="fixed">固定延时</Select.Option>
          <Select.Option value="dynamic">动态延时</Select.Option>
        </Select>
      </Form.Item>

      {data.config?.delayType === 'fixed' && (
        <>
          <Form.Item label="延时时长（秒）">
            <InputNumber
              value={data.config?.duration || 1}
              onChange={(val) => handleConfigChange('duration', val)}
              min={1}
              max={86400}
              style={{ width: '100%' }}
            />
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              最大值：86400秒（24小时）
            </Text>
          </Form.Item>

          <Form.Item label="快捷设置">
            <Space wrap>
              <Button size="small" onClick={() => handleConfigChange('duration', 5)}>5秒</Button>
              <Button size="small" onClick={() => handleConfigChange('duration', 30)}>30秒</Button>
              <Button size="small" onClick={() => handleConfigChange('duration', 60)}>1分钟</Button>
              <Button size="small" onClick={() => handleConfigChange('duration', 300)}>5分钟</Button>
              <Button size="small" onClick={() => handleConfigChange('duration', 3600)}>1小时</Button>
            </Space>
          </Form.Item>
        </>
      )}

      {data.config?.delayType === 'dynamic' && (
        <Form.Item label="动态延时字段">
          <Input
            value={data.config?.delayField || ''}
            onChange={(e) => handleConfigChange('delayField', e.target.value)}
            placeholder="输入包含延时秒数的变量名"
          />
        </Form.Item>
      )}
    </>
  );

  /**
   * 渲染并行执行节点配置
   */
  const renderParallelConfig = () => (
    <>
      <Form.Item label="等待策略">
        <Select
          value={data.config?.waitStrategy || 'all'}
          onChange={(val) => handleConfigChange('waitStrategy', val)}
        >
          <Select.Option value="all">等待所有分支完成</Select.Option>
          <Select.Option value="any">任一分支完成即继续</Select.Option>
          <Select.Option value="failed">等待失败分支</Select.Option>
        </Select>
      </Form.Item>

      <Form.Item label="超时设置（秒）">
        <InputNumber
          value={data.config?.timeout || 300}
          onChange={(val) => handleConfigChange('timeout', val)}
          min={0}
          style={{ width: '100%' }}
        />
        <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
          0表示不设置超时
        </Text>
      </Form.Item>

      <Divider style={{ margin: '12px 0' }}>分支列表</Divider>

      {(data.config?.branches || []).map((branch, index) => (
        <div key={index} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <Form.Item label={`分支 ${index + 1}`} style={{ marginBottom: 0, flex: 1 }}>
            <Input
              value={branch.name}
              onChange={(e) => {
                const newBranches = [...(data.config?.branches || [])];
                newBranches[index] = { ...branch, name: e.target.value };
                handleConfigChange('branches', newBranches);
              }}
              placeholder="分支名称"
              size="small"
            />
          </Form.Item>
          <Button
            type="text"
            danger
            size="small"
            onClick={() => {
              const newBranches = (data.config?.branches || []).filter((_, i) => i !== index);
              handleConfigChange('branches', newBranches);
            }}
          >
            删除
          </Button>
        </div>
      ))}

      <Button
        type="dashed"
        block
        onClick={() => {
          const newBranches = [
            ...(data.config?.branches || []),
            { id: `branch_${Date.now()}`, name: '' }
          ];
          handleConfigChange('branches', newBranches);
        }}
      >
        + 添加分支
      </Button>
    </>
  );

  /**
   * 渲染循环执行节点配置
   */
  const renderLoopConfig = () => (
    <>
      <Form.Item label="循环类型">
        <Select
          value={data.config?.loopType || 'times'}
          onChange={(val) => handleConfigChange('loopType', val)}
        >
          <Select.Option value="times">固定次数</Select.Option>
          <Select.Option value="while">条件循环（满足条件时继续）</Select.Option>
          <Select.Option value="until">直到循环（满足条件时退出）</Select.Option>
        </Select>
      </Form.Item>

      {data.config?.loopType === 'times' && (
        <>
          <Form.Item label="循环次数">
            <InputNumber
              value={data.config?.maxIterations || 1}
              onChange={(val) => handleConfigChange('maxIterations', val)}
              min={1}
              max={1000}
              style={{ width: '100%' }}
            />
          </Form.Item>

          <Form.Item label="循环间隔（秒）">
            <InputNumber
              value={data.config?.loopInterval || 0}
              onChange={(val) => handleConfigChange('loopInterval', val)}
              min={0}
              style={{ width: '100%' }}
            />
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              0表示无间隔
            </Text>
          </Form.Item>
        </>
      )}

      {(data.config?.loopType === 'while' || data.config?.loopType === 'until') && (
        <>
          <Form.Item label="循环条件">
            <TextArea
              value={data.config?.loopCondition || ''}
              onChange={(e) => handleConfigChange('loopCondition', e.target.value)}
              placeholder={
                data.config?.loopType === 'while'
                  ? `满足此条件时继续循环，条件格式：{{变量}} > 10`
                  : `满足此条件时退出循环，条件格式：{{变量}} >= 100`
              }
              rows={3}
            />
          </Form.Item>

          <Form.Item label="最大迭代次数">
            <InputNumber
              value={data.config?.maxIterations || 100}
              onChange={(val) => handleConfigChange('maxIterations', val)}
              min={1}
              max={10000}
              style={{ width: '100%' }}
            />
            <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
              防止无限循环
            </Text>
          </Form.Item>
        </>
      )}

      <Divider style={{ margin: '12px 0' }} />

      <Form.Item label="循环变量">
        <Input
          value={data.config?.loopVariable || 'loopIndex'}
          onChange={(e) => handleConfigChange('loopVariable', e.target.value)}
          placeholder="循环计数器变量名"
        />
        <Text type="secondary" style={{ fontSize: 11, display: 'block', marginTop: 4 }}>
          可在循环体内通过 {`{{loopIndex}}`} 访问当前循环次数
        </Text>
      </Form.Item>
    </>
  );

  /**
   * 根据节点类型渲染配置表单
   */
  const renderNodeConfig = () => {
    switch (type) {
      case 'trigger':
        return renderTriggerConfig();
      case 'action':
        return renderActionConfig();
      case 'condition':
        return renderConditionConfig();
      case 'delay':
        return renderDelayConfig();
      case 'parallel':
        return renderParallelConfig();
      case 'loop':
        return renderLoopConfig();
      default:
        return <Text type="secondary">暂不支持此节点类型的配置</Text>;
    }
  };

  return (
    <div
      className="node-properties-panel"
      style={{
        width: 320,
        height: '100%',
        backgroundColor: '#fafafa',
        borderLeft: '1px solid #e8e8e8',
        overflow: 'auto',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* 面板标题 */}
      <div style={{
        padding: '16px 16px 12px',
        borderBottom: '1px solid #e8e8e8',
        backgroundColor: '#fff',
        flexShrink: 0,
      }}>
        <Title level={5} style={{ margin: 0 }}>
          节点配置
        </Title>
      </div>

      {/* 配置表单 */}
      <div style={{ flex: 1, overflow: 'auto', padding: 16 }}>
        {/* 节点信息卡片 */}
        <Card
          size="small"
          style={{ marginBottom: 16 }}
          bodyStyle={{ padding: 12 }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 36,
              height: 36,
              borderRadius: 8,
              backgroundColor: nodeMeta.color,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontSize: 14,
              fontWeight: 600,
            }}>
              {data.label?.charAt(0) || type.charAt(0)}
            </div>
            <div>
              <Text strong>{data.label || nodeMeta.name}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>
                ID: {id}
              </Text>
            </div>
          </div>
        </Card>

        {/* 节点名称 */}
        <Form layout="vertical" size="small">
          <Form.Item label="节点名称" style={{ marginBottom: 12 }}>
            <Input
              value={data.label}
              onChange={(e) => handleFieldChange('label', e.target.value)}
              placeholder="请输入节点名称"
            />
          </Form.Item>

          <Divider style={{ margin: '12px 0' }} />

          {/* 节点类型配置 */}
          {renderNodeConfig()}
        </Form>

        {/* 删除按钮区域（触发器节点不可删除） */}
        {!isTriggerNode && (
          <div style={{ marginTop: 16, paddingTop: 16, borderTop: '1px solid #e8e8e8' }}>
            <Popconfirm
              title="确认删除"
              description="确定要删除此节点吗？此操作不可撤销"
              onConfirm={handleDeleteNode}
              okText="删除"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button
                type="text"
                danger
                block
                icon={<span>🗑️</span>}
              >
                删除节点
              </Button>
            </Popconfirm>
          </div>
        )}
      </div>
    </div>
  );
}

export default NodeProperties;
