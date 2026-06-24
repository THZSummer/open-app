import { mockAppInfo } from './mock';
import { mockApps } from '../AppList/mock';
import { fetchApi, buildApiUrl, API_CONFIG } from '../../configs/web.config';
import { message } from 'antd';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchAppInfo = async (appId) => {
  await delay(300);
  return mockAppInfo[appId] || null;
};

export const bindEamapToApp = async (appId, eamap) => {
  await delay(300);
  if (mockAppInfo[appId]) {
    mockAppInfo[appId].eamap = eamap;
  }
  const appInList = mockApps.find((a) => a.id === appId);
  if (appInList) {
    appInList.eamap = eamap;
  }
  return { appId, eamap };
};

/**
 * #TBD-CS01 查询应用卡片设置（失效周期 + 删除周期）
 * @param {string} appId
 * @returns {Promise<{expirationDays: number|null, deletionDays: number|null}|null>}
 */
export const fetchCardSetting = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const res = await fetchApi(url, { method: 'GET' });
    if (res?.code === '200' || res?.code === 200) {
      return res.data;
    }
    message.error(res?.messageZh || res?.message || '查询卡片设置失败');
    return null;
  } catch (err) {
    console.error('fetchCardSetting error:', err);
    message.error('查询卡片设置失败');
    return null;
  }
};

/**
 * #TBD-CS02 更新单个卡片周期（失效或删除）
 * @param {string} appId
 * @param {0|1} periodType - 0=删除周期, 1=失效周期
 * @param {number} periodDays - 周期天数
 * @returns {Promise<{success: boolean, message?: string}>}
 */
export const updateCardPeriod = async (appId, periodType, periodDays) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const res = await fetchApi(url, {
      method: 'PUT',
      body: JSON.stringify({ periodType, periodDays }),
    });
    if (res?.code === '200' || res?.code === 200) {
      return { success: true };
    }
    return {
      success: false,
      message: res?.messageZh || res?.message || '保存失败',
    };
  } catch (err) {
    console.error('updateCardPeriod error:', err);
    return { success: false, message: '卡片服务暂时不可用' };
  }
};