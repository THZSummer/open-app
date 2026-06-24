import React, { useState, useEffect } from 'react';
import { Tabs, Input, InputNumber, Select, AutoComplete, Button, Tooltip } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { CARRIER_TABS } from '../../constants';
import { fetchConnectorList, fetchConnectorVersions, fetchConnectorInputParams } from '../../thunk';
import { collectUpstreamRefs } from '../../utils';
import './NodeCards.m.less';

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
  timeout: 3,
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
  return refs.map(item => ({
    value: `\${${item.source}.${item.name}}`,
    label: `\${${item.source}.${item.name}}（${item.type}）`,
  }));
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
      return undefined;
    }
    let cancelled = false;
    fetchConnectorInputParams({
      connectorId: connector.connectorId,
      versionId: connector.versionId,
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
      inputMappings: {},
    });
  };

  /**
   * 更新超时时间
   * @param {number} value 超时秒数
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

  const timeoutMax = appLimits?.connectorTimeoutMax ?? 3;
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
            options={versionList.map((item) => ({ value: item.versionId, label: item.name }))}
            onChange={handleVersionChange}
          />
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
          <span className="section-desc">秒（上限 {timeoutMax} 秒）</span>
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
          icon={<PlusOutlined />}
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
