import { API_CONFIG, fetchApi } from '../../configs/web.config';

export const updateApp = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.UPDATE, {
      method: 'PUT',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const uploadImage = async (bizType, formData) => {
  try {
    const result = await fetchApi(API_CONFIG.FILE.UPLOAD_IMAGE, {
      method: 'POST',
      params: { bizType },
      body: formData,
      rawBody: true,
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
