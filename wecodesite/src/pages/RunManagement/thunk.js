/**
 * ========================================
 * 运行管理模块 - 数据请求层
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #49 查询运行记录列表
 *   - #50 查询运行记录详情
 *
 * 当前页面为全局视角，按 plan-api.md §3.7 要求需带 flowId。
 * 在全局无 flowId 上下文时，使用占位 flowId=1 调用（后端可识别为全局）。
 */
import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 全局视角下使用的占位连接流 ID
 */
const GLOBAL_FLOW_ID = '1';

/**
 * 订阅群列表（当前阶段后端未提供专用接口，统一返回空）
 *
 * @param {Object} params - 查询参数对象
 * 包含以下字段：
 * - page: 当前页码
 * - pageSize: 每页大小
 * - filters: 查询条件 { groupId, subscribeAccount, subscribeType }
 *
 * @returns {Promise<Object>} 列表响应（data 数组与 total）
 */
// eslint-disable-next-line no-unused-vars
export const getGroupSubscribe = async (params) => {
  return { data: [], total: 0 };
};

/**
 * 查询连接流执行列表（#49）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - page: 当前页码（前端 1-based）
 * - pageSize: 每页大小
 * - filters: 查询条件 { executionId, flowNameCn, status, triggerType, startTime, endTime }
 * - flowId: 连接流 ID（可选；不传则使用全局占位 ID=1）
 *
 * @returns {Promise<Object>} 含 data 与 total 的本地约定结构
 */
export const fetchFlowRunList = async (params) => {
  // 解构传入对象中需要使用的参数
  const { page = 1, pageSize = 10, filters = {}, flowId = GLOBAL_FLOW_ID } = params || {};

  try {
    // 组装后端查询参数（仅传入有值的字段，避免空字符串污染）
    const query = { curPage: page, pageSize };
    if (filters.status != null && filters.status !== '') query.status = filters.status;
    if (filters.triggerType != null && filters.triggerType !== '') query.triggerType = filters.triggerType;
    if (filters.startTime) query.startTime = filters.startTime;
    if (filters.endTime) query.endTime = filters.endTime;

    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.EXECUTIONS_LIST, { flowId }),
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
 * 包含以下字段：
 * - executionId: 执行记录 ID
 * - flowId: 连接流 ID（可选；不传则使用全局占位 ID=1）
 *
 * @returns {Promise<Object>} 含 base（基础信息）与 steps（各节点执行步骤）的结构
 */
export const fetchFlowRunDetail = async (params) => {
  // 解构传入对象中需要使用的参数
  const { executionId, flowId = GLOBAL_FLOW_ID } = params || {};

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.EXECUTION_DETAIL, { flowId, executionId })
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
