/**
 * ========================================
 * 连接器编辑页 - 接口基础配置组件
 * ========================================
 *
 * 包含：协议类型 + 协议地址
 * 内部维护字段更新逻辑，通过 onApiConfigChange 统一回写父级 apiConfig
 * 通过 forwardRef 暴露最外层 div，供父级发布校验失败时滚动定位
 */

import React, { forwardRef } from 'react';
import { Form, Input, Radio } from 'antd';
import { HTTP_METHOD_OPTIONS } from '../constants';

/**
 * 接口基础配置组件
 * @param {Object} props
 * props.form antd Form 实例
 * props.apiConfig 当前 API 配置
 * props.editable 是否可编辑
 * props.onApiConfigChange 统一回写 apiConfig 的回调，签名 (nextApiConfig)
 */
const BaseSection = forwardRef((props, ref) => {
  // 解构 props 中需要使用的字段
  const { form, apiConfig, editable, onApiConfigChange } = props;

  /**
   * 通用配置字段更新
   * @param {string} field 字段名
   * @param {any} value 字段值
   */
  const updateConfig = (field, value) => {
    // 以 Form 中的最新值为基准，避免快速连续输入丢失
    const current = form.getFieldValue('apiConfig') || {};
    const next = { ...current, [field]: value };
    onApiConfigChange(next);
  };

  return (
    <div className="section-card" ref={ref}>
      <div className="section-title">
        接口配置
        <span className="section-tip">配置接口协议与地址</span>
      </div>

      <Form.Item label="协议类型" className="form-item-spacing">
        <Radio.Group
          value={apiConfig.protocolType}
          disabled={!editable}
          onChange={(e) => updateConfig('protocolType', e.target.value)}
        >
          {HTTP_METHOD_OPTIONS.map(method => (
            <Radio.Button key={method} value={method}>
              {method}
            </Radio.Button>
          ))}
        </Radio.Group>
      </Form.Item>

      <Form.Item label="协议地址" className="form-item-no-spacing">
        <Input
          value={apiConfig.protocolAddress}
          disabled={!editable}
          onChange={(e) => updateConfig('protocolAddress', e.target.value)}
          placeholder="https://api.example.com/endpoint"
        />
      </Form.Item>
    </div>
  );
});

export default BaseSection;
