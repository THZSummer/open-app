import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Card, Checkbox, Input, message, Modal, Spin } from 'antd';
import { EditOutlined, WarningOutlined } from '@ant-design/icons';
import { VERIFY_TYPE_MAP, FORM_VALIDATION_RULES } from '../../utils/constants';
import { fetchAppDetail } from '../../store/appSlice';
import { fetchVerifyType, updateVerifyType } from '../../pages/BasicInfo/thunk';

import './AuthMethodCard.m.less';

/** SOAHeader(1) 与 SOAURL(3) 互斥的 key */
const MUTUAL_EXCLUSIVE = { 1: 3, 3: 1 };

/** 认证方式红色警告文案（spec FR-005） */
const AUTH_WARNING_TEXT = '认证方式切换后,将影响已发送卡片的数据回调,请谨慎选择。';

function AuthMethodCard() {
  const [searchParams] = useSearchParams();
  const dispatch = useDispatch();
  const appId = searchParams.get('appId');
  const [verifyType, setVerifyType] = useState([0]);
  const [apiSecret, setApiSecret] = useState('');
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(false);
  const [selectedTypes, setSelectedTypes] = useState([0]);
  const [secret, setSecret] = useState('');
  const [secretError, setSecretError] = useState('');

  useEffect(() => {
    if (!appId) return;
    setLoading(true);
    fetchVerifyType(appId)
      .then((result) => {
        if (result?.code === '200' && result.data) {
          const data = result.data;
          setVerifyType(data.verifyType || [0]);
          setApiSecret(data.apiSecret || '');
          setSelectedTypes(data.verifyType || [0]);
          setSecret(data.apiSecret || '');
        } else {
          message.error(result?.messageZh || '获取认证方式失败');
        }
      })
      .finally(() => setLoading(false));
  }, [appId]);

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

  const handleSave = async () => {
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
      onOk: async () => {
        const result = await updateVerifyType(appId, { verifyType: selectedTypes, apiSecret: secret });
        if (result?.code === '200') {
          message.success('认证方式保存成功');
          setEditing(false);
          setVerifyType(selectedTypes);
          setApiSecret(secret);
          dispatch(fetchAppDetail(appId));
        } else {
          message.error(result?.messageZh || '保存失败');
        }
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
    const hasDigitalSignature = verifyType.includes(2);
    return (
      <div className="auth-method-view">
        <div className="auth-form-row">
          <div className="auth-form-label"><span className="auth-form-required">*</span>认证方式：</div>
          <div className="auth-form-field">
            <Checkbox.Group
              options={options}
              value={verifyType}
              disabled
              className="auth-method-checkboxes auth-method-checkboxes-readonly"
            />
          </div>
        </div>
        {hasDigitalSignature && apiSecret && (
          <div className="auth-form-row">
            <div className="auth-form-label"><span className="auth-form-required">*</span> apiSecret：</div>
            <div className="auth-form-field">
              <span className="secret-view-text">{maskSecret(apiSecret)}</span>
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
          <span className="btn-cancel" onClick={() => { setEditing(false); setSelectedTypes(verifyType); setSecret(apiSecret); setSecretError(''); }}>取消</span>
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
            <span className="edit-link" onClick={() => { setSelectedTypes(verifyType); setSecret(apiSecret); setSecretError(''); setEditing(true); }}>
              <EditOutlined /> 配置
            </span>
          )}
        </span>
      }
    >
      <Spin spinning={loading}>
        {editing ? renderEditMode() : renderViewMode()}
      </Spin>
    </Card>
  );
}

export default AuthMethodCard;
