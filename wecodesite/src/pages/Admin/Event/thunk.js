/**
 * ========================================
 * 事件管理 - 数据接口
 * ========================================
 * 主要功能：平台事件定义的增删改查
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

// 获取事件列表
export const fetchEventList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

// 获取事件详情
export const fetchEventDetail = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DETAIL, { id }));
    return result || {};
  } catch (err) {
    return {};
  }
};

// 创建事件
export const createEvent = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.EVENTS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

// 更新事件
export const updateEvent = async (id, data) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.UPDATE, { id }), {
      method: 'PUT',
      body: JSON.stringify(data)
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

// 删除事件
export const deleteEvent = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.DELETE, { id }), {
      method: 'DELETE'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

// 撤回事件
export const withdrawEvent = async (id) => {
  try {
    const result = await fetchApi(buildApiUrl(API_CONFIG.EVENTS.WITHDRAW, { id }), {
      method: 'POST'
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
