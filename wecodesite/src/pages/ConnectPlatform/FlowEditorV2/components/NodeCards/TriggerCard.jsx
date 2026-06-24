import React, { useState } from 'react';
import { Select, Input, Button, Tabs } from 'antd';
import SchemaEditorV2 from '../../../../../components/SchemaEditor/SchemaEditorV2';
import { CARRIER_TABS, TRIGGER_TYPE_OPTIONS } from '../../constants';
import './NodeCards.m.less';

/**
 * 触发器节点卡片
 *
 * @param {Object} props
 *   props.node     当前触发器节点数据
 *   props.editable 是否可编辑
 *   props.flowData 整个连接流数据（此卡片暂不使用，保留以保持接口一致）
 *   props.appLimits 应用级上限（此卡片暂不使用，保留以保持接口一致）
 *   props.onChange (updatedNode) => void
 */
const TriggerCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, onChange } = props;

  // 入参 Tab 的当前选中项
  const [activeCarrier, setActiveCarrier] = useState('header');

  /**
   * 更新触发方式
   * @param {string} value 新的触发方式值
   */
  const handleTriggerTypeChange = (value) => {
    onChange({ ...node, triggerType: value });
  };

  /**
   * 更新某个 systoken 的值
   *
   * @param {Object} params
   *   params.index 索引
   *   params.value 新值
   */
  const handleSystokenChange = (params) => {
    // params.index / params.value
    const { index, value } = params;
    const nextList = [...(node.systokens || [])];
    nextList[index] = value;
    onChange({ ...node, systokens: nextList });
  };

  /**
   * 添加 systoken
   */
  const handleAddSystoken = () => {
    onChange({ ...node, systokens: [...(node.systokens || []), ''] });
  };

  /**
   * 删除 systoken
   * @param {number} index 索引
   */
  const handleRemoveSystoken = (index) => {
    const nextList = [...(node.systokens || [])];
    nextList.splice(index, 1);
    onChange({ ...node, systokens: nextList });
  };

  /**
   * 更新指定 carrier 的入参 schema
   *
   * @param {Object} params
   *   params.carrier 载体类型 header/body/query
   *   params.value   SchemaEditorV2 回传的最新数组
   */
  const handleCarrierSchemaChange = (params) => {
    // params.carrier / params.value
    const { carrier, value } = params;
    const inputParams = node.inputParams || { header: [], body: [], query: [] };
    onChange({
      ...node,
      inputParams: { ...inputParams, [carrier]: value || [] },
    });
  };

  /**
   * 渲染单个 Tab 下的 SchemaEditorV2
   * @param {string} carrier 载体类型
   * @returns {JSX.Element}
   */
  const renderSchemaEditor = (carrier) => {
    const list = node.inputParams?.[carrier] || [];
    return (
      <SchemaEditorV2
        value={list}
        editable={editable}
        hideCarrier
        carrierFilter={carrier}
        carrierOptions={['header', 'body', 'query']}
        onChange={(next) => handleCarrierSchemaChange({ carrier, value: next })}
      />
    );
  };

  return (
    <div>
      {/* 触发方式 */}
      <div className="node-card-section">
        <div className="section-title">触发方式</div>
        <Select
          value={node.triggerType}
          disabled={!editable}
          options={TRIGGER_TYPE_OPTIONS}
          onChange={handleTriggerTypeChange}
          style={{ width: 240 }}
          placeholder="请选择触发方式"
        />
      </div>

      {/* SYSACCOUNT 白名单 */}
      <div className="node-card-section">
        <div className="section-title">SYSACCOUNT 白名单</div>
        <div className="section-desc">仅这些系统账号可以触发此连接流</div>
        <div className="systoken-list">
          {(node.systokens || []).map((token, index) => (
            <div key={`systoken-${index}`} className="systoken-row">
              <Input
                placeholder="请输入 SYSACCOUNT"
                value={token}
                disabled={!editable}
                onChange={(e) => handleSystokenChange({ index, value: e.target.value })}
              />
              <Button
                type="text"
                disabled={!editable}
                onClick={() => handleRemoveSystoken(index)}
              />
            </div>
          ))}
        </div>
        <Button
          type="dashed"
          disabled={!editable}
          className="add-row-btn"
          onClick={handleAddSystoken}
        >
          添加 SYSACCOUNT
        </Button>
      </div>

      {/* 入参配置 */}
      <div className="node-card-section">
        <div className="section-title">入参配置</div>
        <Tabs
          activeKey={activeCarrier}
          onChange={setActiveCarrier}
          items={CARRIER_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: renderSchemaEditor(tab.carrier),
          }))}
        />
      </div>
    </div>
  );
};

export default TriggerCard;
