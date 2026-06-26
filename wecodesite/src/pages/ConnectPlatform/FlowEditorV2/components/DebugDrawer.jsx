/**
 * ========================================
 * 调试抽屉
 * ========================================
 *
 * 展示触发器入参赋值区和执行输出区。
 * 视觉：Hero + 分组卡片 + Trace 时间轴 + 暗色 JSON 面板 + 错误引用块。
 */

import React, { useState, useEffect } from 'react';
import { Drawer, Tabs, Input, Button, Space, Empty, Spin } from 'antd';
import { ApiOutlined, FundProjectionScreenOutlined } from '@ant-design/icons';
import ExecutionTraceViewer from '../../../../components/ExecutionTraceViewer/ExecutionTraceViewer';
import '../FlowEditorV2.m.less';

/**
 * 调试抽屉
 *
 * @param {Object} props
 * @param {boolean} props.visible 是否显示
 * @param {string} props.flowName 连接流名称
 * @param {Object} props.triggerInputParams 触发器入参对象 { header: [], body: [], query: [] }
 * @param {Object} props.debugResult 调试结果
 * @param {boolean} props.debugLoading 调试 loading
 * @param {Function} props.onClose 关闭回调
 * @param {Function} props.onDebug 立即调试回调 (paramValues) => void
 */
const DebugDrawer = (props) => {
  const {
    visible,
    flowName,
    triggerInputParams,
    debugResult,
    debugLoading,
    onClose,
    onDebug,
  } = props;

  // 用户输入的入参值（按 carrier 分类）
  const [paramValues, setParamValues] = useState({
    header: {},
    body: {},
    query: {},
  });

  // 抽屉打开时重置入参值
  useEffect(() => {
    if (visible) {
      setParamValues({ header: {}, body: {}, query: {} });
    }
  }, [visible]);

  /**
   * 更新指定 carrier 下的参数值
   *
   * @param {Object} params
   * @param {string} params.carrier 载体（header/body/query）
   * @param {string} params.name 参数名
   * @param {string} params.value 参数值
   */
  const handleValueChange = (params) => {
    // params.carrier / params.name / params.value
    const { carrier, name, value } = params;
    setParamValues({
      ...paramValues,
      [carrier]: { ...paramValues[carrier], [name]: value },
    });
  };

  /**
   * 渲染单个 carrier 下的参数表
   * @param {string} carrier 载体类型
   * @returns {React.ReactNode} 渲染内容
   */
  const renderParamTable = (carrier) => {
    const list = triggerInputParams?.[carrier] || [];
    if (list.length === 0) {
      return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无入参" />;
    }
    return (
      <div>
        {list.map(param => {
          // 兼容 SchemaEditorV2 的新字段 paramName/paramType
          const pName = param.paramName ?? param.name;
          const pType = param.paramType ?? param.type;
          return (
            <div key={param.id || pName} className="debug-param-row">
              <div className="debug-param-name">{pName}</div>
              <span className="debug-param-type">{pType}</span>
              <Input
                value={paramValues[carrier][pName] || ''}
                placeholder="请输入参数值"
                onChange={(e) => handleValueChange({
                  carrier,
                  name: pName,
                  value: e.target.value,
                })}
              />
            </div>
          );
        })}
      </div>
    );
  };

  /**
   * 触发立即调试
   */
  const handleDebug = () => {
    onDebug(paramValues);
  };

  /**
   * 渲染调试输出
   * @returns {React.ReactNode} 输出渲染
   */
  const renderDebugOutput = () => {
    if (debugLoading) {
      return (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin tip="调试中..." />
        </div>
      );
    }
    if (!debugResult) {
      return (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="暂无调试结果，填写入参并点击「立即调试」"
        />
      );
    }

    const steps = debugResult.steps || [];
    const pillClass = debugResult.success ? 'status-pill pill-success' : 'status-pill pill-error';

    return (
      <>
        {/* 状态 + 耗时 */}
        <div className="drawer-metrics">
          <span className={pillClass}>{debugResult.success ? '执行成功' : '执行失败'}</span>
          <span className="metrics-item">
            耗时 <span className="metrics-num">{debugResult.duration}ms</span>
          </span>
          <span className="metrics-item">
            节点 <span className="metrics-num">{steps.length}</span>
          </span>
        </div>

        {/* 节点执行时间线 */}
        {steps.length > 0 && (
          <div style={{ marginBottom: 16 }}>
            <div style={{ fontSize: 13, color: '#4e5969', marginBottom: 8, fontWeight: 500 }}>
              节点执行时间线
            </div>
            <ExecutionTraceViewer steps={steps} />
          </div>
        )}

        {/* 错误信息 */}
        {debugResult.error && (
          <div style={{ marginTop: 12 }}>
            <div style={{ fontSize: 13, color: '#c43232', marginBottom: 8, fontWeight: 500 }}>
              错误信息
            </div>
            <div className="callout-error">{debugResult.error}</div>
          </div>
        )}
      </>
    );
  };

  const tabItems = [
    { key: 'header', label: 'HTTP 请求头', children: renderParamTable('header') },
    { key: 'body', label: 'HTTP 请求体', children: renderParamTable('body') },
    { key: 'query', label: 'URL 查询参数', children: renderParamTable('query') },
  ];

  return (
    <Drawer
      title={null}
      placement="right"
      width={680}
      open={visible}
      onClose={onClose}
      destroyOnClose
      footer={
        <Space style={{ float: 'right' }}>
          <Button onClick={onClose}>关闭</Button>
          <Button
            type="primary"
            loading={debugLoading}
            onClick={handleDebug}
          >
            立即调试
          </Button>
        </Space>
      }
    >
      {/* Hero 区 */}
      <div className="drawer-hero">
        <div className="drawer-hero-title">
          <span>连接流调试</span>
          <span className="status-pill pill-processing">DEBUG</span>
        </div>
        <div className="drawer-hero-sub">{flowName || '未命名连接流'} · 填写触发器入参后执行立即调试</div>
      </div>

      {/* 入参配置卡片 */}
      <div className="drawer-section">
        <div className="section-title">
          <ApiOutlined style={{ color: '#3370ff' }} />
          入参配置
          <span className="section-title-extra">参数名称与类型只读，仅参数值可编辑</span>
        </div>
        <Tabs items={tabItems} defaultActiveKey="header" />
      </div>

      {/* 执行输出卡片 */}
      <div className="drawer-section">
        <div className="section-title">
          <FundProjectionScreenOutlined style={{ color: '#3370ff' }} />
          执行输出
        </div>
        {renderDebugOutput()}
      </div>
    </Drawer>
  );
};

export default DebugDrawer;
