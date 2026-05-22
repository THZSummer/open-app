import { get, post, put, del } from '@/utils/request';

/**
 * 分类相关接口
 */

// ==================== 类型定义 ====================

export interface Category {
  id: string;
  categoryAlias?: string;
  nameCn: string;
  nameEn: string;
  parentId?: string;
  path: string;
  sortOrder: number;
  status: number;
  children?: Category[];
  createTime?: string;
  createBy?: string;
  lastUpdateTime?: string;
  lastUpdateBy?: string;
}

export interface CategoryOwner {
  id: string;
  categoryId: string;
  userId: string;
  userName: string;
}

export interface CreateCategoryParams {
  categoryAlias?: string;
  nameCn: string;
  nameEn: string;
  parentId?: string;
  sortOrder?: number;
}

export interface UpdateCategoryParams {
  nameCn: string;
  nameEn: string;
  sortOrder?: number;
}

export interface AddOwnerParams {
  userId: string;
  userName?: string;
}

// ==================== API 函数 ====================

/**
 * 获取分类列表（树形结构）
 */
export const getCategoryTree = (categoryAlias?: string) => {
  return get<Category[]>('/categories', { categoryAlias });
};

/**
 * 获取分类详情
 */
export const getCategoryDetail = (id: string) => {
  return get<Category>(`/categories/${id}`);
};

/**
 * 创建分类
 */
export const createCategory = (data: CreateCategoryParams) => {
  return post<Category>('/categories', data);
};

/**
 * 更新分类
 */
export const updateCategory = (id: string, data: UpdateCategoryParams) => {
  return put<Category>(`/categories/${id}`, data);
};

/**
 * 删除分类
 */
export const deleteCategory = (id: string) => {
  return del<void>(`/categories/${id}`);
};

/**
 * 获取分类责任人列表
 */
export const getCategoryOwners = (categoryId: string) => {
  return get<CategoryOwner[]>(`/categories/${categoryId}/owners`);
};

/**
 * 添加分类责任人
 */
export const addCategoryOwner = (categoryId: string, data: AddOwnerParams) => {
  return post<CategoryOwner>(`/categories/${categoryId}/owners`, data);
};

/**
 * 移除分类责任人
 */
export const removeCategoryOwner = (categoryId: string, userId: string) => {
  return del<void>(`/categories/${categoryId}/owners/${userId}`);
};
