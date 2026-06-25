import { API_CONFIG, fetchApi, buildApiUrl } from '../../configs/web.config';

export const fetchCardSetting = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

export const updateCardPeriod = async (appId, periodType, periodDays) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const result = await fetchApi(url, {
      method: 'PUT',
      body: JSON.stringify({ periodType, periodDays }),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
