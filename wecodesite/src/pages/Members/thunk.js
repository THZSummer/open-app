import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchMemberList = async (appId, params = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_MEMBERS.LIST, { appId });
    const result = await fetchApi(url, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const searchUsers = async (appId, keyword) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_MEMBERS.SEARCH_USERS, { appId });
    const result = await fetchApi(url, { params: { keyword } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const addMembers = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_MEMBERS.ADD, { appId });
    const result = await fetchApi(url, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteMember = async (appId, id) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_MEMBERS.DELETE, { appId, id });
    const result = await fetchApi(url, { method: 'DELETE' });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const transferOwner = async (appId, data = {}) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_MEMBERS.TRANSFER_OWNER, { appId });
    const result = await fetchApi(url, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

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
