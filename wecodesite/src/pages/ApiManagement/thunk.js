/**
 * ========================================
 * API权限管理 - 接口封装层
 * ========================================
 * 功能：应用订阅API权限相关接口
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 获取API分类列表
 *
 * @description 根据API类型获取对应的分类数据
 * @param {string} apiType - API类型标识
 * @returns {Promise<Object>} 分类列表响应
 */
export const fetchCategories = async (apiType) => {
  try {
    const response = await fetchApi(API_CONFIG.CATEGORIES.LIST, {
      params: { categoryAlias: apiType }
    });
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取API列表（用于选择器）
 *
 * @description 根据分类和筛选条件获取可选的API列表
 * @param {Object} options - 查询选项
 * @returns {Promise<Object>} API列表响应
 */
export const fetchApis = async ({
  keyword,
  needReview,
  apiType,
  categoryId,
  curPage,
  pageSize,
  appId
}) => {
  const params = {};
  if (keyword) params.keyword = keyword;
  if (needReview !== undefined && needReview !== 'all') {
    params.needApproval = needReview === 'true' ? 1 : 0;
  }
  if (curPage) params.curPage = curPage;
  if (pageSize) params.pageSize = pageSize;
  if (appId) params.appId = appId;
  params.includeChildren = true;

  try {
    const response = await fetchApi(
      buildApiUrl(API_CONFIG.CATEGORIES.APIS, { id: categoryId }),
      { params }
    );
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取应用已订阅的API列表
 *
 * @description 分页获取当前应用的API订阅记录
 * @param {string} appId - 应用标识
 * @param {Object} paginationParams - 分页和筛选参数
 * @returns {Promise<Object>} 订阅列表响应
 */
export const fetchAppApis = async (appId, paginationParams = {}) => {
  try {
    const response = await fetchApi(
      buildApiUrl(API_CONFIG.APP_APIS.LIST, { appId }),
      { params: paginationParams }
    );
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 申请API权限订阅
 *
 * @description 向应用添加API权限订阅
 * @param {string} appId - 应用标识
 * @param {Array} permissionIds - 权限ID列表
 * @returns {Promise<Object>} 订阅结果
 */
export const subscribeApis = async (appId, { permissionIds }) => {
  try {
    const response = await fetchApi(
      buildApiUrl(API_CONFIG.APP_APIS.SUBSCRIBE, { appId }),
      { method: 'POST', body: JSON.stringify({ permissionIds }) }
    );
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回API申请
 *
 * @description 将待审核的API订阅申请撤回
 * @param {string} appId - 应用标识
 * @param {string} subscriptionId - 订阅记录标识
 * @returns {Promise<Object>} 撤回操作结果
 */
export const withdrawApiApplication = async (appId, subscriptionId) => {
  try {
    const response = await fetchApi(
      buildApiUrl(API_CONFIG.APP_APIS.WITHDRAW, { appId, id: subscriptionId }),
      { method: 'POST' }
    );
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 催办API审批
 *
 * @description 对待审核的API订阅进行催办提醒
 * @param {string} subscriptionId - 订阅记录标识
 * @returns {Promise<Object>} 催办结果
 */
export const remindApproval = async (subscriptionId) => {
  try {
    const response = await fetchApi(`/approval/remind`, {
      method: 'POST',
      body: JSON.stringify({ id: subscriptionId })
    });
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 移除API订阅
 *
 * @description 删除已通过的API订阅记录
 * @param {string} appId - 应用标识
 * @param {string} subscriptionId - 订阅记录标识
 * @returns {Promise<Object>} 删除结果
 */
export const deleteApiSubscription = async (appId, subscriptionId) => {
  try {
    const response = await fetchApi(
      buildApiUrl(API_CONFIG.APP_APIS.DELETE, { appId, id: subscriptionId }),
      { method: 'DELETE' }
    );
    return response || {};
  } catch (err) {
    return {};
  }
};

/**
 * 获取Tab配置
 *
 * @description 获取API订阅抽屉的两层Tab配置数据
 * @param {string} searchKey - 配置查询键
 * @returns {Promise<Object>} Tab配置数据
 */
export const fetchTabConfig = async (searchKey = 'CEC.Open/Api.Drawer.TabsList') => {
  try {
    // TODO: 替换为真实接口调用
    // const result = await fetchApi('/lookup', { params: { searchKey } });

    const mockResponse = {
      code: 200,
      message: 'success',
      data: {
        lookups: {
          'CEC.Open/Api.Drawer.TabsList': {
            items: [
              {
                itemCode: 'business',
                itemDesc: '业务应用订阅api抽屉弹窗两层tab数据',
                itemValue: JSON.stringify([
                  {
                    key: 'business_app_business_role',
                    label: '业务身份权限',
                    desc: '业务应用下的业务身份权限',
                    children: [
                      { key: 'api_business_app_soa', label: 'SOA类型' },
                      { key: 'api_business_app_apig', label: 'API类型' }
                    ]
                  },
                  {
                    key: 'business_app_person_role',
                    label: '个人身份权限',
                    desc: '业务应用下的个人身份权限',
                    children: [
                      { key: 'api_business_user_soa', label: 'SOA类型' },
                      { key: 'api_business_user_apig', label: 'API类型' }
                    ]
                  }
                ])
              },
              {
                itemCode: 'person',
                itemDesc: '个人应用订阅api抽屉弹窗两层tab数据',
                itemValue: JSON.stringify([
                  {
                    key: 'person_app_person_role',
                    label: '个人身份权限',
                    desc: '个人应用下的个人身份权限',
                    children: [
                      { key: 'api_personal_user_aksk', label: 'AKSK' }
                    ]
                  }
                ])
              }
            ]
          }
        }
      }
    };

    return mockResponse;
  } catch (err) {
    return { code: 500, message: '获取Tab配置失败' };
  }
};
