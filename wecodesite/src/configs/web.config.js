import { queryParams } from "../utils/common";

export const API_CONFIG = {
  BASE_URL: '/service/open/v2',

  APIS: {
    LIST: '/apis',
    DETAIL: '/apis/{id}',
    CREATE: '/apis',
    UPDATE: '/apis/{id}',
    DELETE: '/apis/{id}',
    WITHDRAW: '/apis/{id}/withdraw',
    CATEGORIES: '/categories',
  },

  // ===== 连接器（对齐 plan-api.md v7.0） =====
  CONNECTORS: {
    LIST: '/connectors',                                                      // #2 查询连接器列表
    DETAIL: '/connectors/{connectorId}',                                      // #3 查询连接器详情
    CREATE: '/connectors',                                                    // #1 创建连接器
    UPDATE: '/connectors/{connectorId}',                                      // #4 更新连接器
    DELETE: '/connectors/{connectorId}',                                      // #7 删除连接器
    INVALIDATE: '/connectors/{connectorId}/invalidate',                       // #5 失效连接器
    RECOVER: '/connectors/{connectorId}/recover',                             // #6 恢复连接器

    // ===== 连接器版本（#8~#16） =====
    VERSIONS_LIST: '/connectors/{connectorId}/versions',                      // #9 查询版本列表
    VERSION_CREATE: '/connectors/{connectorId}/versions',                     // #8 创建草稿版本
    VERSION_DETAIL: '/connectors/{connectorId}/versions/{versionId}',         // #10 查询版本详情
    VERSION_UPDATE: '/connectors/{connectorId}/versions/{versionId}',         // #11 更新版本
    VERSION_PUBLISH: '/connectors/{connectorId}/versions/{versionId}/publish',// #12 发布版本
    VERSION_COPY_TO_DRAFT: '/connectors/{connectorId}/versions/{versionId}/copy-to-draft', // #13 复制到草稿
    VERSION_INVALIDATE: '/connectors/{connectorId}/versions/{versionId}/invalidate',       // #14 失效版本
    VERSION_RECOVER: '/connectors/{connectorId}/versions/{versionId}/recover',             // #15 恢复版本
    VERSION_DELETE: '/connectors/{connectorId}/versions/{versionId}',                      // #16 删除版本
  },

  // ===== 连接流（对齐 plan-api.md v7.0） =====
  FLOWS: {
    LIST: '/flows',                                                           // #18 查询连接流列表
    DETAIL: '/flows/{flowId}',                                                // #19 查询连接流详情
    CREATE: '/flows',                                                         // #17 创建连接流
    UPDATE: '/flows/{flowId}',                                                // #20 更新连接流
    DELETE: '/flows/{flowId}',                                                // #27 删除连接流
    COPY: '/flows/{flowId}/copy',                                             // #21 复制连接流
    DEPLOY: '/flows/{flowId}/deploy',                                         // #22 部署连接流
    START: '/flows/{flowId}/start',                                           // #23 启动连接流
    STOP: '/flows/{flowId}/stop',                                             // #24 停止连接流
    INVALIDATE: '/flows/{flowId}/invalidate',                                 // #25 失效连接流
    RECOVER: '/flows/{flowId}/recover',                                       // #26 恢复连接流

    // ===== 连接流版本（#28~#38） =====
    VERSIONS_LIST: '/flows/{flowId}/versions',                                // #29 查询版本列表
    VERSION_CREATE: '/flows/{flowId}/versions',                               // #28 创建草稿版本
    VERSION_DETAIL: '/flows/{flowId}/versions/{versionId}',                   // #30 查询版本详情
    VERSION_UPDATE: '/flows/{flowId}/versions/{versionId}',                   // #31 更新版本
    VERSION_PUBLISH: '/flows/{flowId}/versions/{versionId}/publish',          // #32 发布版本
    VERSION_COPY_TO_DRAFT: '/flows/{flowId}/versions/{versionId}/copy-to-draft', // #33 复制到草稿
    VERSION_INVALIDATE: '/flows/{flowId}/versions/{versionId}/invalidate',    // #34 失效版本
    VERSION_RECOVER: '/flows/{flowId}/versions/{versionId}/recover',          // #35 恢复版本
    VERSION_DELETE: '/flows/{flowId}/versions/{versionId}',                   // #36 删除版本
    VERSION_CANCEL: '/flows/{flowId}/versions/{versionId}/cancel',            // #37 撤回审批
    VERSION_URGE: '/flows/{flowId}/versions/{versionId}/urge',                // #38 催办审批
    VERSION_DEBUG: '/flows/{flowId}/versions/{versionId}/debug',              // #51 调试代理

    // ===== 运行记录（#49~#50） =====
    EXECUTIONS_LIST: '/executions',                                            // #49 查询运行记录列表
    EXECUTION_DETAIL: '/executions/{executionId}',                             // #50 查询运行记录详情
  },

  EVENTS: {
    LIST: '/events',
    DETAIL: '/events/{id}',
    CREATE: '/events',
    UPDATE: '/events/{id}',
    DELETE: '/events/{id}',
    WITHDRAW: '/events/{id}/withdraw',
  },

  CALLBACKS: {
    LIST: '/callbacks',
    DETAIL: '/callbacks/{id}',
    CREATE: '/callbacks',
    UPDATE: '/callbacks/{id}',
    DELETE: '/callbacks/{id}',
    WITHDRAW: '/callbacks/{id}/withdraw',
  },

  APP_APIS: {
    LIST: '/apps/{appId}/apis',
    SUBSCRIBE: '/apps/{appId}/apis/subscribe',
    WITHDRAW: '/apps/{appId}/apis/{id}/withdraw',
    DELETE: '/apps/{appId}/apis/{id}',
    CARD_SETTINGS: '/apps/{appId}/card-settings',
  },

  APP_EVENTS: {
    LIST: '/apps/{appId}/events',
    SUBSCRIBE: '/apps/{appId}/events/subscribe',
    CONFIG: '/apps/{appId}/events/{id}/config',
    WITHDRAW: '/apps/{appId}/events/{id}/withdraw',
    DELETE: '/apps/{appId}/events/{id}',
  },

  APP_CALLBACKS: {
    LIST: '/apps/{appId}/callbacks',
    SUBSCRIBE: '/apps/{appId}/callbacks/subscribe',
    CONFIG: '/apps/{appId}/callbacks/{id}/config',
    WITHDRAW: '/apps/{appId}/callbacks/{id}/withdraw',
    DELETE: '/apps/{appId}/callbacks/{id}',
  },

  CATEGORIES: {
    LIST: '/categories',
    DETAIL: '/categories/{id}',
    CREATE: '/categories',
    UPDATE: '/categories/{id}',
    DELETE: '/categories/{id}',
    OWNERS: '/categories/{id}/owners',
    APIS: '/categories/{id}/apis',
    EVENTS: '/categories/{id}/events',
    CALLBACKS: '/categories/{id}/callbacks',
    REMOVEOWNER: '/categories/{id}/owners/{userId}'
  },

  APPROVALS: {
    PENDING: '/approvals/pending',
    DETAIL: '/approvals/{id}',
    APPROVE: '/approvals/{id}/approve',
    REJECT: '/approvals/{id}/reject',
    CANCEL: '/approvals/{id}/cancel',
    BATCH_APPROVE: '/approvals/batch-approve',
    BATCH_REJECT: '/approvals/batch-reject',
    REMIND_APPROVE: '/approvals/{id}/urge',
  },

  APPROVAL_FLOWS: {
    LIST: '/approval-flows',
    DETAIL: '/approval-flows/{id}',
    CREATE: '/approval-flows',
    UPDATE: '/approval-flows/{id}',
    DELETE: '/approval-flows/{id}',
  },

  // ===== 应用管理模块 (APP-MGMT-001) =====
  APP: {
    LIST: '/app/list',
    CREATE: '/app',
    DETAIL: '/app',
    UPDATE: '/app',
    DELETE: '/app',
    ICONS: '/app/icons',
    EAMAP_LIST: '/app/eamap',
    CURRENT_ROLE: '/app/current-role',
    IDENTITY: '/app/identity',
    VERIFY_TYPE_GET: '/app/verify-type',
    VERIFY_TYPE_UPDATE: '/app/verify-type',
    BIND_EAMAP: '/app/bind-eamap',
  },

  APP_MEMBERS: {
    LIST: '/member/list',
    ADD: '/member',
    DELETE: '/member',
    TRANSFER_OWNER: '/member/transfer-owner',
    SEARCH_USERS: '/member/search-users',
  },

  APP_ABILITIES: {
    LIST: '/ability/list',
    SUBSCRIBED: '/ability/subscribed',
    ADD: '/ability',
  },

  APP_VERSIONS: {
    LIST: '/version/list',
    CREATE: '/version',
    DETAIL: '/version',
    UPDATE: '/version',
    PUBLISH: '/version/publish',
    WITHDRAW: '/version/withdraw',
    DELETE: '/version',
  },

  // ===== 通用查询接口 =====
  LOOKUP: {
    WHITELIST: '/lookup/whitelist',
  },

  // ===== 通用文件接口 =====
  FILE: {
    UPLOAD_IMAGE: '/file/upload-image',
  },
};

export const buildApiUrl = (template, params = {}) => {
  let url = template;
  Object.keys(params).forEach(key => {
    url = url.replace(`{${key}}`, params[key]);
  });
  return url;
};

export const fetchApi = async (url, options = {}) => {
  const { params, rawBody, ...fetchOptions } = options;
  let fullUrl = `${API_CONFIG.BASE_URL}${url}`;
  if (params) {
    const queryString = new URLSearchParams(params).toString();
    fullUrl = queryString ? `${fullUrl}?${queryString}` : fullUrl;
  }

  const headers = {};
  // 只有在 rawBody=false 且没有显示设置 Content-Type 时才默认设置值
  if (!rawBody && !(fetchOptions.headers?.['Content-Type'])) {
    headers['Content-Type'] = 'application/json';
  }
  const appId = queryParams('appId');
  if (appId) {
    headers['X-App-Id'] = appId;
  }

  const response = await fetch(fullUrl, {
    ...fetchOptions,
    credentials: 'include',
    headers: {
      ...headers,
      ...fetchOptions.headers,
    },
  });
  return response;
};
