import React, { useState, useEffect, useMemo } from 'react';
import { Select, Input, InputNumber, Tabs, AutoComplete, Tag } from 'antd';
import { CARRIER_TABS } from '../../constants';
import { VERSION_STATUS_MAP as CONNECTOR_VERSION_STATUS_MAP } from '../../../ConnectorEditor/constants';
import { fetchConnectorList, fetchConnectorVersions, fetchConnectorInputParams } from '../../thunk';
import {
  buildRefOptions,
  collectUpstreamRefs,
  normalizeMapping,
  VALUE_MODE_OPTIONS,
} from '../../utils';
import './NodeCards.m.less';

/**
 * 认证方式 ID → 文案映射
 */
const AUTH_METHOD_LABEL = {
  SOA: 'SOA 认证',
  APIG: 'APIG 认证',
  Cookie: 'Cookie 认证',
  SIGNATURE: '数字签名认证',
  NONE: '无认证',
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
  // 当前版本对应的入参 schema（来自连接器版本详情）
  const [paramsSchema, setParamsSchema] = useState({ header: [], body: [], query: [] });
  // 当前版本对应的认证配置（来自连接器版本详情）
  const [authConfigs, setAuthConfigs] = useState([]);
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
      setAuthConfigs([]);
      return undefined;
    }
    let cancelled = false;
    fetchConnectorInputParams({
      connectorId: node.connectorId,
      versionId: node.versionId,
    }).then((res) => {
      if (cancelled) return;
      if (res?.code === '200') {
        // 版本详情返回的入参 schema 用于渲染入参映射表
        setParamsSchema({
          header: res.data?.header || [],
          body: res.data?.body || [],
          query: res.data?.query || [],
        });
        // 版本详情返回的认证配置用于渲染认证明细块
        setAuthConfigs(res.data?.authConfigs || []);
        // 版本详情返回的出参 schema 与认证方式写入节点数据，用于后续上游引用平铺展开
        const nextOutputParams = res.data?.outputParams || { header: [], body: [] };
        const nextAuthConfigs = res.data?.rawAuthConfigs || [];
        // 版本详情返回的完整连接器配置快照需要写入节点数据，供保存编排时自包含
        const nextConnectorVersionConfig = res.data?.connectorVersionConfig || {};
        const shouldSyncNode = JSON.stringify(node.connectorVersionConfig || {}) !== JSON.stringify(nextConnectorVersionConfig)
          || JSON.stringify(node.outputParams || {}) !== JSON.stringify(nextOutputParams)
          || JSON.stringify(node.authConfigs || []) !== JSON.stringify(nextAuthConfigs)
          || (!!res.data?.authType && node.authMethodId !== res.data.authType);
        if (shouldSyncNode) {
          onChange({
            ...node,
            connectorVersionConfig: nextConnectorVersionConfig,
            outputParams: nextOutputParams,
            authConfigs: nextAuthConfigs,
            authMethodId: res.data?.authType || node.authMethodId,
          });
        }
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
      authMappings: {},
      authConfigs: [],
      connectorVersionConfig: {},
      outputParams: { header: [], body: [] },
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
      authMappings: {},
      authConfigs: [],
      connectorVersionConfig: {},
      outputParams: { header: [], body: [] },
      inputMappings: {},
    });
  };

  /**
   * 更新超时时间
   * @param {number} value 超时毫秒数
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
   * 更新 Cookie 认证映射的某个字段
   *
   * @param {Object} params
   *   params.paramName 参数名
   *   params.field     字段名（mode/value）
   *   params.value     新值
   */
  const handleAuthMappingFieldChange = (params) => {
    // params.paramName / params.field / params.value
    const { paramName, field, value } = params;
    const authMappings = node.authMappings || {};
    const cookieMappings = { ...(authMappings.Cookie || {}) };
    const current = normalizeMapping(cookieMappings[paramName]);
    const next = { ...current, [field]: value };
    // 切换来源时清空值，避免静态值和引用变量串用
    if (field === 'mode') {
      next.value = '';
    }
    cookieMappings[paramName] = next;
    onChange({
      ...node,
      authMappings: { ...authMappings, Cookie: cookieMappings },
    });
  };

  /**
   * 渲染 Cookie 认证映射输入框
   *
   * @param {Object} param 认证参数
   * @returns {JSX.Element} Cookie 输入控件
   */
  const renderCookieAuthValueInput = (param) => {
    // 当前 Cookie 参数的映射配置
    const mapping = normalizeMapping(node.authMappings?.Cookie?.[param.name]);
    if (mapping.mode === 'ref') {
      return (
        <AutoComplete
          placeholder="选择上游参数"
          value={mapping.value}
          disabled={!editable}
          options={refOptions}
          filterOption={(input, option) => {
            // 分组选项本身不参与过滤，实际过滤由子选项完成
            if (Array.isArray(option?.options)) return false;
            return (option?.label || option?.value || '').toLowerCase().includes((input || '').toLowerCase());
          }}
          onChange={(value) => handleAuthMappingFieldChange({
            paramName: param.name,
            field: 'value',
            value,
          })}
        />
      );
    }
    return (
      <Input
        placeholder="请输入静态值"
        value={mapping.value}
        disabled={!editable}
        onChange={(e) => handleAuthMappingFieldChange({
          paramName: param.name,
          field: 'value',
          value: e.target.value,
        })}
      />
    );
  };

  /**
   * 渲染认证配置参数行
   *
   * @param {Object} params
   *   params.authType 认证类型
   *   params.param    认证参数
   * @returns {JSX.Element} 认证参数行
   */
  const renderAuthParamRow = (params) => {
    // params.authType / params.param
    const { authType, param } = params;
    const isCookie = authType === 'Cookie';
    const mapping = normalizeMapping(node.authMappings?.Cookie?.[param.name]);

    if (isCookie) {
      return (
        <div key={`${authType}-${param.carrier}-${param.name}`} className="auth-config-row auth-config-row-cookie">
          <Input value={param.name} disabled />
          <Input value={param.type} disabled />
          <Input value={param.carrier} disabled />
          <Select
            value={mapping.mode}
            disabled={!editable}
            options={VALUE_MODE_OPTIONS}
            onChange={(value) => handleAuthMappingFieldChange({
              paramName: param.name,
              field: 'mode',
              value,
            })}
          />
          {renderCookieAuthValueInput(param)}
        </div>
      );
    }

    return (
      <div key={`${authType}-${param.carrier}-${param.name}`} className="auth-config-row">
        <Input value={param.name} disabled />
        <Input value={param.type} disabled />
        <Input value={param.carrier} disabled />
        <Input value={param.mappingValue} disabled />
      </div>
    );
  };

  /**
   * 渲染认证配置区块
   * @returns {JSX.Element} 认证配置区块
   */
  const renderAuthConfigs = () => {
    if (authConfigs.length === 0) {
      return <div className="param-empty">当前版本暂无认证配置</div>;
    }

    return (
      <div className="auth-config-list">
        {authConfigs.map((config) => (
          <div className="auth-config-block" key={config.type}>
            <div className="auth-config-title">
              <span className="auth-method-tag">{config.type}</span>
              <span>{AUTH_METHOD_LABEL[config.type] || config.type}</span>
            </div>
            {config.params.map((param) => renderAuthParamRow({ authType: config.type, param }))}
          </div>
        ))}
      </div>
    );
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
                  placeholder="选择上游参数"
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

  /**
   * 构建连接器版本下拉框选项
   * @returns {Array} 版本选项列表
   */
  const buildVersionOptions = () => {
    return versionList.map((item) => ({
      value: item.versionId,
      label: (
        <div className="version-option">
          <span className="version-option-name">{item.name}</span>
          <span className="version-option-time">{item.createTime}</span>
          <Tag color={CONNECTOR_VERSION_STATUS_MAP[item.status]?.color}>
            {CONNECTOR_VERSION_STATUS_MAP[item.status]?.text}
          </Tag>
        </div>
      ),
    }));
  };

  // 超时上限（毫秒）
  const timeoutMax = appLimits?.connectorTimeoutMax ?? 300000;

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
            options={buildVersionOptions()}
            onChange={handleVersionChange}
          />
        </div>
      ) : null}

      {/* 认证方式 */}
      {currentVersion ? (
        <div className="node-card-section">
          <div className="section-title">认证方式</div>
          {renderAuthConfigs()}
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
          <span className="section-desc">毫秒（上限 {timeoutMax} 毫秒）</span>
        </div>
      </div>
    </div>
  );
};

export default ConnectorCard;
