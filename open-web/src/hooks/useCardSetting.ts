import { useState } from 'react';
import { message } from 'antd';
import {
  getCardSetting,
  updateCardPeriod,
  CardSetting,
  PeriodType,
} from '@/services/card.service';

/** 字段名 */
export type FieldName = 'expiration' | 'deletion';

/** 单行状态机 */
interface FieldState {
  /** UI 模式：readonly=只读态，editing=编辑态 */
  mode: 'readonly' | 'editing';
  /** 编辑态 InputNumber 当前值（可能为 null，表示未输入） */
  editValue: number | null;
  /** 是否正在保存 */
  saving: boolean;
}

/** 每行的约束（按 periodType 决定） */
const FIELD_CONSTRAINTS: Record<FieldName, { min: number; max: number; periodType: PeriodType }> = {
  expiration: { min: 1, max: 7, periodType: 1 },
  deletion: { min: 1, max: 30, periodType: 0 },
};

/** 初始 FieldState */
const INITIAL_FIELD_STATE: FieldState = {
  mode: 'readonly',
  editValue: null,
  saving: false,
};

/**
 * 进入编辑态的裁剪规则
 *
 * - displayed === null → null（显示 placeholder）
 * - displayed < min   → min
 * - displayed > max   → max
 * - min ≤ displayed ≤ max → displayed
 */
function clampToEditable(displayed: number | null, min: number, max: number): number | null {
  if (displayed === null || displayed === undefined) return null;
  if (displayed < min) return min;
  if (displayed > max) return max;
  return displayed;
}

/**
 * 卡片设置 hook
 *
 * 管理板块级状态：查询数据、两行独立状态机（只读↔编辑态）。
 * 错误提示由 request.ts 拦截器统一处理，本 hook 不重复 message.error。
 *
 * 注：导出名 useCardSetting（不与 React 内置 hook 冲突）。
 */
export const useCardSetting = () => {
  const [loading, setLoading] = useState(false);
  const [cardSetting, setCardSetting] = useState<CardSetting | null>(null);

  // 每行独立状态机
  const [expirationState, setExpirationState] = useState<FieldState>(INITIAL_FIELD_STATE);
  const [deletionState, setDeletionState] = useState<FieldState>(INITIAL_FIELD_STATE);

  const getState = (field: FieldName) =>
    field === 'expiration' ? expirationState : deletionState;
  const setState = (field: FieldName) =>
    field === 'expiration' ? setExpirationState : setDeletionState;

  const getDisplayed = (field: FieldName): number | null => {
    if (!cardSetting) return null;
    return field === 'expiration' ? cardSetting.expirationDays : cardSetting.deletionDays;
  };

  /**
   * 查询卡片设置
   *
   * 进入板块 / 保存成功后调用。成功时回填 cardSetting，并把两行都切回只读态。
   */
  const fetchCardSetting = async (appId: string) => {
    setLoading(true);
    try {
      const response = await getCardSetting(appId);
      const data = response.data.data;
      setCardSetting(data);
      // 切回只读态（编辑值清空）
      setExpirationState({ ...INITIAL_FIELD_STATE });
      setDeletionState({ ...INITIAL_FIELD_STATE });
      return data;
    } catch (error) {
      // request.ts 拦截器已自动 toast，这里只记日志
      console.error('查询卡片设置失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  };

  /**
   * 进入编辑态
   *
   * 按裁剪规则把当前展示值（可能超出可写范围）映射为编辑态初始值。
   */
  const enterEditMode = (field: FieldName) => {
    const { min, max } = FIELD_CONSTRAINTS[field];
    const displayed = getDisplayed(field);
    const editValue = clampToEditable(displayed, min, max);
    setState(field)({ mode: 'editing', editValue, saving: false });
  };

  /**
   * 设置编辑值（用户输入）
   *
   * - val === null → editValue=null（显示 placeholder）
   * - val 越界 → 裁剪到 [min, max]
   */
  const setEditValue = (field: FieldName, val: number | null) => {
    const state = getState(field);
    if (state.mode !== 'editing' || state.saving) return;

    const { min, max } = FIELD_CONSTRAINTS[field];
    let newEditValue: number | null = null;
    if (val !== null && !isNaN(val)) {
      newEditValue = Math.max(min, Math.min(max, Math.round(val)));
    }
    setState(field)({ ...state, editValue: newEditValue });
  };

  /**
   * 保存当前字段
   *
   * 流程：PUT → 成功 → 重新 GET → 切回只读态 → message.success
   * 失败：request.ts 拦截器自动 toast；本 hook 保留当前输入值，停留在编辑态。
   */
  const saveField = async (appId: string, field: FieldName) => {
    const state = getState(field);
    if (state.mode !== 'editing' || state.saving || state.editValue === null) return;

    const { periodType } = FIELD_CONSTRAINTS[field];
    setState(field)({ ...state, saving: true });

    try {
      await updateCardPeriod(appId, {
        periodDays: state.editValue,
        periodType,
      });
      // 保存成功后重新 GET 回填
      await fetchCardSetting(appId);
      message.success('保存成功');
    } catch (error) {
      // request.ts 拦截器已自动 toast；这里只记日志
      console.error('保存失败:', error);
    } finally {
      setState(field)((prev) => ({ ...prev, saving: false }));
    }
  };

  /**
   * 取消编辑
   *
   * 切回只读态，展示修改前的值（即 cardSetting 中的 displayed 值）。
   */
  const cancelEdit = (field: FieldName) => {
    const state = getState(field);
    if (state.mode !== 'editing' || state.saving) return;
    setState(field)({ ...INITIAL_FIELD_STATE });
  };

  /**
   * 派生：当前字段是否可保存
   *
   * 可保存 = 编辑态 + 已修改 + 值合法 + 未保存中
   */
  const canSave = (field: FieldName): boolean => {
    const state = getState(field);
    if (state.mode !== 'editing' || state.saving || state.editValue === null) return false;
    const displayed = getDisplayed(field);
    const isDirty = state.editValue !== displayed;
    const { min, max } = FIELD_CONSTRAINTS[field];
    const isValid = state.editValue >= min && state.editValue <= max;
    return isDirty && isValid;
  };

  return {
    loading,
    cardSetting,
    expirationState,
    deletionState,
    fetchCardSetting,
    enterEditMode,
    setEditValue,
    saveField,
    cancelEdit,
    canSave,
  };
};
