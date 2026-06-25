import React, { useEffect } from 'react';
import { Spin, Tooltip, InputNumber, Space, Button } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import { useCardSetting, FieldName } from '@/hooks/useCardSetting';
import styles from './CardSettingSection.module.less';

/**
 * 卡片设置板块 Props
 *
 * appId：当前应用的外部业务 ID（长 ID），由应用详情 page 透传。
 */
export interface CardSettingSectionProps {
  appId: string;
}

/** 字段配置（标签 / Tooltip / 约束） */
const FIELD_CONFIG: Record<
  FieldName,
  { label: string; tooltip: string; min: number; max: number; placeholder: string }
> = {
  expiration: {
    label: '定期失效时间',
    tooltip:
      '根据每张消息卡片第一次投放时间开始计算，系统按设置的时间自动对卡片进行失效，失效的卡片在端侧不再支持交互',
    min: 1,
    max: 7,
    placeholder: '请输入 1~7',
  },
  deletion: {
    label: '定期删除时间',
    tooltip:
      '只有失效的卡片可以删除，根据每张消息卡片失效时间开始计算，系统按照设置的时间自动对卡片进行删除',
    min: 1,
    max: 30,
    placeholder: '请输入 1~30',
  },
};

/**
 * 卡片设置板块
 *
 * 两态 UI：
 * - 只读态：`X 天 [修改]`
 * - 编辑态：`[InputNumber] 天 [保存] [取消]`
 *
 * 间距约束（对齐 antd `<Space>` 默认 8px）：
 * - 只读态：`天` ↔ `[修改]`
 * - 编辑态：`天` ↔ `[保存]`、`[保存]` ↔ `[取消]`
 */
const CardSettingSection: React.FC<CardSettingSectionProps> = ({ appId }) => {
  const {
    loading,
    expirationState,
    deletionState,
    fetchCardSetting,
    enterEditMode,
    setEditValue,
    saveField,
    cancelEdit,
    canSave,
    cardSetting,
  } = useCardSetting();

  // 板块挂载时加载数据
  useEffect(() => {
    if (appId) {
      fetchCardSetting(appId);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [appId]);

  const getState = (field: FieldName) =>
    field === 'expiration' ? expirationState : deletionState;

  const getDisplayed = (field: FieldName): number | null => {
    if (!cardSetting) return null;
    return field === 'expiration' ? cardSetting.expirationDays : cardSetting.deletionDays;
  };

  const renderRow = (field: FieldName) => {
    const config = FIELD_CONFIG[field];
    const state = getState(field);
    const displayed = getDisplayed(field);

    return (
      <div key={field} className={styles.settingRow}>
        <div className={styles.settingLabel}>
          {config.label}
          <Tooltip title={config.tooltip}>
            <QuestionCircleOutlined className={styles.tipIcon} />
          </Tooltip>
        </div>

        {state.mode === 'readonly' ? (
          // 只读态：`X 天 [修改]`
          <Space size={8}>
            <span className={styles.readonlyValue}>
              {displayed === null ? (
                <span className={styles.placeholder}>—</span>
              ) : (
                <>
                  <span className={styles.num}>{displayed}</span> 天
                </>
              )}
            </span>
            <Button type="link" size="small" onClick={() => enterEditMode(field)}>
              修改
            </Button>
          </Space>
        ) : (
          // 编辑态：`[InputNumber] 天 [保存] [取消]`
          <Space size={8}>
            <InputNumber
              min={config.min}
              max={config.max}
              value={state.editValue ?? undefined}
              placeholder={config.placeholder}
              disabled={state.saving}
              onChange={(val) => setEditValue(field, val === null ? null : Number(val))}
              style={{ width: 100 }}
            />
            <span className={styles.unit}>天</span>
            <Button
              type="primary"
              disabled={!canSave(field) || state.saving}
              loading={state.saving}
              onClick={() => saveField(appId, field)}
            >
              保存
            </Button>
            <Button disabled={state.saving} onClick={() => cancelEdit(field)}>
              取消
            </Button>
          </Space>
        )}
      </div>
    );
  };

  return (
    <div className={styles.cardSettingSection}>
      <div className={styles.sectionTitle}>卡片设置</div>
      <Spin spinning={loading}>
        <div className={styles.sectionBody}>
          {renderRow('expiration')}
          {renderRow('deletion')}
        </div>
      </Spin>
    </div>
  );
};

export default CardSettingSection;
