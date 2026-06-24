import React, { useState } from 'react';
import { Tabs } from 'antd';
import SchemaEditorV2 from '../../../../../components/SchemaEditor/SchemaEditorV2';
import { OUTPUT_CARRIER_TABS } from '../../constants';
import './NodeCards.m.less';

/**
 * 数据输出节点卡片
 *
 * @param {Object} props
 *   props.node      输出节点数据
 *   props.editable  是否可编辑
 *   props.flowData  整个连接流数据（保留接口一致性）
 *   props.appLimits 应用级上限（保留接口一致性）
 *   props.onChange  (updatedNode) => void
 */
const OutputCard = (props) => {
  // props.node / props.editable / props.flowData / props.appLimits / props.onChange
  const { node, editable, onChange } = props;

  // 当前激活的载体 Tab，默认从 body 开始（与配置顺序一致）
  const [activeCarrier, setActiveCarrier] = useState(OUTPUT_CARRIER_TABS[0].carrier);

  /**
   * 更新某 carrier 下的 schema
   *
   * @param {Object} params
   *   params.carrier 载体
   *   params.value   SchemaEditorV2 回传的最新数组
   */
  const handleCarrierSchemaChange = (params) => {
    // params.carrier / params.value
    const { carrier, value } = params;
    const assembleParams = node.assembleParams || { body: [], header: [] };
    onChange({
      ...node,
      assembleParams: { ...assembleParams, [carrier]: value || [] },
    });
  };

  /**
   * 渲染指定 carrier 下的 SchemaEditorV2
   * @param {string} carrier 载体类型
   * @returns {JSX.Element}
   */
  const renderSchemaEditor = (carrier) => {
    const list = node.assembleParams?.[carrier] || [];
    return (
      <SchemaEditorV2
        value={list}
        editable={editable}
        hideCarrier
        carrierFilter={carrier}
        carrierOptions={['body', 'header']}
        onChange={(next) => handleCarrierSchemaChange({ carrier, value: next })}
      />
    );
  };

  return (
    <div>
      <div className="node-card-section">
        <div className="section-title">响应参数组装</div>
        <div className="section-desc">
          配置响应体与响应头参数
        </div>
        <Tabs
          activeKey={activeCarrier}
          onChange={setActiveCarrier}
          items={OUTPUT_CARRIER_TABS.map((tab) => ({
            key: tab.key,
            label: tab.label,
            children: renderSchemaEditor(tab.carrier),
          }))}
        />
      </div>
    </div>
  );
};

export default OutputCard;
