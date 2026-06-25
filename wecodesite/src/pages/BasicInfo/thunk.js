import { API_CONFIG, fetchApi, buildApiUrl } from '../../configs/web.config';

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

// ==================== 卡片设置（数据源：卡片服务） ====================

/**
 * #TBD-CS01 查询应用卡片设置（失效周期 + 删除周期）
 * @param {string} appId
 * @returns {Promise<object>} 响应体（code/data/messageZh 等）
 */
export const fetchCardSetting = async (appId) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const result = await fetchApi(url);
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * #TBD-CS02 更新单个卡片周期（失效或删除）
 * @param {string} appId
 * @param {0|1} periodType - 0=删除周期, 1=失效周期
 * @param {number} periodDays - 周期天数
 * @returns {Promise<object>} 响应体
 */
export const updateCardPeriod = async (appId, periodType, periodDays) => {
  try {
    const url = buildApiUrl(API_CONFIG.APP_APIS.CARD_SETTINGS, { appId });
    const result = await fetchApi(url, {
      method: 'PUT',
      body: JSON.stringify({ periodType, periodDays }),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
