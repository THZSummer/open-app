/**
 * ========================================
 * 运行管理模块 - 数据请求层
 * ========================================
 *
 * 对齐 plan-api.md v8.2：
 *   - #49 查询运行记录列表  GET /executions?keyword=&flowId=&status=...
 *   - #50 查询运行记录详情  GET /executions/{executionId}
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 订阅群列表（当前阶段后端未提供专用接口，统一返回空）
 */
// eslint-disable-next-line no-unused-vars
export const getGroupSubscribe = async (params) => {
  return { data: [], total: 0 };
};

/**
 * 查询连接流执行列表（#49）
 *
 * @param {Object} params - 参数对象
 * @param {number} params.page - 当前页码（1-based）
 * @param {number} params.pageSize - 每页大小
 * @param {Object} params.filters - 查询条件 { keyword, flowId, status, triggerType, startTime, endTime }
 * @returns {Promise<Object>} 含 data 与 total 的本地约定结构
 */
export const fetchFlowRunList = async (params) => {
  const { page = 1, pageSize = 10, filters = {} } = params || {};

  try {
    const query = { curPage: page, pageSize };
    if (filters.keyword) query.keyword = filters.keyword;
    if (filters.flowId) query.flowId = filters.flowId;
    if (filters.status != null && filters.status !== '') query.status = filters.status;
    if (filters.triggerType != null && filters.triggerType !== '') query.triggerType = filters.triggerType;
    if (filters.startTime) query.startTime = filters.startTime;
    if (filters.endTime) query.endTime = filters.endTime;

    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.EXECUTIONS_LIST),
      { params: query }
    );

    if (result?.code !== '200') {
      return { data: [], total: 0 };
    }

    return {
      data: result.data || [],
      total: Number(result.page?.total) || 0,
    };
  } catch (err) {
    return { data: [], total: 0 };
  }
};

/**
 * 查询连接流执行详情（#50）
 *
 * @param {Object} params - 参数对象
 * @param {string} params.executionId - 执行记录 ID
 * @returns {Promise<Object>} 含 base（基础信息）与 steps（各节点执行步骤）的结构
 */
export const fetchFlowRunDetail = async (params) => {
  const { executionId } = params || {};

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.EXECUTION_DETAIL, { executionId })
    );

    if (result?.code !== '200') {
      return { base: null, steps: [] };
    }

    const detail = result.data || {};
    return {
      base: detail,
      steps: detail.steps || [],
    };
  } catch (err) {
    return { base: null, steps: [] };
  }
};
