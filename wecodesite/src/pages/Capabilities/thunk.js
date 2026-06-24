import { API_CONFIG, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchSubscribedAbilities = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_ABILITIES.SUBSCRIBED, { params: { appId } });
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
    const result = await fetchApi(API_CONFIG.APP_ABILITIES.ADD, {
      method: 'POST',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppDetail = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.DETAIL, { params: { appId } });
    return result || {};
  } catch (err) {
    return {};
  }
};
