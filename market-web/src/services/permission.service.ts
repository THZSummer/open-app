import { get, post, put } from '@/utils/request';

/**
 * 权限管理相关接口
 */

// ==================== 类型定义 ====================

export interface Category {
  id: string;
  nameCn: string;
  path: string;
  categoryPath: string[];
}

export interface Permission {
  id: string;
  nameCn: string;
  nameEn: string;
  scope: string;
  status: number;
  needApproval: number;
  isSubscribed?: number;
}

export interface ApiPermission extends Permission {
  api: {
    path: string;
    method: string;
    docUrl?: string;
  };
}

export interface EventPermission extends Permission {
  event: {
    topic: string;
    docUrl?: string;
  };
}

export interface CallbackPermission extends Permission {
  callback: {
    docUrl?: string;
  };
}

export interface Subscription {
  id: string;
  appId: string;
  permissionId: string;
  permission: Permission;
  category?: Category;
  status: number; // 0=待审, 1=已授权, 2=已拒绝, 3=已取消
  authType?: number;
  approvalUrl?: string;
  createTime?: string;
  // 事件/回调特有字段
  channelType?: number;
  channelAddress?: string;
}

export interface ApiSubscription extends Subscription {
  api?: {
    path: string;
    method: string;
    docUrl?: string;
  };
}

export interface EventSubscription extends Subscription {
  event?: {
    topic: string;
  };
}

export interface CallbackSubscription extends Subscription {
  // 回调没有额外字段
}

// ==================== API 权限管理 ====================

/**
 * 获取应用 API 权限列表
 */
export const getAppApiSubscriptions = (appId: string, params?: {
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<ApiSubscription[]>(`/apps/${appId}/apis`, params);
};

/**
 * 获取分类下 API 权限列表（权限树懒加载）
 */
export const getCategoryApiPermissions = (categoryId: string, params?: {
  keyword?: string;
  needApproval?: number;
  includeChildren?: boolean;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<ApiPermission[]>(`/categories/${categoryId}/apis`, params);
};

/**
 * 申请 API 权限（支持批量）
 */
export const subscribeApiPermissions = (appId: string, permissionIds: string[]) => {
  return post<{
    successCount: number;
    failedCount: number;
    records: Subscription[];
  }>(`/apps/${appId}/apis/subscribe`, { permissionIds });
};

/**
 * 撤回 API 权限申请
 */
export const withdrawApiSubscription = (appId: string, subscriptionId: string) => {
  return post<Subscription>(`/apps/${appId}/apis/${subscriptionId}/withdraw`);
};

// ==================== 事件权限管理 ====================

/**
 * 获取应用事件订阅列表
 */
export const getAppEventSubscriptions = (appId: string, params?: {
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<EventSubscription[]>(`/apps/${appId}/events`, params);
};

/**
 * 获取分类下事件权限列表（权限树懒加载）
 */
export const getCategoryEventPermissions = (categoryId: string, params?: {
  keyword?: string;
  needApproval?: number;
  includeChildren?: boolean;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<EventPermission[]>(`/categories/${categoryId}/events`, params);
};

/**
 * 申请事件权限（支持批量）
 */
export const subscribeEventPermissions = (appId: string, permissionIds: string[]) => {
  return post<{
    successCount: number;
    failedCount: number;
    records: Subscription[];
  }>(`/apps/${appId}/events/subscribe`, { permissionIds });
};

/**
 * 配置事件消费参数
 */
export const configEventSubscription = (appId: string, subscriptionId: string, data: {
  channelType: number;
  channelAddress?: string;
  authType: number;
}) => {
  return put<EventSubscription>(`/apps/${appId}/events/${subscriptionId}/config`, data);
};

/**
 * 撤回事件权限申请
 */
export const withdrawEventSubscription = (appId: string, subscriptionId: string) => {
  return post<Subscription>(`/apps/${appId}/events/${subscriptionId}/withdraw`);
};

// ==================== 回调权限管理 ====================

/**
 * 获取应用回调订阅列表
 */
export const getAppCallbackSubscriptions = (appId: string, params?: {
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<CallbackSubscription[]>(`/apps/${appId}/callbacks`, params);
};

/**
 * 获取分类下回调权限列表（权限树懒加载）
 */
export const getCategoryCallbackPermissions = (categoryId: string, params?: {
  keyword?: string;
  needApproval?: number;
  includeChildren?: boolean;
  curPage?: number;
  pageSize?: number;
}) => {
  return get<CallbackPermission[]>(`/categories/${categoryId}/callbacks`, params);
};

/**
 * 申请回调权限（支持批量）
 */
export const subscribeCallbackPermissions = (appId: string, permissionIds: string[]) => {
  return post<{
    successCount: number;
    failedCount: number;
    records: Subscription[];
  }>(`/apps/${appId}/callbacks/subscribe`, { permissionIds });
};

/**
 * 配置回调消费参数
 */
export const configCallbackSubscription = (appId: string, subscriptionId: string, data: {
  channelType: number;
  channelAddress?: string;
  authType: number;
}) => {
  return put<CallbackSubscription>(`/apps/${appId}/callbacks/${subscriptionId}/config`, data);
};

/**
 * 撤回回调权限申请
 */
export const withdrawCallbackSubscription = (appId: string, subscriptionId: string) => {
  return post<Subscription>(`/apps/${appId}/callbacks/${subscriptionId}/withdraw`);
};
