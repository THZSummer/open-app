import { API_CONFIG, fetchApi } from '../../../configs/web.config';

export const bindEamap = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.BIND_EAMAP, {
      method: 'POST',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
