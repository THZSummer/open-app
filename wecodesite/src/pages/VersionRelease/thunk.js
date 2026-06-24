import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchVersionList = async (appId, params = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.LIST, { appId });
    const result = await fetchApi(url, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const createVersion = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.CREATE, { appId });
    const result = await fetchApi(url, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchVersionDetail = async (appId, versionId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.DETAIL, { appId, versionId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

export const publishVersion = async (appId, versionId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.PUBLISH, { appId, versionId });
    const result = await fetchApi(url, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawVersion = async (appId, versionId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.WITHDRAW, { appId, versionId });
    const result = await fetchApi(url, { method: 'POST' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteVersion = async (appId, versionId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.DELETE, { appId, versionId });
    const result = await fetchApi(url, { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const updateVersion = async (appId, versionId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_VERSIONS.UPDATE, { appId, versionId });
    const result = await fetchApi(url, {
      method: 'PUT',
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
