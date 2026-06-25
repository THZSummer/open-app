import React, { useState, useEffect } from 'react';
import { Tabs, Input, InputNumber, Select, AutoComplete, Button, Tooltip, Tag } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import { CARRIER_TABS } from '../../constants';
import { VERSION_STATUS_MAP as CONNECTOR_VERSION_STATUS_MAP } from '../../../ConnectorEditor/constants';
import { fetchConnectorList, fetchConnectorVersions, fetchConnectorInputParams } from '../../thunk';
import { collectUpstreamRefs } from '../../utils';
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
 * 生成分支 ID
 * @returns {string} 唯一 ID
 */
const genBranchId = () => `b_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;

/**
 * 生成连接器节点 ID
 * @returns {string} 唯一 ID
 */
const genConnectorId = () => `node_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`;

/**
 * 创建空连接器节点
 * @returns {Object} 连接器节点
 */
const createEmptyConnector = () => ({
  id: genConnectorId(),
  type: 'connector',
  connectorId: '',
  versionId: '',
  authMethodId: '',
  authMappings: {},
  timeout: 3000,
  inputMappings: {},
});

/**
 * 取值模式选项
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
  // 按上游节点分组展示完整引用表达式，便于区分不同节点来源
  const groupMap = new Map();
  refs.forEach((item) => {
    const groupLabel = item.groupLabel || item.nodeId || '上游参数';
    const groupOptions = groupMap.get(groupLabel) || [];
    groupOptions.push({
      value: item.value,
      label: item.label || item.path || item.value,
    });
    groupMap.set(groupLabel, groupOptions);
  });
  return Array.from(groupMap.entries()).map(([label, options]) => ({ label, options }));
};

/**
 * 标准化 mapping
 *
 * @param {*} raw 原始 mapping
 * @returns {Object} { mode, value }
 */
const normalizeMapping = (raw) => {
  if (raw && typeof raw === 'object' && 'mode' in raw) {
    return { mode: raw.mode || 'static', value: raw.value ?? '' };
  }
  const str = typeof raw === 'string' ? raw : '';
  return { mode: 'static', value: str };
};

/**
 * 分支内嵌的连接器配置（精简版：连接器选择 + 版本 + 入参映射 + 超时，无认证方式块）
 *
 * @param {Object} props
 *   props.connector  连接器节点数据
 *   props.editable   是否可编辑
 *   props.appLimits  应用级上限
 *   props.flowData   整个连接流数据（用于上游引用收集）
 *   props.parallelNodeId 并行节点 ID（用于上游引用收集的截断点）
 *   props.onChange   (updatedConnector) => void
 */
const BranchConnectorEditor = (props) => {
  // props.connector / props.editable / props.appLimits / props.flowData / props.parallelNodeId / props.onChange
  const { connector, editable, appLimits, flowData, parallelNodeId, onChange } = props;

  // 连接器列表
  const [connectorList, setConnectorList] = useState([]);
  // 当前连接器对应的版本列表
  const [versionList, setVersionList] = useState([]);
  // 当前版本对应的入参 schema
  const [paramsSchema, setParamsSchema] = useState({ header: [], body: [], query: [] });
  // 当前版本对应的认证配置（来自连接器版本详情）
  const [authConfigs, setAuthConfigs] = useState([]);
  // 当前激活的载体 Tab
  const [activeCarrier, setActiveCarrier] = useState('header');

  /**
   * 拉取连接器列表
   */
  useEffect(() => {
    let cancelled = false;
    fetchConnectorList().then((res) => {
      if (cancelled) return;
      if (res?.code === '200') setConnectorList(res.data || []);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  /**
   * 拉取版本列表
   */
  useEffect(() => {
    if (!connector.connectorId) {
      setVersionList([]);
      return undefined;
    }
    let cancelled = false;
    fetchConnectorVersions(connector.connectorId).then((res) => {
      if (cancelled) return;
      if (res?.code === '200') setVersionList(res.data || []);
    });
    return () => {
      cancelled = true;
    };
  }, [connector.connectorId]);

  /**
   * 拉取入参 schema
   */
  useEffect(() => {
    if (!connector.connectorId || !connector.versionId) {
      setParamsSchema({ header: [], body: [], query: [] });
      setAuthConfigs([]);
      return undefined;
    }
    let cancelled = false;
    fetchConnectorInputParams({
      connectorId: connector.connectorId,
      versionId: connector.versionId,
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
        // 版本详情返回的出参 schema 与认证方式写入分支连接器，用于后续上游引用平铺展开
        const nextOutputParams = res.data?.outputParams || { header: [], body: [] };
        const nextAuthConfigs = res.data?.rawAuthConfigs || [];
        // 版本详情返回的完整连接器配置快照需要写入分支连接器，供保存编排时自包含
        const nextConnectorVersionConfig = res.data?.connectorVersionConfig || {};
        const shouldSyncConnector = JSON.stringify(connector.connectorVersionConfig || {}) !== JSON.stringify(nextConnectorVersionConfig)
          || JSON.stringify(connector.outputParams || {}) !== JSON.stringify(nextOutputParams)
          || JSON.stringify(connector.authConfigs || []) !== JSON.stringify(nextAuthConfigs)
          || (!!res.data?.authType && connector.authMethodId !== res.data.authType);
        if (shouldSyncConnector) {
          onChange({
            ...connector,
            connectorVersionConfig: nextConnectorVersionConfig,
            outputParams: nextOutputParams,
            authConfigs: nextAuthConfigs,
            authMethodId: res.data?.authType || connector.authMethodId,
          });
        }
      }
    });
    return () => {
      cancelled = true;
    };
  }, [connector.connectorId, connector.versionId]);

  /**
   * 切换连接器
   * @param {string} value 连接器 ID
   */
  const handleConnectorChange = (value) => {
    onChange({
      ...connector,
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
      ...connector,
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
    onChange({ ...connector, timeout: value });
  };

  /**
   * 上游可引用参数列表（以并行节点为截断点）
   */
  const upstreamRefs = React.useMemo(
    () => collectUpstreamRefs({ flowData, currentNodeId: parallelNodeId }),
    [flowData, parallelNodeId]
  );

  /**
   * AutoComplete 候选项
   */
  const refOptions = React.useMemo(() => buildRefOptions(upstreamRefs), [upstreamRefs]);

  /**
   * 更新入参映射的某个字段
   *
   * @param {Object} params
   *   params.carrier    载体
   *   params.paramName  参数名
   *   params.field      字段名（mode/value）
   *   params.value      新值
   */
  const handleMappingFieldChange = (params) => {
    // params.carrier / params.paramName / params.field / params.value
    const { carrier, paramName, field, value } = params;
    const mappings = connector.inputMappings || {};
    const carrierMap = { ...(mappings[carrier] || {}) };
    const current = normalizeMapping(carrierMap[paramName]);
    const next = { ...current, [field]: value };
    // 切换模式时清空值
    if (field === 'mode') {
      next.value = '';
    }
    carrierMap[paramName] = next;
    onChange({
      ...connector,
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
    const authMappings = connector.authMappings || {};
    const cookieMappings = { ...(authMappings.Cookie || {}) };
    const current = normalizeMapping(cookieMappings[paramName]);
    const next = { ...current, [field]: value };
    // 切换来源时清空值，避免静态值和引用变量串用
    if (field === 'mode') {
      next.value = '';
    }
    cookieMappings[paramName] = next;
    onChange({
      ...connector,
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
    const mapping = normalizeMapping(connector.authMappings?.Cookie?.[param.name]);
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
    const mapping = normalizeMapping(connector.authMappings?.Cookie?.[param.name]);

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
   * 渲染入参映射表
   * @param {string} carrier 载体类型
   * @returns {JSX.Element}
   */
  const renderMappingTable = (carrier) => {
    const list = paramsSchema[carrier] || [];
    const mappings = connector.inputMappings?.[carrier] || {};

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

  const timeoutMax = appLimits?.connectorTimeoutMax ?? 300000;
  const currentVersion = versionList.find((item) => item.versionId === connector.versionId);

  return (
    <div>
      {/* 连接器选择 */}
      <div className="node-card-section">
        <div className="section-title">连接器</div>
        <Select
          style={{ width: '100%' }}
          placeholder="请选择连接器"
          value={connector.connectorId || undefined}
          disabled={!editable}
          options={connectorList.map((item) => ({ value: item.id, label: item.name }))}
          onChange={handleConnectorChange}
        />
      </div>

      {/* 版本选择 */}
      {connector.connectorId ? (
        <div className="node-card-section">
          <div className="section-title">版本</div>
          <Select
            style={{ width: '100%' }}
            placeholder="请选择连接器版本"
            value={connector.versionId || undefined}
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
            value={connector.timeout}
            disabled={!editable}
            onChange={handleTimeoutChange}
          />
          <span className="section-desc">毫秒（上限 {timeoutMax} 毫秒）</span>
        </div>
      </div>
    </div>
  );
};

/**
 * 并行节点卡片
 *
 * @param {Object} props
 *   props.node      并行节点数据
 *   props.editable  是否可编辑
 *   props.flowData  整个连接流数据（保留接口一致性）
 *   props.appLimits 应用级上限（限制分支数）
 *   props.onChange  (updatedNode) => void
 */
const ParallelCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, flowData, appLimits, onChange } = props;

  // 当前激活的分支 Tab
  const [activeBranchKey, setActiveBranchKey] = useState(
    node.activeBranchId || node.branches?.[0]?.id || ''
  );

  // 分支数量上限
  const branchMax = appLimits?.parallelBranchMax ?? 8;
  const branches = node.branches || [];

  /**
   * 切换分支 Tab
   * @param {string} key 分支 ID
   */
  const handleTabChange = (key) => {
    setActiveBranchKey(key);
    onChange({ ...node, activeBranchId: key });
  };

  /**
   * 更新指定分支
   *
   * @param {Object} params
   *   params.branchId 分支 ID
   *   params.patch    要合并到分支上的字段
   */
  const handleBranchChange = (params) => {
    // params.branchId / params.patch
    const { branchId, patch } = params;
    const nextBranches = branches.map((branch) => (
      branch.id === branchId ? { ...branch, ...patch } : branch
    ));
    onChange({ ...node, branches: nextBranches });
  };

  /**
   * 更新分支内的连接器
   *
   * @param {Object} params
   *   params.branchId        分支 ID
   *   params.updatedConnector 更新后的连接器
   */
  const handleBranchConnectorChange = (params) => {
    // params.branchId / params.updatedConnector
    const { branchId, updatedConnector } = params;
    handleBranchChange({ branchId, patch: { connector: updatedConnector } });
  };

  /**
   * 添加分支
   */
  const handleAddBranch = () => {
    if (branches.length >= branchMax) return;
    const newBranch = {
      id: genBranchId(),
      label: `分支${branches.length + 1}`,
      connector: createEmptyConnector(),
    };
    onChange({ ...node, branches: [...branches, newBranch] });
    setActiveBranchKey(newBranch.id);
  };

  /**
   * 删除分支（仅在分支数 > 1 时可删）
   *
   * @param {Object} params
   *   params.branchId 分支 ID
   *   params.event    事件对象（用于阻止 Tab 切换）
   */
  const handleRemoveBranch = (params) => {
    // params.branchId / params.event
    const { branchId, event } = params;
    if (event) event.stopPropagation();
    if (branches.length <= 1) return;
    const nextBranches = branches.filter((branch) => branch.id !== branchId);
    const nextActiveId = activeBranchKey === branchId
      ? nextBranches[0]?.id || ''
      : activeBranchKey;
    setActiveBranchKey(nextActiveId);
    onChange({
      ...node,
      branches: nextBranches,
      activeBranchId: nextActiveId,
    });
  };

  return (
    <div>
      {/* 分支头部信息 */}
      <div className="branch-header">
        <span className="branch-count-text">
          共 {branches.length} 个分支（上限 {branchMax}）
        </span>
        <Button
          type="dashed"
          size="small"
          disabled={!editable || branches.length >= branchMax}
          onClick={handleAddBranch}
        >
          添加分支
        </Button>
      </div>

      {/* 分支 Tab */}
      <Tabs
        activeKey={activeBranchKey}
        onChange={handleTabChange}
        items={branches.map((branch) => ({
          key: branch.id,
          label: (
            <span>
              {branch.label}
              {editable && branches.length > 1 ? (
                <Tooltip title="删除分支">
                  <DeleteOutlined
                    style={{ marginLeft: 8, color: '#f5222d' }}
                    onClick={(event) => handleRemoveBranch({ branchId: branch.id, event })}
                  />
                </Tooltip>
              ) : null}
            </span>
          ),
          children: (
            <div className="branch-body">
              {/* 分支名称 */}
              <div className="branch-label-row">
                <span className="inline-form-label">分支名称</span>
                <Input
                  style={{ maxWidth: 240 }}
                  value={branch.label}
                  disabled={!editable}
                  onChange={(e) => handleBranchChange({
                    branchId: branch.id,
                    patch: { label: e.target.value },
                  })}
                />
              </div>

              {/* 内嵌连接器配置 */}
              <BranchConnectorEditor
                connector={branch.connector}
                editable={editable}
                appLimits={appLimits}
                flowData={flowData}
                parallelNodeId={node.id}
                onChange={(updatedConnector) => handleBranchConnectorChange({
                  branchId: branch.id,
                  updatedConnector,
                })}
              />
            </div>
          ),
        }))}
      />
    </div>
  );
};

export default ParallelCard;
