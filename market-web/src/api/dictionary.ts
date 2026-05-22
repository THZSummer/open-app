import { get, post, put, del } from '@/utils/request';
import type { ApiResponse, AxiosResponse } from '@/utils/request';

export interface PageParams {
  pageNum: number;
  pageSize: number;
}

export interface PageResult<T> {
  list: T[];
  pageNum: number;
  pageSize: number;
  total: number;
}

export interface Dictionary {
  id: string;
  code: string;
  name: string;
  value: string;
  description: string;
  path: string;
  language: number;
  status: number;
  createBy: string;
  createTime: string;
  lastUpdateBy: string;
  lastUpdateTime: string;
}

export interface DictionaryQueryParams extends PageParams {
  code?: string;
  name?: string;
  path?: string;
  language?: number;
  status?: number;
}

export interface DictionaryForm {
  code: string;
  name: string;
  value?: string;
  path?: string;
  description?: string;
  language?: number;
  status?: number;
}

export interface Task {
  taskId: string;
  taskType: number;
  bizType: number;
  status: number;
  fileId: string;
  fileName: string;
  result: string;
  createBy: string;
  createTime: string;
  lastUpdateBy: string;
  lastUpdateTime: string;
}

export interface TaskQueryParams extends PageParams {
  taskType?: number;
  bizType?: number;
  status?: number;
}

export const getDictionaryList = (params: DictionaryQueryParams): Promise<AxiosResponse<ApiResponse<PageResult<Dictionary>>>> => {
  return get('/dictionary/list', params);
};

export const getDictionaryDetail = (id: string): Promise<AxiosResponse<ApiResponse<Dictionary>>> => {
  return get(`/dictionary/${id}`);
};

export const createDictionary = (data: DictionaryForm): Promise<AxiosResponse<ApiResponse<Dictionary>>> => {
  return post('/dictionary', data);
};

export const updateDictionary = (id: string, data: DictionaryForm): Promise<AxiosResponse<ApiResponse<Dictionary>>> => {
  return put(`/dictionary/${id}`, data);
};

export const deleteDictionary = (id: string): Promise<AxiosResponse<ApiResponse<void>>> => {
  return del(`/dictionary/${id}`);
};

export const downloadImportTemplate = (): Promise<Blob> => {
  return get('/dictionary/import/template', {}, { responseType: 'blob' }).then((res: any) => res.data);
};

export const submitImportTask = (file: File): Promise<AxiosResponse<ApiResponse<{ taskId: string }>>> => {
  const formData = new FormData();
  formData.append('file', file);
  return post('/dictionary/import/async', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

export const submitExportTask = (params?: { selectedIds?: string[]; filters?: DictionaryQueryParams }): Promise<AxiosResponse<ApiResponse<{ taskId: string }>>> => {
  return post('/dictionary/export/async', { params });
};

export const getTaskList = (params: TaskQueryParams): Promise<AxiosResponse<ApiResponse<PageResult<Task>>>> => {
  return get('/lookup/tasks', params);
};

export const getTaskProgress = (taskId: string): Promise<AxiosResponse<ApiResponse<{ progress: number; status: number; successCount: number; failCount: number }>>> => {
  return get(`/lookup/task/progress/${taskId}`);
};

export const downloadTaskResult = (taskId: string): Promise<Blob> => {
  return get(`/lookup/tasks/${taskId}/download`, {}, { responseType: 'blob' }).then((res: any) => res.data);
};
