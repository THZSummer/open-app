import { API_CONFIG, fetchApi } from '../../configs/web.config';

// ==================== 真实 API 调用 ====================

export const fetchMemberList = async (appId, params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_MEMBERS.LIST, { params: { appId, ...params } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const searchUsers = async (appId, keyword) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_MEMBERS.SEARCH_USERS, { params: { appId, keyword } });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const addMembers = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_MEMBERS.ADD, {
      method: 'POST',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const deleteMember = async (appId, id) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_MEMBERS.DELETE, {
      method: 'DELETE',
      params: { appId, id },
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const transferOwner = async (appId, data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_MEMBERS.TRANSFER_OWNER, {
      method: 'POST',
      params: { appId },
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

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
