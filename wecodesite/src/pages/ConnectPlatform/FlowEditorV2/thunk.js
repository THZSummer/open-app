/**
 * ========================================
 * 连接流编辑器 V2 - API 调用函数
 * ========================================
 *
 * 对齐 plan-api.md v7.0：
 *   - #29 查询连接流版本列表
 *   - #30 查询连接流版本详情
 *   - #31 更新连接流版本（保存草稿）
 *   - #32 发布连接流版本
 *   - #28 创建草稿版本 / #33 复制版本到草稿
 *   - #34 失效版本 / #37 撤回审批 / #36 删除版本
 *   - #38 催办审批
 *   - #51 调试连接流版本（代理）
 *   - #2 查询连接器列表（status=2 有效可用）
 *   - #9 查询连接器版本列表（status=2 已发布）
 *   - #10 查询连接器版本详情（取入参）
 *
 * 应用级配置（超时上限/限流上限/记录条数上限/日志开关）
 * 后端在 plan-api.md §3.9a 由 market-server Property 提供，
 * 暂未对接独立接口，沿用前端默认配置。
 */

import { API_CONFIG, buildApiUrl, fetchApi } from '../../../configs/web.config';
import { buildVersionSummary } from '../../../utils/common';

/**
 * 应用级配置默认值（与 plan-api.md §3.9a #54~#55 字段对齐）
 */
const DEFAULT_APP_CONFIG = {
  maxTimeoutMs: 30000,
  maxQps: 1000,
  maxConcurrency: 100,
  maxRecords: 10000,
  logSwitch: 1,
};

/**
 * 获取应用级配置（暂用前端默认值，后端接口就绪后切换）
 *
 * @returns {Promise<Object>} 应用级配置
 */
export const fetchAppConfig = async () => {
  return { code: '200', data: { ...DEFAULT_APP_CONFIG } };
};

/**
 * 查询连接流版本列表（#29）
 *
 * @param {string} flowId - 连接流 ID
 * @returns {Promise<Object>} 版本列表响应
 */
export const fetchVersionList = async (flowId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSIONS_LIST, { flowId })
    );
    if (result?.code !== '200') return result || {};

    // 将后端数据映射为 UI 所需摘要结构（按创建时间倒序）
    const list = (result.data || [])
      .map(buildVersionSummary)
      .sort((a, b) => (a.createTime < b.createTime ? 1 : -1));
    return { code: '200', data: list };
  } catch (err) {
    return {};
  }
};

/**
 * 查询连接流版本详情（#30）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 版本详情响应
 */
export const fetchVersionDetail = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DETAIL, { flowId, versionId })
    );
    if (result?.code !== '200') return result || {};

    // 将后端版本摘要字段（含 v{versionNumber} 拼接的 name）与其余业务字段合并
    const detail = result.data || {};
    return {
      code: '200',
      data: {
        ...detail,
        ...buildVersionSummary(detail),
      },
    };
  } catch (err) {
    return {};
  }
};

/**
 * 保存草稿版本（#31 更新连接流版本）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅草稿状态可编辑）
 * - config: 编排配置（orchestrationConfig 内容：flowConfig / nodes / edges）
 *
 * @returns {Promise<Object>} 保存结果
 */
