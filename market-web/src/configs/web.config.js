/**
 * Web API 配置文件
 * 统一管理所有接口配置
 */

export default {
  // 分类管理 API 配置
  CLASSIFY_LIST: '/market-web/service/open/v2/lookup/classify/list',
  CLASSIFY_DETAIL: '/market-web/service/open/v2/lookup/classify/{classifyId}',
  CLASSIFY_CREATE: '/market-web/service/open/v2/lookup/classify',
  CLASSIFY_UPDATE: '/market-web/service/open/v2/lookup/classify/{classifyId}',
  CLASSIFY_DELETE: '/market-web/service/open/v2/lookup/classify/{classifyId}',

  // LookUp 项管理 API 配置
  ITEM_LIST: '/market-web/service/open/v2/lookup/classify/{classifyId}/items',
  ITEM_DETAIL: '/market-web/service/open/v2/lookup/items/{itemId}',
  ITEM_CREATE: '/market-web/service/open/v2/lookup/classify/{classifyId}/items',
  ITEM_UPDATE: '/market-web/service/open/v2/lookup/items/{itemId}',
  ITEM_DELETE: '/market-web/service/open/v2/lookup/items/{itemId}',

  // 数据字典 API 配置
  DICTIONARY_LIST: '/market-web/service/open/v2/dictionary/list',
  DICTIONARY_DETAIL: '/market-web/service/open/v2/dictionary/{id}',
  DICTIONARY_CREATE: '/market-web/service/open/v2/dictionary',
  DICTIONARY_UPDATE: '/market-web/service/open/v2/dictionary/{id}',
  DICTIONARY_DELETE: '/market-web/service/open/v2/dictionary/{id}',

  // 审批管理 API 配置
  APPROVAL_PENDING_LIST: '/market-web/service/open/v2/apps/pending',
  APPROVAL_PUBLISHED_LIST: '/market-web/service/open/v2/apps/publish',
  APPROVAL_PROCESS: '/market-web/service/open/v2/apps/approval',
};
