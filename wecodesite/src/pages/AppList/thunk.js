/**
 * ========================================
 * 应用列表 - API接口
 * ========================================
 *
 * 提供应用列表、创建应用、EAMAP列表、图标列表等接口调用
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 获取应用列表
 * @param {Object} params - 查询参数
 * @param {number} params.curPage - 当前页码
 * @param {number} params.pageSize - 每页条数
 * @returns {Promise<Object>} 应用列表数据
 */
export const fetchAppList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取默认图标列表
 * @returns {Promise<Object>} 图标列表数据
 */
export const fetchDefaultIcons = async () => {
  try {
    const result = await fetchApi(API_CONFIG.APP.ICONS);
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取 EAMAP 应用服务列表
 * @param {Object} params - 查询参数
 * @returns {Promise<Object>} EAMAP 列表数据
 */
export const fetchEamapOptions = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.EAMAP_LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建应用
 * @param {Object} data - 应用数据
 * @returns {Promise<Object>} 创建结果
 */
export const createApp = async (data = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.CREATE, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 上传图片
 * @param {string} bizType - 业务类型
 * @param {FormData} formData - 文件表单数据
 * @returns {Promise<Object>} 上传结果
 */
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

/**
 * 获取应用详情
 * @param {string} appId - 应用ID
 * @returns {Promise<Object>} 应用详情数据
 */
export const fetchAppById = async (appId) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP.DETAIL, { appId }), {
      method: 'GET',
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 绑定 EAMAP 应用服务
 * @param {string} appId - 应用ID
 * @param {Object} data - 绑定数据
 * @param {string} data.eamapAppCode - EAMAP 编码
 * @returns {Promise<Object>} 绑定结果
 */
export const bindEamap = async (appId, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.APP.BIND_EAMAP, { appId }), {
      method: 'POST',
      body: JSON.stringify({ eamapAppCode: data.eamapAppCode }),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
