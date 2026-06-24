import { API_CONFIG, fetchApi } from '../../configs/web.config';

/**
 * 获取能力详情（从已订阅能力列表中查找）
 * 后端接口: GET /service/open/v2/ability/subscribed?appId=xxx
 */
export const fetchCapabilityDetail = async (appId, abilityType) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_ABILITIES.SUBSCRIBED, { params: { appId } });
    if (result?.code === '200' && Array.isArray(result.data)) {
      const ability = result.data.find(a => a.abilityType === Number(abilityType));
      return ability || null;
    }
    return null;
  } catch (err) {
    console.error('fetchCapabilityDetail error:', err);
    return null;
  }
};

/**
 * 获取能力列表（含所有能力的详情）
 * 后端接口: GET /service/open/v2/ability/list?appId=xxx
 */
export const fetchAbilityListForDetail = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_ABILITIES.LIST, { params: { appId } });
    if (result?.code === '200') {
      return result.data || [];
    }
    return [];
  } catch (err) {
    console.error('fetchAbilityListForDetail error:', err);
    return [];
  }
};
