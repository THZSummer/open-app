import { get, post, put, del } from '@/utils/request';
import type { ApiResponse, AxiosResponse } from '@/utils/request';

/**
 * 分页参数 - 后端使用 pageNum
 */
export interface PageParams {
  pageNum: number;
  pageSize: number;
}

/**
 * 分页结果 - 后端返回格式
 */
export interface PageResult<T> {
  list: T[];
  pageNum: number;
  pageSize: number;
  total: number;
}

/**
 * 分类管理 API 接口
 */

/**
 * 分类对象
 */
export interface Classify {
  classifyId: string;
  classifyCode: string;
  classifyName: string;
  path: string;
  classifyDesc: string;
  status: number; // 0-失效 1-有效
  createBy: string;
  createTime: string;
  lastUpdateBy: string;
  lastUpdateTime: string;
  itemCount?: number;
}

/**
 * LookUp 项对象
 */
export interface LookUpItem {
  itemId: string;
  classifyId: string;
  itemCode: string;
  itemName: string;
  itemValue: string;
  itemIndex: number;
  itemDesc: string;
  itemAttr1?: string;
  itemAttr2?: string;
  itemAttr3?: string;
  itemAttr4?: string;
  itemAttr5?: string;
  itemAttr6?: string;
  status: number; // 0-失效 1-有效
  createBy: string;
  createTime: string;
  lastUpdateBy: string;
  lastUpdateTime: string;
  classifyName?: string;
}

/**
 * 分类查询参数
 */
export interface ClassifyQueryParams extends PageParams {
  classifyCode?: string;
  classifyName?: string;
  classifyDesc?: string;
  status?: number;
}

/**
 * LookUp 项查询参数
 */
export interface ItemQueryParams extends PageParams {
  classifyId?: string;
  itemCode?: string;
  itemName?: string;
  status?: number;
}

/**
 * 分类创建/编辑表单
 */
export interface ClassifyForm {
  classifyCode: string;
  classifyName: string;
  path?: string;
  classifyDesc?: string;
  status?: number;
}

/**
 * LookUp 项创建/编辑表单
 */
export interface ItemForm {
  itemCode: string;
  itemName: string;
  itemValue: string;
  itemIndex: number;
  itemDesc?: string;
  itemAttr1?: string;
  itemAttr2?: string;
  itemAttr3?: string;
  itemAttr4?: string;
  itemAttr5?: string;
  itemAttr6?: string;
  status?: number;
}

/**
 * 导入结果
 */
export interface ImportResult {
  success: number;
  failed: number;
  errors: string[];
}

// ============================================
// 分类管理 API
// ============================================

/**
 * 获取分类列表
 * @param params 查询参数
 */
export const getClassifyList = (params: ClassifyQueryParams): Promise<AxiosResponse<ApiResponse<PageResult<Classify>>>> => {
  return get('/lookup/classify/list', params);
};

/**
 * 获取分类详情
 * @param classifyId 分类ID
 */
export const getClassifyDetail = (classifyId: string): Promise<AxiosResponse<ApiResponse<Classify>>> => {
  return get(`/lookup/classify/${classifyId}`);
};

/**
 * 新增分类
 * @param data 分类表单数据
 */
export const createClassify = (data: ClassifyForm): Promise<AxiosResponse<ApiResponse<Classify>>> => {
  return post('/lookup/classify', data);
};

/**
 * 编辑分类
 * @param classifyId 分类ID
 * @param data 分类表单数据
 */
export const updateClassify = (classifyId: string, data: ClassifyForm): Promise<AxiosResponse<ApiResponse<Classify>>> => {
  return put(`/lookup/classify/${classifyId}`, data);
};

/**
 * 删除分类
 * @param classifyId 分类ID
 */
export const deleteClassify = (classifyId: string): Promise<AxiosResponse<ApiResponse<void>>> => {
  return del(`/lookup/classify/${classifyId}`);
};

// ============================================
// LookUp 项管理 API
// ============================================

/**
 * 获取分类下的 LookUp 项列表
 * @param classifyId 分类ID
 * @param params 查询参数
 */
export const getItemList = (classifyId: string, params: ItemQueryParams): Promise<AxiosResponse<ApiResponse<PageResult<LookUpItem>>>> => {
  return get(`/lookup/classify/${classifyId}/items`, params);
};

/**
 * 获取 LookUp 项详情
 * @param itemId 项ID
 */
export const getItemDetail = (itemId: string): Promise<AxiosResponse<ApiResponse<LookUpItem>>> => {
  return get(`/lookup/items/${itemId}`);
};

/**
 * 新增 LookUp 项
 * @param classifyId 分类ID
 * @param data 项表单数据
 */
