import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Card, Button, InputNumber, Tooltip, message } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { fetchCardSetting, updateCardPeriod } from '../../pages/BasicInfo/thunk';

import './CardSettings.m.less';

function CardSettings() {
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId');

  const [cardSetting, setCardSetting] = useState({
    expirationDays: null,
    deletionDays: null,
  });
  const [cardSettingRowEditing, setCardSettingRowEditing] = useState({
    expiration: false,
    deletion: false,
  });
  const [cardSettingDraft, setCardSettingDraft] = useState({
    expiration: null,
    deletion: null,
  });
  const [cardSettingRowSaving, setCardSettingRowSaving] = useState({
    expiration: false,
    deletion: false,
  });

  useEffect(() => {
    if (!appId) return;
    fetchCardSetting(appId).then((res) => {
      if (res?.code === '200' || res?.code === 200) {
        setCardSetting(res.data || { expirationDays: null, deletionDays: null });
      }
    });
  }, [appId]);

  const CARD_FIELD_CONSTRAINTS = {
    expiration: { min: 1, max: 7, periodType: 1 },
    deletion: { min: 1, max: 30, periodType: 0 },
  };

  const clampToEditable = (v, field) => {
    if (v == null) return null;
    const { min, max } = CARD_FIELD_CONSTRAINTS[field];
    return Math.max(min, Math.min(max, Math.round(v)));
  };

  const handleRowEdit = (field) => {
    setCardSettingDraft({
      ...cardSettingDraft,
      [field]: clampToEditable(
        field === 'expiration' ? cardSetting.expirationDays : cardSetting.deletionDays,
        field
      ) ?? 1,
    });
    setCardSettingRowEditing({ ...cardSettingRowEditing, [field]: true });
  };

  const handleRowCancel = (field) => {
    setCardSettingDraft({ ...cardSettingDraft, [field]: null });
    setCardSettingRowEditing({ ...cardSettingRowEditing, [field]: false });
    setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
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
        setCardSettingRowEditing({ ...cardSettingRowEditing, [field]: false });
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } finally {
      setCardSettingRowSaving({ ...cardSettingRowSaving, [field]: false });
    }
  };

  return (
    <Card className="card-setting-card" title="卡片设置">
      <div className="basic-info-view">
        <div className="info-row">
          <span className="info-label">
            定期失效时间
            <Tooltip title="根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互">
              <QuestionCircleOutlined style={{ marginLeft: 4, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
            </Tooltip>
          </span>
          <span className="info-value">
            {cardSettingRowEditing.expiration ? (
              <>
                <InputNumber
                  min={1}
                  max={7}
                  value={cardSettingDraft.expiration}
                  onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, expiration: v })}
                  disabled={cardSettingRowSaving.expiration}
                />
                <span>天</span>
                <span className="card-setting-actions">
                  <Button
                    type="link"
                    loading={cardSettingRowSaving.expiration}
                    onClick={() => handleCardSettingSaveRow('expiration')}
                  >
                    保存
                  </Button>
                  <Button type="link" onClick={() => handleRowCancel('expiration')}>取消</Button>
                </span>
              </>
            ) : (
              <>
                <span>{cardSetting.expirationDays != null ? `${cardSetting.expirationDays} 天` : '— 天'}</span>
                <Button type="link" onClick={() => handleRowEdit('expiration')}>
                  修改
                </Button>
              </>
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
            {cardSettingRowEditing.deletion ? (
              <>
                <InputNumber
                  min={1}
                  max={30}
                  value={cardSettingDraft.deletion}
                  onChange={(v) => setCardSettingDraft({ ...cardSettingDraft, deletion: v })}
                  disabled={cardSettingRowSaving.deletion}
                />
                <span>天</span>
                <span className="card-setting-actions">
                  <Button
                    type="link"
                    loading={cardSettingRowSaving.deletion}
                    onClick={() => handleCardSettingSaveRow('deletion')}
                  >
                    保存
                  </Button>
                  <Button type="link" onClick={() => handleRowCancel('deletion')}>取消</Button>
                </span>
              </>
            ) : (
              <>
                <span>{cardSetting.deletionDays != null ? `${cardSetting.deletionDays} 天` : '— 天'}</span>
                <Button type="link" onClick={() => handleRowEdit('deletion')}>
                  修改
                </Button>
              </>
            )}
          </span>
        </div>
      </div>
    </Card>
  );
}

export default CardSettings;
