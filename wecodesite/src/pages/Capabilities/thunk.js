import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchSubscribedAbilities = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_ABILITIES.SUBSCRIBED, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAbilityList = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_ABILITIES.LIST, { params: { appId } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const addAbility = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_ABILITIES.ADD, { appId });
    const result = await fetchApi(url, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppDetail = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.DETAIL, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};
