import API_CONFIG from '../../../configs/web.config';
import { fetchApi } from '../../../utils/webFetch';

/**
 * 查询已绑定的机器人账号列表
 *
 * @param {string} appId 应用业务 ID
 * @returns {Promise<Object>} API 响应
 */
export const fetchBoundAccounts = async (appId) => {
  try {
    const url = `${API_CONFIG.APP_CHATBOT_ACCOUNTS}?appId=${encodeURIComponent(appId)}`;
    const result = await fetchApi(url, { method: 'GET' });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 绑定机器人账号
 *
 * @param {string} appId 应用业务 ID
 * @param {string} accountId 机器人账号 ID
 * @returns {Promise<Object>} API 响应
 */
export const bindAccount = async (appId, accountId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_CHATBOT_ACCOUNTS, {
      method: 'POST',
      body: JSON.stringify({ appId, accountId }),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 解绑机器人账号
 *
 * @param {string} appId 应用业务 ID
 * @param {string} accountId 机器人账号 ID
 * @returns {Promise<Object>} API 响应
 */
export const unbindAccount = async (appId, accountId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP_CHATBOT_ACCOUNTS, {
      method: 'DELETE',
      body: JSON.stringify({ appId, accountId }),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
