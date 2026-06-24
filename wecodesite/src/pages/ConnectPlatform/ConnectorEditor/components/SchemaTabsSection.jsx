/**
 * ========================================
 * 连接器编辑页 - 入参 / 出参 Tab 化区块组件
 * ========================================
 *
 * 按 carrier（header / body / query）分 Tab 维护 SchemaEditor
 * 通过 forwardRef 暴露最外层 div，供父级发布校验失败时滚动定位
 */

import React, { forwardRef } from 'react';
import { Tabs } from 'antd';
import SchemaEditor from '../../../../components/SchemaEditor/SchemaEditorV2.jsx';

/**
 * 入参 / 出参 Tab 化区块组件
 * @param {Object} props
 * props.title 标题
 * props.tip 标题旁说明
 * props.tabs Tab 配置数组（每项含 key / label / carrier）
 * props.schemaConfig 传给 SchemaEditor 的配置
 * props.hideCarrier 是否隐藏 carrier 下拉（入参 Tab 隐藏）
 * props.form Form 实例（透传给 SchemaEditor）
 * props.apiConfig 当前 API 配置（透传给 SchemaEditor）
 * props.editable 是否可编辑（透传给 SchemaEditor）
 */
const SchemaTabsSection = forwardRef((props, ref) => {
  // 解构 props 中需要使用的字段
  const {
    title,
    tip,
    tabs,
    schemaConfig,
    hideCarrier,
    form,
    apiConfig,
    editable,
  } = props;

  // 组装 Tab 项
  const tabItems = tabs.map(tab => ({
    key: tab.key,
    label: tab.label,
    children: (
      <SchemaEditor
        form={form}
        apiConfig={apiConfig}
        {...schemaConfig}
        editable={editable}
        carrierFilter={tab.carrier}
        hideCarrier={hideCarrier}
      />
    ),
  }));

  return (
    <div className="section-card" ref={ref}>
      <div className="section-title">
        {title}
        <span className="section-tip">{tip}</span>
      </div>
      <Tabs defaultActiveKey={tabs[0].key} items={tabItems} />
    </div>
  );
});

export default SchemaTabsSection;
