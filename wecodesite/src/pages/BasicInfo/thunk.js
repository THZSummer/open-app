import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchCurrentRole = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.CURRENT_ROLE, { appId });
    const result = await fetchApi(url);
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

export const updateApp = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.UPDATE, { appId });
    const result = await fetchApi(url, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchAppIdentity = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.IDENTITY, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchVerifyType = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.VERIFY_TYPE_GET, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

export const updateVerifyType = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP.VERIFY_TYPE_UPDATE, { appId });
    const result = await fetchApi(url, {
      method: 'PUT',
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
    const url = buildApiUrl(API_CONFIG.APP.BIND_EAMAP, { appId });
    const result = await fetchApi(url, {
      method: 'POST',
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
