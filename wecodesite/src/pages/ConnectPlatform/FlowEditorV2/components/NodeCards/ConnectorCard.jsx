import React, { useState, useEffect, useMemo } from 'react';
import { Select, Input, InputNumber, Tabs, AutoComplete } from 'antd';
import { CARRIER_TABS } from '../../constants';
import { fetchConnectorList, fetchConnectorVersions, fetchConnectorInputParams } from '../../thunk';
import { collectUpstreamRefs } from '../../utils';
import './NodeCards.m.less';

/**
 * 认证方式 ID → 文案映射
 */
const AUTH_METHOD_LABEL = {
  SOA: 'SOA 认证',
  APIG: 'APIG 认证',
  NONE: '无认证',
};

/**
 * 取值模式选项：静态值 / 引用上游参数
 */
const VALUE_MODE_OPTIONS = [
  { value: 'static', label: '静态值' },
  { value: 'ref', label: '引用上游参数' },
];

/**
 * 把上游 ref 列表转换为 AutoComplete options
 *
 * @param {Array} refs 上游引用列表
 * @returns {Array} AutoComplete options
 */
const buildRefOptions = (refs) => {
  // 将上游 ref 转为 ${source.name} 形式
  return refs.map(item => ({
    value: `\${${item.source}.${item.name}}`,
    label: `\${${item.source}.${item.name}}（${item.type}）`,
  }));
};

/**
 * 标准化 mapping，兼容旧字符串值
 *
 * @param {*} raw 原始 mapping 值
 * @returns {Object} 标准化后的 mapping 对象 { mode, value }
 */
const normalizeMapping = (raw) => {
  if (raw && typeof raw === 'object' && 'mode' in raw) {
    return { mode: raw.mode || 'static', value: raw.value ?? '' };
  }
  // 兼容旧的字符串形式
  const str = typeof raw === 'string' ? raw : '';
  return { mode: 'static', value: str };
};

/**
 * 连接器节点卡片
 *
 * @param {Object} props
 *   props.node      连接器节点数据
 *   props.editable  是否可编辑
 *   props.flowData  整个连接流数据（用于收集上游引用参数）
 *   props.appLimits 应用级上限（用于约束超时时间）
 *   props.onChange  (updatedNode) => void
 */
const ConnectorCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, flowData, appLimits, onChange } = props;

  // 连接器列表
  const [connectorList, setConnectorList] = useState([]);
  // 当前连接器对应的版本列表
  const [versionList, setVersionList] = useState([]);
  // 当前版本对应的入参 schema（来自后端 mock）
  const [paramsSchema, setParamsSchema] = useState({ header: [], body: [], query: [] });
  // 当前激活的载体 Tab
  const [activeCarrier, setActiveCarrier] = useState('header');

  /**
   * 上游可引用参数列表
   */
  const upstreamRefs = useMemo(
    () => collectUpstreamRefs({ flowData, currentNodeId: node.id }),
    [flowData, node.id]
  );

  /**
   * AutoComplete 候选项
   */
  const refOptions = useMemo(() => buildRefOptions(upstreamRefs), [upstreamRefs]);

  /**
   * 拉取连接器列表
   */
  useEffect(() => {
    let cancelled = false;
    fetchConnectorList().then((res) => {
      if (cancelled) return;
      if (res?.code === '200') {
        setConnectorList(res.data || []);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  /**
   * 连接器变更后拉取版本列表
   */
  useEffect(() => {
    if (!node.connectorId) {
      setVersionList([]);
      return undefined;
    }
    let cancelled = false;
    fetchConnectorVersions(node.connectorId).then((res) => {
      if (cancelled) return;
      if (res?.code === '200') {
        setVersionList(res.data || []);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [node.connectorId]);

  /**
   * 版本变更后拉取入参 schema
   */
  useEffect(() => {
    if (!node.connectorId || !node.versionId) {
      setParamsSchema({ header: [], body: [], query: [] });
      return undefined;
    }
    let cancelled = false;
    fetchConnectorInputParams({
      connectorId: node.connectorId,
      versionId: node.versionId,
    }).then((res) => {
      if (cancelled) return;
      if (res?.code === '200') {
        setParamsSchema({
          header: res.data?.header || [],
          body: res.data?.body || [],
          query: res.data?.query || [],
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [node.connectorId, node.versionId]);

  /**
   * 当前选中版本的元数据
   */
  const currentVersion = versionList.find((item) => item.versionId === node.versionId);

  /**
   * 切换连接器
   * @param {string} value 连接器 ID
   */
  const handleConnectorChange = (value) => {
    onChange({
      ...node,
      connectorId: value,
      versionId: '',
      authMethodId: '',
      inputMappings: {},
    });
  };

  /**
   * 切换版本
   * @param {string} value 版本 ID
   */
  const handleVersionChange = (value) => {
    const version = versionList.find((item) => item.versionId === value);
    onChange({
      ...node,
      versionId: value,
      authMethodId: version?.authType || '',
      inputMappings: {},
    });
  };

  /**
   * 更新超时时间
   * @param {number} value 超时秒数
   */
  const handleTimeoutChange = (value) => {
    onChange({ ...node, timeout: value });
  };

  /**
   * 更新入参映射的某个字段
   *
   * @param {Object} params
   *   params.carrier   载体
   *   params.paramName 参数名
   *   params.field     字段名（mode/value）
   *   params.value     新值
   */
  const handleMappingFieldChange = (params) => {
    // params.carrier / params.paramName / params.field / params.value
    const { carrier, paramName, field, value } = params;
    const mappings = node.inputMappings || {};
    const carrierMap = { ...(mappings[carrier] || {}) };
    const current = normalizeMapping(carrierMap[paramName]);
    const next = { ...current, [field]: value };
    // 切换模式时清空值，避免遗留
    if (field === 'mode') {
      next.value = '';
    }
    carrierMap[paramName] = next;
    onChange({
      ...node,
      inputMappings: { ...mappings, [carrier]: carrierMap },
    });
  };

  /**
   * 渲染单个 Tab 下的入参映射表
   * @param {string} carrier 载体
   * @returns {JSX.Element}
   */
  const renderMappingTable = (carrier) => {
    const list = paramsSchema[carrier] || [];
    const mappings = node.inputMappings?.[carrier] || {};

    if (list.length === 0) {
      return <div className="param-empty">当前版本暂无 {carrier} 入参</div>;
    }

    return (
      <div className="param-table">
        {list.map((param) => {
          const mapping = normalizeMapping(mappings[param.name]);
          return (
            <div key={param.name} className="param-row param-row-with-mode">
              <Input value={param.name} disabled />
              <Input value={param.type} disabled />
              <Select
                value={mapping.mode}
                disabled={!editable}
                options={VALUE_MODE_OPTIONS}
                onChange={(value) => handleMappingFieldChange({
                  carrier,
                  paramName: param.name,
                  field: 'mode',
                  value,
                })}
              />
              {mapping.mode === 'ref' ? (
                <AutoComplete
                  placeholder="选择或输入引用表达式"
                  value={mapping.value}
                  disabled={!editable}
                  options={refOptions}
                  filterOption={(input, option) => (
                    (option?.value || '').toLowerCase().includes((input || '').toLowerCase())
                  )}
                  onChange={(value) => handleMappingFieldChange({
                    carrier,
                    paramName: param.name,
                    field: 'value',
                    value,
                  })}
                />
              ) : (
                <Input
                  placeholder="请输入静态值"
                  value={mapping.value}
                  disabled={!editable}
                  onChange={(e) => handleMappingFieldChange({
                    carrier,
                    paramName: param.name,
                    field: 'value',
                    value: e.target.value,
                  })}
                />
              )}
            </div>
          );
        })}
      </div>
    );
  };

  // 超时上限
  const timeoutMax = appLimits?.connectorTimeoutMax ?? 3;

  return (
    <div>
      {/* 连接器选择 */}
      <div className="node-card-section">
        <div className="section-title">连接器</div>
        <Select
          style={{ width: '100%' }}
          placeholder="请选择连接器"
          value={node.connectorId || undefined}
          disabled={!editable}
          options={connectorList.map((item) => ({ value: item.id, label: item.name }))}
          onChange={handleConnectorChange}
        />
      </div>

      {/* 版本选择 */}
      {node.connectorId ? (
        <div className="node-card-section">
          <div className="section-title">版本</div>
          <Select
            style={{ width: '100%' }}
            placeholder="请选择连接器版本"
            value={node.versionId || undefined}
            disabled={!editable}
            options={versionList.map((item) => ({ value: item.versionId, label: item.name }))}
            onChange={handleVersionChange}
          />
        </div>
      ) : null}

      {/* 认证方式 */}
      {currentVersion ? (
        <div className="node-card-section">
          <div className="section-title">认证方式</div>
          <span className="auth-method-tag">
            {AUTH_METHOD_LABEL[currentVersion.authType] || currentVersion.authType || '未知'}
          </span>
        </div>
      ) : null}

      {/* 入参映射 */}
      {currentVersion ? (
        <div className="node-card-section">
          <div className="section-title">入参映射</div>
          <Tabs
            activeKey={activeCarrier}
            onChange={setActiveCarrier}
            items={CARRIER_TABS.map((tab) => ({
              key: tab.key,
              label: tab.label,
              children: renderMappingTable(tab.carrier),
            }))}
          />
        </div>
      ) : null}

      {/* 超时时间 */}
      <div className="node-card-section">
        <div className="section-title">超时时间</div>
        <div className="inline-form-row">
          <InputNumber
            min={1}
            max={timeoutMax}
            value={node.timeout}
            disabled={!editable}
            onChange={handleTimeoutChange}
          />
          <span className="section-desc">秒（上限 {timeoutMax} 秒）</span>
        </div>
      </div>
    </div>
  );
};

export default ConnectorCard;
