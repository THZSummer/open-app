/**
 * ========================================
 * 连接器编辑页 - 认证方式配置组件
 * ========================================
 *
 * 包含：
 * - 认证方式多选（SOA / APIG / Cookie / 数字签名）
 * - 通用认证子区块（SOA / APIG / Cookie）
 * - 数字签名子区块（含密钥显隐切换）
 * 内部维护：签名密钥显隐 / 认证方式变更 / 通用参数更新 / 签名字段更新
 * 通过 onApiConfigChange 统一回写父级 apiConfig
 * 通过 forwardRef 暴露最外层 div，供父级发布校验失败时滚动定位
 */

import React, { forwardRef, useState } from 'react';
import { Form, Input, Button, Select, Checkbox } from 'antd';
import {
  AUTH_SCHEMA_MAP,
  AUTH_TYPE_OPTIONS,
  AUTH_TYPE_NAMES,
  AUTH_CARRIER_OPTIONS,
  AUTH_PARAM_ROW_CONFIG,
  COMMON_AUTH_TYPES,
  SIGNATURE_AUTH_TYPE,
} from '../constants';

const { Option } = Select;

/**
 * 认证方式配置组件
 * @param {Object} props
 * props.form antd Form 实例
 * props.apiConfig 当前 API 配置
 * props.editable 是否可编辑
 * props.onApiConfigChange 统一回写 apiConfig 的回调，签名 (nextApiConfig)
 */