export const saveDraft = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId, config } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_UPDATE, { flowId, versionId }),
      {
        method: 'PUT',
        body: JSON.stringify({ orchestrationConfig: config }),
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 发布连接流版本（#32）
 * 流程：先保存最新草稿，再触发发布（进入审批流程）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 * - config: 编排配置
 *
 * @returns {Promise<Object>} 发布结果
 */
export const publishVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId, config } = params;

  try {
    // 1. 先保存最新草稿
    const saveRes = await saveDraft({ flowId, versionId, config });
    if (saveRes?.code !== '200') return saveRes || {};

    // 2. 触发发布（后端自动进入三级审批流程）
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_PUBLISH, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 创建草稿版本（#28 / #33）
 * 传入 baseVersionId 时走「复制到草稿」#33；否则走「创建空草稿」#28
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - baseVersionId: 基础版本 ID（可选）
 *
 * @returns {Promise<Object>} 创建结果
 */
export const createDraftVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, baseVersionId } = params;

  try {
    let result;
    if (baseVersionId) {
      // 基于已发布/已失效版本复制到草稿（#33）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.FLOWS.VERSION_COPY_TO_DRAFT, {
          flowId,
          versionId: baseVersionId,
        }),
        { method: 'POST' }
      );
    } else {
      // 创建空草稿（#28）
      result = await fetchApi(
        buildApiUrl(API_CONFIG.FLOWS.VERSION_CREATE, { flowId }),
        { method: 'POST' }
      );
    }
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 失效版本（#34）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅已发布状态可失效）
 *
 * @returns {Promise<Object>} 操作结果
 */
export const expireVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_INVALIDATE, { flowId, versionId }),
      { method: 'PUT' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 撤回版本审批（#37）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID（仅待审批状态可撤回）
 *
 * @returns {Promise<Object>} 操作结果
 */
export const withdrawVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_CANCEL, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 删除版本（#36）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 操作结果
 */
export const deleteVersion = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DELETE, { flowId, versionId }),
      { method: 'DELETE' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询连接器列表（#2，过滤为有效可用 status=2）
 *
 * @returns {Promise<Object>} 连接器列表
 */
export const fetchConnectorList = async () => {
  try {
    const result = await fetchApi(API_CONFIG.CONNECTORS.LIST, {
      params: { status: 2 },
    });
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询连接器版本列表（#9，过滤为已发布 status=2）
 *
 * @param {string} connectorId - 连接器 ID
 * @returns {Promise<Object>} 版本列表
 */
export const fetchConnectorVersions = async (connectorId) => {
  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSIONS_LIST, { connectorId }),
      { params: { status: 2 } }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 查询连接器版本入参（#10，仅取 connectionConfig.input）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - connectorId: 连接器 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 入参配置（含 protocol / header / query / body）
 */
export const fetchConnectorInputParams = async (params) => {
  // 解构传入对象中需要使用的参数
  const { connectorId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.CONNECTORS.VERSION_DETAIL, { connectorId, versionId })
    );
    if (result?.code !== '200') return result || {};

    // 仅返回入参配置
    return {
      code: '200',
      data: result.data?.connectionConfig?.input || {},
    };
  } catch (err) {
    return {};
  }
};

/**
 * 查询版本详情信息（含状态关联字段）
 * 与 #30 等价，UI 层用于展示审批/驳回/撤回等元数据
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 版本详情信息
 */
export const fetchVersionDetailInfo = async (params) => {
  return fetchVersionDetail(params);
};

/**
 * 调试连接流（#51 调试连接流版本（代理））
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 * - inputParams: 模拟触发数据（与触发器入参 Schema 对齐）
 *
 * @returns {Promise<Object>} 调试结果（含 executionId / status / durationMs / nodes）
 */
export const debugFlow = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId, inputParams } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_DEBUG, { flowId, versionId }),
      {
        method: 'POST',
        body: JSON.stringify({ triggerData: inputParams || {} }),
      }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};

/**
 * 催办审批（#38）
 *
 * @param {Object} params - 参数对象
 * 包含以下字段：
 * - flowId: 连接流 ID
 * - versionId: 版本 ID
 *
 * @returns {Promise<Object>} 催办结果
 */
export const urgeApproval = async (params) => {
  // 解构传入对象中需要使用的参数
  const { flowId, versionId } = params;

  try {
    const result = await fetchApi(
      buildApiUrl(API_CONFIG.FLOWS.VERSION_URGE, { flowId, versionId }),
      { method: 'POST' }
    );
    return result || {};
  } catch (err) {
    return {};
  }
};
