import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Card, Button, Form, Input, message, Spin, Tag, Upload, Modal, InputNumber, Space, Tooltip } from 'antd';
import { EyeInvisibleOutlined, EyeOutlined, CopyOutlined, EditOutlined, CloseOutlined, PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons';
import { useSelector } from 'react-redux';
import AuthMethodCard from '../../components/AuthMethodCard/AuthMethodCard';
import AppCredentials from '../../components/AppCredentials/AppCredentials';
import BasicInfoCard from '../../components/BasicInfoCard/BasicInfoCard';
import { fetchCurrentRole, updateApp, fetchAppIdentity, fetchVerifyType, updateVerifyType, bindEamap, fetchEamapList, uploadImage, fetchCardSetting, updateCardPeriod } from './thunk';
import { FILE_VALIDATION, VERIFY_TYPE_MAP } from '../../utils/constants';

import './BasicInfo.m.less';

function BasicInfo() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { appBaseInfo } = useSelector(state => state.app);
  const appId = searchParams.get('appId');

  // 卡片设置 state
  const [cardSetting, setCardSetting] = useState({
    expirationDays: null,
    deletionDays: null,
  });
  const [cardSettingEditing, setCardSettingEditing] = useState(false);
  const [cardSettingDraft, setCardSettingDraft] = useState({
    expiration: null,
    deletion: null,
  });
  const [cardSettingRowSaving, setCardSettingRowSaving] = useState({
    expiration: false,
    deletion: false,
  });

  useEffect(() => {
    if (!appId) {
      navigate('/');
    }
  }, [appId, navigate]);

  // 卡片设置：独立加载（数据源为卡片服务，非 appDetail）
  useEffect(() => {
    if (!appId) return;
    fetchCardSetting(appId).then((res) => {
      if (res?.code === '200' || res?.code === 200) {
        setCardSetting(res.data || { expirationDays: null, deletionDays: null });
      }
    });
  }, [appId]);


  // ========== 卡片设置 handlers ==========
  const CARD_FIELD_CONSTRAINTS = {
    expiration: { min: 1, max: 7, periodType: 1 },
    deletion: { min: 1, max: 30, periodType: 0 },
  };

  const clampToEditable = (v, field) => {
    if (v == null) return null;
    const { min, max } = CARD_FIELD_CONSTRAINTS[field];
    return Math.max(min, Math.min(max, Math.round(v)));
  };

  const handleCardSettingEdit = () => {
    setCardSettingDraft({
      expiration: clampToEditable(cardSetting.expirationDays, 'expiration') ?? 1,
      deletion: clampToEditable(cardSetting.deletionDays, 'deletion') ?? 1,
    });
    setCardSettingEditing(true);
  };

  const handleCardSettingCancel = () => {
    setCardSettingEditing(false);
    setCardSettingDraft({ expiration: null, deletion: null });
    setCardSettingRowSaving({ expiration: false, deletion: false });
  };

  const handleCardSettingSaveRow = async (field) => {
    const constraint = CARD_FIELD_CONSTRAINTS[field];
    const draftValue = cardSettingDraft[field];
    if (draftValue == null || draftValue < constraint.min || draftValue > constraint.max) {
      return;
    }
    setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: true });
    try {
      const result = await updateCardPeriod(appId, constraint.periodType, draftValue);
      if (result?.code === '200' || result?.code === 200) {
        message.success('保存成功');
        const fresh = await fetchCardSetting(appId);
        if (fresh?.code === '200' || fresh?.code === 200) {
          setCardSetting(fresh.data || { expirationDays: null, deletionDays: null });
        }
        setCardSettingDraft({ ...cardSettingDraft, [field]: null });
        const otherField = field === 'expiration' ? 'deletion' : 'expiration';
        if (cardSettingDraft[otherField] == null) {
          setCardSettingEditing(false);
        }
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } finally {
      setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
    }
  };

  if (!appBaseInfo) {
    return <Spin spinning={true} className="basic-info-loading" />;
  }

  return (
    <div className="basic-info-page">
      <AppCredentials />
      <BasicInfoCard />
      {appBaseInfo.appType !== 0 && <AuthMethodCard />}
        {/* 卡片设置 */}
        <Card
          className="info-card"
          title={
            <span className="card-title-with-action">
              卡片设置
              {!cardSettingEditing && (
                <Button type="link" icon={<EditOutlined />} onClick={handleCardSettingEdit}>
                  编辑
                </Button>
              )}
            </span>
          }
        >
          <div className="basic-info-view">
            <div className="info-row">
              <span className="info-label">
                定期失效时间
                <Tooltip title="根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互">
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
                </Tooltip>
              </span>
              <span className="info-value">
                {cardSettingDraft.expiration != null ? (
                  <Space>
                    <InputNumber
                      min={1}
                      max={7}
                      value={cardSettingDraft.expiration}
                      onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, expiration: v })}
                      disabled={cardSettingRowSaving.expiration}
                    />
                    <span>天</span>
                    <Button
                      type="primary"
                      size="small"
                      loading={cardSettingRowSaving.expiration}
                      onClick={() => handleCardSettingSaveRow('expiration')}
                    >
                      保存
                    </Button>
                  </Space>
                ) : (
                  <span>{cardSetting.expirationDays != null ? `${cardSetting.expirationDays} 天` : '— 天'}</span>
                )}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">
                定期删除时间
                <Tooltip title="只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除">
                  <QuestionCircleOutlined style={{ marginLeft: 4, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
                </Tooltip>
              </span>
              <span className="info-value">
                {cardSettingDraft.deletion != null ? (
                  <Space>
                    <InputNumber
                      min={1}
                      max={30}
                      value={cardSettingDraft.deletion}
                      onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, deletion: v })}
                      disabled={cardSettingRowSaving.deletion}
                    />
                    <span>天</span>
                    <Button
                      type="primary"
                      size="small"
                      loading={cardSettingRowSaving.deletion}
                      onClick={() => handleCardSettingSaveRow('deletion')}
                    >
                      保存
                    </Button>
                  </Space>
                ) : (
                  <span>{cardSetting.deletionDays != null ? `${cardSetting.deletionDays} 天` : '— 天'}</span>
                )}
              </span>
            </div>
          </div>
          {cardSettingEditing && (
            <div className="edit-form-row card-actions-row">
              <div className="edit-form-label"></div>
              <div className="edit-form-field">
                <div className="card-actions">
                  <Button onClick={handleCardSettingCancel}>取消</Button>
                </div>
              </div>
            </div>
          )}
        </Card>
     
    </div>
  );
}

export default BasicInfo;
