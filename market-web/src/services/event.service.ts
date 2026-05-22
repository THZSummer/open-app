import { get, post, put, del } from '@/utils/request';

/**
 * 事件管理相关接口
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

export interface EventProperty {
  propertyName: string;
  propertyValue: string;
}

export interface Event {
  id: string;
  nameCn: string;
  nameEn: string;
  topic: string;
  categoryId: string;
  categoryName?: string;
  status: number; // 0=草稿, 1=待审, 2=已发布, 3=已下线
  permission: Permission;
  properties?: EventProperty[];
  createTime?: string;
  createBy?: string;
  lastUpdateTime?: string;
  lastUpdateBy?: string;
}

export interface EventListParams {
  categoryId?: string;
  status?: number;
  keyword?: string;
  curPage?: number;
  pageSize?: number;
}

export interface CreateEventParams {
  nameCn: string;
  nameEn: string;
  topic: string;
  categoryId: string;
  permission: {
    nameCn: string;
    nameEn: string;
    scope: string;
    approvalFlowId?: string;
  };
  properties?: EventProperty[];
}

export interface UpdateEventParams {
  nameCn?: string;
  nameEn?: string;
  topic?: string;
  categoryId?: string;
  permission?: {
    nameCn?: string;
    nameEn?: string;
    scope?: string;
    approvalFlowId?: string;
  };
  properties?: EventProperty[];
}

// ==================== API 函数 ====================

/**
 * 获取事件列表
 */
export const getEventList = (params: EventListParams) => {
  return get<Event[]>('/events', params);
};

/**
 * 获取事件详情
 */
export const getEventDetail = (id: string) => {
  return get<Event>(`/events/${id}`);
};

/**
 * 注册事件
 */
export const createEvent = (data: CreateEventParams) => {
  return post<Event>('/events', data);
};

/**
 * 更新事件
 */
export const updateEvent = (id: string, data: UpdateEventParams) => {
  return put<Event>(`/events/${id}`, data);
};

/**
 * 删除事件
 */
export const deleteEvent = (id: string) => {
  return del<void>(`/events/${id}`);
};

/**
 * 撤回事件
 */
export const withdrawEvent = (id: string) => {
  return post<Event>(`/events/${id}/withdraw`);
};
