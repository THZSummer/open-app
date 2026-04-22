import axios, { AxiosInstance, InternalAxiosRequestConfig, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { message } from 'antd';

/**
 * 统一响应格式
 */
export interface ApiResponse<T = any> {
  code: string;
  messageZh: string;
  messageEn: string;
  data: T;
  page: {
    curPage: number;
    pageSize: number;
    total: number;
    totalPage: number;
  } | null;
}

/**
 * 分页参数
 */
export interface PageParams {
  curPage?: number;
  pageSize?: number;
}

/**
 * 创建 axios 实例
 */
const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/**
 * 请求拦截器
 * - 添加 Token
 * - 添加应用信息
 */
instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // TODO: 从状态管理或 localStorage 获取 Token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // TODO: 从状态管理获取应用信息
    const appId = localStorage.getItem('appId');
    if (appId) {
      config.headers['X-App-Id'] = appId;
    }

    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

/**
 * 响应拦截器
 * - 统一错误处理
 * - Token 过期处理
 */
instance.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { data } = response;

    // 业务错误处理
    if (data.code !== '200') {
      message.error(data.messageZh || '请求失败');
      return Promise.reject(new Error(data.messageZh || '请求失败'));
    }

    return response;
  },
  (error: AxiosError<ApiResponse>) => {
    // HTTP 错误处理
    if (error.response) {
      const { status, data } = error.response;

      switch (status) {
        case 401:
          message.error('未授权，请先登录');
          // TODO: 跳转到登录页
          // window.location.href = '/login';
          break;
        case 403:
          message.error('没有权限访问');
          break;
        case 404:
          message.error('请求的资源不存在');
          break;
        case 500:
          message.error('服务器内部错误');
          break;
        default:
          message.error(data?.messageZh || '请求失败');
      }
    } else if (error.request) {
      message.error('网络错误，请检查网络连接');
    } else {
      message.error('请求配置错误');
    }

    return Promise.reject(error);
  }
);

/**
 * GET 请求
 */
export const get = <T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<AxiosResponse<ApiResponse<T>>> => {
  return instance.get(url, { params, ...config });
};

/**
 * POST 请求
 */
export const post = <T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<AxiosResponse<ApiResponse<T>>> => {
  return instance.post(url, data, config);
};

/**
 * PUT 请求
 */
export const put = <T = any>(
  url: string,
  data?: any,
  config?: AxiosRequestConfig
): Promise<AxiosResponse<ApiResponse<T>>> => {
  return instance.put(url, data, config);
};

/**
 * DELETE 请求
 */
export const del = <T = any>(
  url: string,
  params?: any,
  config?: AxiosRequestConfig
): Promise<AxiosResponse<ApiResponse<T>>> => {
  return instance.delete(url, { params, ...config });
};

export default instance;
