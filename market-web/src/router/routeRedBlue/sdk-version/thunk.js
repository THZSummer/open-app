/**
 * SDK版本管理 API 接口
 */
import API_CONFIG from '../../../configs/web.config';
import { buildApiUrl, fetchApi } from '../../../utils/webFetch';

/**
 * 获取SDK版本列表
 */
export const getSdkVersionList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.SDK_VERSION_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取SDK版本详情
 */
export const getSdkVersionDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.SDK_VERSION_DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 上架新版本
 */
export const createSdkVersion = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.SDK_VERSION_CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 编辑版本
 */
export const updateSdkVersion = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.SDK_VERSION_UPDATE, { id }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 废弃/恢复
 */
export const updateSdkVersionStatus = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.SDK_VERSION_UPDATE_STATUS, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询SDK权限列表
 */
export const getSdkPermissions = async () => {
  try {
    const result = await fetchApi(API_CONFIG.SDK_PERMISSIONS);
    return result || {};
  } catch (err) {
    return {};
  }
};