export const createItem = (classifyId: string, data: ItemForm): Promise<AxiosResponse<ApiResponse<LookUpItem>>> => {
  return post(`/lookup/classify/${classifyId}/items`, data);
};

/**
 * 编辑 LookUp 项
 * @param itemId 项ID
 * @param data 项表单数据
 */
export const updateItem = (itemId: string, data: ItemForm): Promise<AxiosResponse<ApiResponse<LookUpItem>>> => {
  return put(`/lookup/items/${itemId}`, data);
};

/**
 * 删除 LookUp 项
 * @param itemId 项ID
 */
export const deleteItem = (itemId: string): Promise<AxiosResponse<ApiResponse<void>>> => {
  return del(`/lookup/items/${itemId}`);
};

// ============================================
// 批量导入导出 API
// ============================================

/**
 * 下载导入模板
 */
export const downloadImportTemplate = (): Promise<Blob> => {
  return get('/lookup/import/template', {}, { responseType: 'blob' }).then((res: any) => res.data);
};

/**
 * 批量导入 LookUp 项
 * @param classifyId 分类ID
 * @param file Excel 文件
 */
export const importItems = (classifyId: string, file: File): Promise<AxiosResponse<ApiResponse<ImportResult>>> => {
  const formData = new FormData();
  formData.append('file', file);
  return post(`/lookup/import?classifyId=${classifyId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

/**
 * 导出 LookUp 项
 * @param classifyId 分类ID
 * @param params 导出筛选参数
 */
export const exportItems = (classifyId: string, params?: ItemQueryParams): Promise<Blob> => {
  return get('/lookup/export', { classifyId, ...params }, { responseType: 'blob' }).then((res: any) => res.data);
};

/**
 * 异步导入 LookUp 项（创建任务，立即返回）
 * @param classifyId 分类ID
 * @param file Excel 文件
 */
export const importItemsAsync = (classifyId: string, file: File): Promise<AxiosResponse<ApiResponse<{ taskId: number }>>> => {
  const formData = new FormData();
  formData.append('file', file);
  return post(`/lookup/import/async?classifyId=${classifyId}`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

/**
 * 异步导出 LookUp 项（创建任务，立即返回）
 * @param params 导出参数
 */
export const exportItemsAsync = (params: { classifyId?: string; status?: number; itemCode?: string; itemName?: string }): Promise<AxiosResponse<ApiResponse<{ taskId: number }>>> => {
  return post('/lookup/export/async', params);
};

/**
 * 下载任务结果文件
 * @param taskId 任务ID
 */
export const downloadTaskResult = (taskId: string): Promise<Blob> => {
  return get(`/lookup/tasks/${taskId}/download`, {}, { responseType: 'blob' }).then((res: any) => res.data);
};

// ============================================
// 任务管理 API
// ============================================

/**
 * 任务对象
 */
export interface Task {
  taskId: string;
  taskType: number;   // 1-导入，2-导出
  bizType: number;    // 1-LookUp，2-数据字典
  status: number;     // 0-待处理，1-处理中，2-已完成，3-失败
  fileId: string;
  fileName: string;
  result: string;
  createBy: string;
  createTime: string;
  lastUpdateBy: string;
  lastUpdateTime: string;
}

/**
 * 任务查询参数
 */
export interface TaskQueryParams extends PageParams {
  taskType?: number;
  bizType?: number;
  status?: number;
}

/**
 * 任务创建参数
 */
export interface TaskCreateParams {
  taskType: number;
  bizType: number;
  fileName?: string;
  fileId?: string;
}

/**
 * 任务状态更新参数
 */
export interface TaskStatusUpdateParams {
  status: number;
  result?: string;
}

/**
 * 获取任务列表
 * @param params 查询参数
 */
export const getTaskList = (params: TaskQueryParams): Promise<AxiosResponse<ApiResponse<PageResult<Task>>>> => {
  return get('/lookup/tasks', params);
};

/**
 * 获取任务详情
 * @param taskId 任务ID
 */
export const getTaskDetail = (taskId: string): Promise<AxiosResponse<ApiResponse<Task>>> => {
  return get(`/lookup/tasks/${taskId}`);
};

/**
 * 创建任务
 * @param data 任务创建参数
 */
export const createTask = (data: TaskCreateParams): Promise<AxiosResponse<ApiResponse<{ taskId: string }>>> => {
  return post('/lookup/tasks', data);
};

/**
 * 更新任务状态
 * @param taskId 任务ID
 * @param data 任务状态更新参数
 */
export const updateTaskStatus = (taskId: string, data: TaskStatusUpdateParams): Promise<AxiosResponse<ApiResponse<void>>> => {
  return put(`/lookup/tasks/${taskId}/status`, data);
};
