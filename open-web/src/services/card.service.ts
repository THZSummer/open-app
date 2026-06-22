import { get, put } from '@/utils/request';

// ==================== 类型定义 ====================

/**
 * 卡片设置查询响应
 *
 * 对应后端 CardSettingResponse：
 * - expirationDays: 定期失效时间（天），null 表示卡片服务未配置；可能为任意整数（如系统默认 14）
 * - deletionDays: 定期删除时间（天），null 表示卡片服务未配置；可能为任意整数（如系统默认 7）
 */
export interface CardSetting {
  expirationDays: number | null;
  deletionDays: number | null;
}

/**
 * 卡片周期类型
 * - 0: 定期删除周期（范围 1~30）
 * - 1: 定期失效周期（范围 1~7）
 */
export type PeriodType = 0 | 1;

/**
 * 卡片周期更新请求（失效/删除合并为单一接口）
 *
 * - periodDays: 周期天数（失效：1~7；删除：1~30）
 * - periodType: 0=删除周期，1=失效周期
 */
export interface UpdateCardPeriodRequest {
  periodDays: number;
  periodType: PeriodType;
}

// ==================== API 函数 ====================

/**
 * #TBD-CS01 查询卡片设置
 *
 * GET /apps/{appId}/card-settings
 */
export const getCardSetting = (appId: string) =>
  get<CardSetting>(`/apps/${appId}/card-settings`);

/**
 * #TBD-CS02 更新卡片周期（失效/删除合并）
 *
 * PUT /apps/{appId}/card-settings
 * body: { periodDays, periodType }
 *
 * 注：open-server 这边字段名为 periodDays，由后端在转发卡片服务时映射为卡片服务的 period 字段。
 */
export const updateCardPeriod = (appId: string, data: UpdateCardPeriodRequest) =>
  put<void>(`/apps/${appId}/card-settings`, data);
