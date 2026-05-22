import { get, post, put, del } from '@/utils/request';

/**
 * 回调管理相关接口
 */

// ==================== 类型定义 ====================

export interface Permission {
  id: string;
  nameCn: string;
  nameEn: string;
  scope: string;
  status?: number;
  needApproval?: number;
  approvalFlowId?: string;
}

export interface CallbackProperty {
  propertyName: string;
  propertyValue: string;
}

export interface Callback {
  id: string;
  nameCn: string;
  nameEn: string;
  categoryId: string;
  categoryName?: string;
  status: number; // 0=草稿, 1=待审, 2=已发布, 3=已下线
  permission: Permission;
  properties?: CallbackProperty[];
  createTime?: string;
  createBy?: string;
  lastUpdateTime?: string;
  lastUpdateBy?: string;
}

export interface CallbackListParams {
  categoryId?: string;
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}

export interface CreateCallbackParams {
  nameCn: string;
  nameEn: string;
  categoryId: string;
  permission: {
    nameCn: string;
    nameEn: string;
    scope: string;
    approvalFlowId?: string;
  };
  properties?: CallbackProperty[];
}

export interface UpdateCallbackParams {
  nameCn?: string;
  nameEn?: string;
  categoryId?: string;
  permission?: {
    nameCn?: string;
    nameEn?: string;
    scope?: string;
    approvalFlowId?: string;
  };
  properties?: CallbackProperty[];
}

// ==================== API 函数 ====================

/**
 * 获取回调列表
 */
export const getCallbackList = (params: CallbackListParams) => {
  return get<Callback[]>('/callbacks', params);
};

/**
 * 获取回调详情
 */
export const getCallbackDetail = (id: string) => {
  return get<Callback>(`/callbacks/${id}`);
};

/**
 * 注册回调
 */
export const createCallback = (data: CreateCallbackParams) => {
  return post<Callback>('/callbacks', data);
};

/**
 * 更新回调
 */
export const updateCallback = (id: string, data: UpdateCallbackParams) => {
  return put<Callback>(`/callbacks/${id}`, data);
};

/**
 * 删除回调
 */
export const deleteCallback = (id: string) => {
  return del<void>(`/callbacks/${id}`);
};

/**
 * 撤回回调
 */
export const withdrawCallback = (id: string) => {
  return post<Callback>(`/callbacks/${id}/withdraw`);
};
