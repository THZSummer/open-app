import { API_CONFIG, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchVersionList = async (appId, params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.LIST, { params: { appId, ...params } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const createVersion = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.CREATE, {
      method: 'POST',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchVersionDetail = async (appId, versionId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.DETAIL, { params: { appId, versionId } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const publishVersion = async (appId, versionId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.PUBLISH, {
      method: 'POST',
      params: { appId, versionId },
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const withdrawVersion = async (appId, versionId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.WITHDRAW, {
      method: 'POST',
      params: { appId, versionId },
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteVersion = async (appId, versionId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.DELETE, {
      method: 'DELETE',
      params: { appId, versionId },
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const updateVersion = async (appId, versionId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_VERSIONS.UPDATE, {
      method: 'PUT',
      params: { appId, versionId },
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