const AuthSection = forwardRef((props, ref) => {
  // 解构 props 中需要使用的字段
  const { form, apiConfig, editable, onApiConfigChange } = props;

  // 数字签名密钥掩码展示状态（独立于编辑态，仅本组件使用）
  const [signatureSecretMasked, setSignatureSecretMasked] = useState(true);

  // 当前已勾选的认证方式
  const authTypes = apiConfig.authType || [];

  /**
   * 切换密钥掩码显示
   */
  const toggleSecretMask = () => {
    setSignatureSecretMasked(prev => !prev);
  };

  /**
   * 处理认证多选变化
   * 勾选时初始化默认参数；取消时清除对应参数
   * @param {Array} newAuthTypes 新的认证方式数组
   */
  const handleAuthTypeChange = (newAuthTypes) => {
    const current = form.getFieldValue('apiConfig') || {};
    const oldAuthRequestSchema = current.authRequestSchema || {};
    const nextAuthRequestSchema = {};

    // 仅保留仍勾选的通用认证参数；新勾选项使用默认参数
    COMMON_AUTH_TYPES.forEach(t => {
      if (newAuthTypes.includes(t)) {
        nextAuthRequestSchema[t] = oldAuthRequestSchema[t] || JSON.parse(JSON.stringify(AUTH_SCHEMA_MAP[t] || []));
      }
    });

    const next = {
      ...current,
      authType: newAuthTypes,
      authRequestSchema: nextAuthRequestSchema,
    };
    onApiConfigChange(next);
  };

  /**
   * 更新指定认证方式的单个参数字段
   * @param {Object} options
   * options.authType 认证方式标识
   * options.index 参数索引
   * options.field 字段名
   * options.value 字段值
   */
  const updateAuthParam = (options) => {
    // 解构所需参数
    const { authType, index, field, value } = options;
    const current = form.getFieldValue('apiConfig') || {};
    const oldItems = (current.authRequestSchema || {})[authType] || [];
    const nextItems = oldItems.map((item, i) => (i === index ? { ...item, [field]: value } : item));
    const nextSchema = { ...(current.authRequestSchema || {}), [authType]: nextItems };
    const next = { ...current, authRequestSchema: nextSchema };
    onApiConfigChange(next);
  };

  /**
   * 更新数字签名配置（独立结构）
   * @param {string} field 字段名（paramName / carrier / fixedValue / secret）
   * @param {string} value 字段值
   */
  const handleSignatureChange = (field, value) => {
    const current = form.getFieldValue('apiConfig') || {};
    const nextSig = { ...(current.signatureConfig || {}), [field]: value };
    const next = { ...current, signatureConfig: nextSig };
    onApiConfigChange(next);
  };

  /**
   * 渲染认证参数行
   * @param {Object} options
   * options.rowKey 行唯一标识
   * options.item 参数对象
   * options.valueColumnValue 固定值/值来源列展示值
   * options.valueColumnPlaceholder 固定值/值来源列占位文案
   * options.onParamNameChange 参数名称变更回调
   * options.onCarrierChange carrier 变更回调
   * options.extraFields 额外字段节点
   */
  const renderAuthParamRow = (options) => {
    // 解构渲染认证参数行所需参数
    const { rowKey, item, valueColumnValue, valueColumnPlaceholder, onParamNameChange, onCarrierChange, extraFields = null } = options;
    return (
      <div className="auth-param-row" key={rowKey}>
        <Input
          className="auth-field-name"
          value={item.paramName}
          placeholder="参数名称"
          disabled={!editable}
          onChange={(e) => onParamNameChange(e.target.value)}
        />
        <Select className="auth-field-type" value="string" disabled>
          <Option value="string">string</Option>
        </Select>
        <Select
          className="auth-field-carrier"
          value={item.carrier || 'header'}
          disabled={!editable}
          onChange={onCarrierChange}
        >
          {AUTH_CARRIER_OPTIONS.map(opt => (
            <Option key={opt} value={opt}>{opt}</Option>
          ))}
        </Select>
        <Input
          className="auth-field-value"
          value={valueColumnValue}
          placeholder={valueColumnPlaceholder}
          disabled
        />
        {extraFields}
      </div>
    );
  };

  /**
   * 渲染单条通用认证参数行（SOA / APIG / Cookie）
   * @param {Object} options
   * options.authType 认证方式标识
   * options.item 参数对象
   * options.index 参数索引
   */
  const renderCommonAuthRow = (options) => {
    // 解构所需参数
    const { authType, item, index } = options;
    // 当前认证方式的参数行展示配置
    const rowConfig = AUTH_PARAM_ROW_CONFIG[authType] || {};
    // 值来源/字段映射列：优先使用配置值，否则使用参数固定值
    const valueColumnValue = Object.prototype.hasOwnProperty.call(rowConfig, 'value') ? rowConfig.value : (item.fixedValue || '');
    const valueColumnPlaceholder = rowConfig.valuePlaceholder || '值来源';

    return renderAuthParamRow({
      rowKey: `${authType}-${index}`,
      item,
      valueColumnValue,
      valueColumnPlaceholder,
      onParamNameChange: (value) => updateAuthParam({ authType, index, field: 'paramName', value }),
      onCarrierChange: (value) => updateAuthParam({ authType, index, field: 'carrier', value }),
    });
  };

  /**
   * 渲染通用认证子区块
   * @param {string} authType 认证方式标识
   */
  const renderCommonAuthBlock = (authType) => {
    // 当前认证方式下的参数列表
    const items = (apiConfig.authRequestSchema || {})[authType] || [];
    return (
      <div className="auth-sub-section" key={authType}>
        <div className="auth-sub-title">
          <span className="auth-sub-tag">{authType}</span>
          {AUTH_TYPE_NAMES[authType]}
        </div>
        {items.length === 0 ? (
          <div className="placeholder-text">暂无参数</div>
        ) : (
          items.map((item, index) => renderCommonAuthRow({ authType, item, index }))
        )}
      </div>
    );
  };

  /**
   * 渲染数字签名子区块（单行：参数名 + string + carrier + 固定值 + 密钥 + 显隐）
   */
  const renderSignatureBlock = () => {
    // 数字签名独立配置
    const sig = apiConfig.signatureConfig || {};
    // 数字签名参数行展示配置
    const rowConfig = AUTH_PARAM_ROW_CONFIG[SIGNATURE_AUTH_TYPE] || {};
    // 数字签名额外展示签名密钥和显隐按钮
    const extraFields = rowConfig.showSecret ? (
      <>
        <Input
          className="auth-field-value"
          type={signatureSecretMasked ? 'password' : 'text'}
          value={sig.secret}
          placeholder="签名密钥"
          disabled={!editable}
          onChange={(e) => handleSignatureChange('secret', e.target.value)}
        />
        <Button className="signature-mask-btn" onClick={toggleSecretMask}>
          {signatureSecretMasked ? '显示' : '隐藏'}
        </Button>
      </>
    ) : null;

    return (
      <div className="auth-sub-section" key="SIGNATURE">
        <div className="auth-sub-title">
          <span className="auth-sub-tag">数字签名</span>
          {AUTH_TYPE_NAMES[SIGNATURE_AUTH_TYPE]}
        </div>
        {renderAuthParamRow({
          rowKey: SIGNATURE_AUTH_TYPE,
          item: sig,
          valueColumnValue: sig.fixedValue || rowConfig.value,
          valueColumnPlaceholder: rowConfig.valuePlaceholder,
          onParamNameChange: (value) => handleSignatureChange('paramName', value),
          onCarrierChange: (value) => handleSignatureChange('carrier', value),
          extraFields,
        })}
      </div>
    );
  };

  return (
    <div className="section-card" ref={ref}>
      <div className="section-title">
        认证方式配置
        <span className="section-tip">支持多选，勾选后可分别维护参数</span>
      </div>

      <Form.Item label="认证方式（可多选）" className="form-item-spacing">
        <Checkbox.Group
          value={authTypes}
          disabled={!editable}
          onChange={handleAuthTypeChange}
        >
          {AUTH_TYPE_OPTIONS.map(opt => (
            <Checkbox key={opt.value} value={opt.value}>
              {opt.label}
            </Checkbox>
          ))}
        </Checkbox.Group>
      </Form.Item>

      {authTypes.length === 0 ? (
        <div className="placeholder-text">请先选择认证方式</div>
      ) : (
        <>
          {COMMON_AUTH_TYPES.filter(t => authTypes.includes(t)).map(renderCommonAuthBlock)}
          {authTypes.includes(SIGNATURE_AUTH_TYPE) && renderSignatureBlock()}
        </>
      )}
    </div>
  );
});

export default AuthSection;
