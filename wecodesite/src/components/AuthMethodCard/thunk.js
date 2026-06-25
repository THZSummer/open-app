import { API_CONFIG, fetchApi } from '../../configs/web.config';

export const fetchVerifyType = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.VERIFY_TYPE_GET, { params: { appId } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const updateVerifyType = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.VERIFY_TYPE_UPDATE, {
      method: 'PUT',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
