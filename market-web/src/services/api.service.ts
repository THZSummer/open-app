import { get, post, put, del } from '@/utils/request';

/**
 * API 管理相关接口
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

export interface ApiProperty {
  propertyName: string;
  propertyValue: string;
}

export interface Api {
  id: string;
  nameCn: string;
  nameEn: string;
  path: string;
  method: string;
  categoryId: string;
  categoryName?: string;
  status: number; // 0=草稿, 1=待审, 2=已发布, 3=已下线
  permission: Permission;
  properties?: ApiProperty[];
  createTime?: string;
  createBy?: string;
  lastUpdateTime?: string;
  lastUpdateBy?: string;
}

export interface ApiListParams {
  categoryId?: string;
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}

export interface CreateApiParams {
  nameCn: string;
  nameEn: string;
  path: string;
  method: string;
  categoryId: string;
  permission: {
    nameCn: string;
    nameEn: string;
    scope: string;
    approvalFlowId?: string;
  };
  properties?: ApiProperty[];
}

export interface UpdateApiParams {
  nameCn?: string;
  nameEn?: string;
  path?: string;
  method?: string;
  categoryId?: string;
  permission?: {
    nameCn?: string;
    nameEn?: string;
    scope?: string;
    approvalFlowId?: string;
  };
  properties?: ApiProperty[];
}

// ==================== API 函数 ====================

/**
 * 获取 API 列表
 */
export const getApiList = (params: ApiListParams) => {
  return get<Api[]>('/apis', params);
};

/**
 * 获取 API 详情
 */
export const getApiDetail = (id: string) => {
  return get<Api>(`/apis/${id}`);
};

/**
 * 注册 API
 */
export const createApi = (data: CreateApiParams) => {
  return post<Api>('/apis', data);
};

/**
 * 更新 API
 */
export const updateApi = (id: string, data: UpdateApiParams) => {
  return put<Api>(`/apis/${id}`, data);
};

/**
 * 删除 API
 */
export const deleteApi = (id: string) => {
  return del<void>(`/apis/${id}`);
};

/**
 * 撤回 API
 */
export const withdrawApi = (id: string) => {
  return post<Api>(`/apis/${id}/withdraw`);
};
