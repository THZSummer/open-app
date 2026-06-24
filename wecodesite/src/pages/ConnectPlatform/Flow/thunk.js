/**
 * ========================================
 * 连接流列表 - API调用函数
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #18 查询连接流列表
 *   - #17 创建连接流
 *   - #20 更新连接流
 *   - #21 复制连接流 / #22 部署连接流
 *   - #23 启动连接流 / #24 停止连接流
 *   - #25 失效连接流 / #26 恢复连接流
 *   - #27 删除连接流
 *   - 已发布版本查询：复用 #29 GET /flows/{flowId}/versions?status=5
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';

/**
 * 查询连接流列表（#18）
 *
 * @param {Object} params - 查询参数对象
 * 包含以下字段：
 * - curPage: 当前页码（默认 1）
 * - pageSize: 每页数量（默认 20）
 * - keyword: 关键词模糊搜索（按中文名称）
 * - lifecycleStatus: 生命周期状态（1=已停止 / 2=运行中 / 3=已失效）
 *
 * @returns {Promise<Object>} 列表响应
 */
export const fetchFlowList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.FLOWS.LIST, { params });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建连接流（#17）
 *
 * @param {Object} data - 连接流数据
 * 包含以下字段：
 * - nameCn: 中文名称
 * - nameEn: 英文名称
 * - descriptionCn: 中文描述
 * - descriptionEn: 英文描述
 *
 * @returns {Promise<Object>} 创建结果
 */
export const createFlow = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.FLOWS.CREATE, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 更新连接流（#20）
 *
 * @param {Object} params - 更新参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - data: 表单数据（nameCn / nameEn / descriptionCn / descriptionEn 可选）
 *
 * @returns {Promise<Object>} 更新结果
 */
export const updateFlow = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, data } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.UPDATE, { flowId }),
      {
        method: 'PUT',
        body: JSON.stringify(data),
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除连接流（#27）
 *
 * @param {string} flowId - 连接流 ID（仅已失效状态可删除）
 * @returns {Promise<Object>} 删除结果
 */
export const deleteFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.DELETE, { flowId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 启动连接流（#23）
 * 后端 POST /flows/{flowId}/start，将 lifecycleStatus 由「已停止」迁移至「运行中」
 *
 * @param {string} flowId - 连接流 ID
 * @returns {Promise<Object>} 启动结果
 */
export const startFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.START, { flowId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 停止连接流（#24）
 * 后端 POST /flows/{flowId}/stop，将 lifecycleStatus 由「运行中」迁移至「已停止」
 *
 * @param {string} flowId - 连接流 ID
 * @returns {Promise<Object>} 停止结果
 */
export const stopFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.STOP, { flowId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 部署连接流（#22）
 * 仅更新已部署版本绑定，不改变 lifecycleStatus
 *
 * @param {Object} params - 部署参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 目标已发布版本 ID
 *
 * @returns {Promise<Object>} 部署结果
 */
export const deployFlow = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.DEPLOY, { flowId }),
      {
        method: 'POST',
        body: JSON.stringify({ versionId }),
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询当前连接流的已发布版本列表（复用 #29，状态=5 已发布）
 *
 * @param {string} flowId - 连接流 ID
 * @returns {Promise<Object>} 已发布版本列表
 */
export const fetchPublishedVersions = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSIONS_LIST, { flowId }),
      { params: { status: 5 } }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 复制连接流（#21）
 *
 * @param {string} flowId - 源连接流 ID
 * @returns {Promise<Object>} 复制结果
 */
export const copyFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.COPY, { flowId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 失效连接流（#25）
 *
 * @param {string} flowId - 连接流 ID（仅已停止状态可失效）
 * @returns {Promise<Object>} 失效结果
 */
export const disableFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.INVALIDATE, { flowId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 恢复连接流（#26）
 *
 * @param {string} flowId - 连接流 ID（仅已失效状态可恢复）
 * @returns {Promise<Object>} 恢复结果
 */
export const restoreFlow = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.RECOVER, { flowId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
