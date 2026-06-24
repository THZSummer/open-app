import { API_CONFIG, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchCurrentRole = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.CURRENT_ROLE, { params: { appId } });
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

export const fetchAppIdentity = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.IDENTITY, { params: { appId } });
    return result || {};
  } catch (err) {
    return {};
  }
};

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

export const fetchEamapList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.EAMAP_LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

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

export const uploadImage = async (bizType, formData) => {
  try {
    const result = await fetchApi(API_CONFIG.FILE.UPLOAD_IMAGE, {
      method: 'POST',
      params: { bizType },
      body: formData,  // 直接传 FormData，不走 JSON.stringify
      rawBody: true,   // 告诉 fetchApi 不要自动 JSON.stringify
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
