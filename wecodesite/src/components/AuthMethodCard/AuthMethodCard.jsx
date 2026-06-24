import React, { useState, useEffect } from 'react';
import { Card, Checkbox, Input, message, Modal } from 'antd';
import { EditOutlined, WarningOutlined } from '@ant-design/icons';
import { VERIFY_TYPE_MAP, FORM_VALIDATION_RULES } from '../../utils/constants';

import './AuthMethodCard.m.less';

/** SOAHeader(1) 与 SOAURL(3) 互斥的 key */
const MUTUAL_EXCLUSIVE = { 1: 3, 3: 1 };

/** 认证方式红色警告文案（spec FR-005） */
const AUTH_WARNING_TEXT = '认证方式切换后,将影响已发送卡片的数据回调,请谨慎选择。';

/**
 * 认证方式卡片组件
 *
 * @param {number[]} value - 当前选中的认证方式列表
 * @param {string} apiSecret - 数字签名 apiSecret
 * @param {boolean} editing - 是否处于编辑态
 * @param {Function} onEdit - 点击配置按钮回调
 * @param {Function} onSave - 保存回调 (selectedTypes, apiSecret) => void
 * @param {Function} onCancel - 取消回调
 */
function AuthMethodCard({ value = [0], apiSecret = '', editing = false, onEdit, onSave, onCancel }) {
  const [selectedTypes, setSelectedTypes] = useState(value);
  const [secret, setSecret] = useState(apiSecret);
  const [secretError, setSecretError] = useState('');

  // 脱敏展示：只显示前4位和后4位，中间用 * 填充
  const maskSecret = (s) => {
    if (!s) return '';
    if (s.length <= 8) return s.slice(0, 2) + '****' + s.slice(-2);
    return s.slice(0, 4) + '********' + s.slice(-4);
  };

  // 外部 value 变化时同步内部状态
  useEffect(() => {
    setSelectedTypes(value);
  }, [value]);

  useEffect(() => {
    setSecret(apiSecret);
  }, [apiSecret]);

  const handleTypeChange = (checkedValues) => {
    // SOAHeader(1) 与 SOAURL(3) 互斥：选中其中一个时自动取消另一个
    let finalValues = checkedValues;
    // 找出新增的互斥项
    const prevSet = new Set(selectedTypes);
    const addedItems = checkedValues.filter((v) => !prevSet.has(v));
    for (const item of addedItems) {
      if (MUTUAL_EXCLUSIVE[item] !== undefined && checkedValues.includes(MUTUAL_EXCLUSIVE[item])) {
        // 移除互斥的另一个
        finalValues = finalValues.filter((v) => v !== MUTUAL_EXCLUSIVE[item]);
      }
    }

    setSelectedTypes(finalValues);
    if (!finalValues.includes(2)) {
      setSecret('');
      setSecretError('');
    }
  };

  const handleSecretChange = (e) => {
    const val = e.target.value;
    setSecret(val);
    if (val && !FORM_VALIDATION_RULES.apiSecret.pattern.test(val)) {
      setSecretError(FORM_VALIDATION_RULES.apiSecret.message);
    } else {
      setSecretError('');
    }
  };

  const handleSave = () => {
    if (selectedTypes.length === 0) {
      message.error('请至少选择 1 种认证方式');
      return;
    }
    if (selectedTypes.includes(2)) {
      if (!secret) {
        setSecretError('数字签名必须输入 apiSecret');
        return;
      }
      if (!FORM_VALIDATION_RULES.apiSecret.pattern.test(secret)) {
        setSecretError(FORM_VALIDATION_RULES.apiSecret.message);
        return;
      }
    }
    // 二次确认弹窗
    Modal.confirm({
      title: '提示',
      content: '确定要修改认证方式吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => {
        onSave(selectedTypes, secret);
      },
    });
  };

  const renderViewMode = () => {
    const options = Object.entries(VERIFY_TYPE_MAP)
      .sort(([, a], [, b]) => (a.order ?? 0) - (b.order ?? 0))
      .map(([key, item]) => ({
        label: item.label || item.text,
        value: Number(key),
      }));
    const hasDigitalSignature = value.includes(2);
    return (
      <div className="auth-method-view">
        <div className="auth-form-row">
          <div className="auth-form-label"><span className="auth-form-required">*</span>认证方式：</div>
          <div className="auth-form-field">
            <Checkbox.Group
              options={options}
              value={value}
              disabled
              className="auth-method-checkboxes auth-method-checkboxes-readonly"
            />
          </div>
        </div>
        {hasDigitalSignature && apiSecret && (
          <div className="auth-form-row">
            <div className="auth-form-label"><span className="auth-form-required">*</span> apiSecret：</div>
            <div className="auth-form-field">
              <span className="secret-view-text">{apiSecret}</span>
            </div>
          </div>
        )}
      </div>
    );
  };

  const renderEditMode = () => {
    const options = Object.entries(VERIFY_TYPE_MAP)
      .sort(([, a], [, b]) => (a.order ?? 0) - (b.order ?? 0))
      .map(([key, item]) => ({
        label: item.label || item.text,
        value: Number(key),
      }));

    const showSecretInput = selectedTypes.includes(2);

    return (
      <div className="auth-method-edit">
        <div className="auth-warning"><WarningOutlined /> {AUTH_WARNING_TEXT}</div>
        <div className="auth-form-row">
          <div className="auth-form-label"><span className="auth-form-required">*</span>认证方式：</div>
          <div className="auth-form-field">
            <Checkbox.Group
              options={options}
              value={selectedTypes}
              onChange={handleTypeChange}
              className="auth-method-checkboxes"
            />
          </div>
        </div>
        {showSecretInput && (
          <div className="auth-form-row">
            <div className="auth-form-label"><span className="auth-form-required">*</span>apiSecret：</div>
            <div className="auth-form-field">
              <Input
                placeholder="请输入16位字母+数字"
                value={secret}
                onChange={handleSecretChange}
                maxLength={16}
                status={secretError ? 'error' : undefined}
              />
              {secretError && <div className="secret-error">{secretError}</div>}
            </div>
          </div>
        )}
        <div className="auth-form-actions">
          <span className="btn-cancel" onClick={onCancel}>取消</span>
          <span className="btn-save" onClick={handleSave}>保存</span>
        </div>
      </div>
    );
  };

  return (
    <Card
      className="auth-method-card"
      title={
        <span className="card-title-with-action">
          认证方式
          {!editing && (
            <span className="edit-link" onClick={onEdit}>
              <EditOutlined /> 配置
            </span>
          )}
        </span>
      }
    >
      {editing ? renderEditMode() : renderViewMode()}
    </Card>
  );
}

export default AuthMethodCard;
