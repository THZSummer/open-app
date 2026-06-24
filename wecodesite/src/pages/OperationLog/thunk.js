import { API_CONFIG, buildApiUrl, fetchApi } from '../../configs/web.config';

/**
 * 分页查询操作日志
 * 后端接口: GET /service/open/v2/operate-log
 */
export const fetchOperationLogList = async ({ page, pageSize, filters = {} }) => {
  try {
    const params = {
      curPage: page,
      pageSize,
    };
    if (filters.account) params.operateUser = filters.account;
    if (filters.operationObject) params.operateObject = filters.operationObject;
    if (filters.operationTime?.length === 2) {
      params.startTime = filters.operationTime[0];
      params.endTime = filters.operationTime[1];
    }
    const result = await fetchApi('/operate-log', { params });
    if (result?.code === '200') {
      return {
        data: (result.data || []).map(item => ({ ...item, key: item.id })),
        total: result.page?.total || 0,
      };
    }
    return { data: [], total: 0 };
  } catch (err) {
    console.error('fetchOperationLogList error:', err);
    return { data: [], total: 0 };
  }
};

/**
 * 获取操作日志筛选条件
 * 后端接口: GET /service/open/v2/operate-log/filters
 */
export const fetchOperationLogFilters = async () => {
  try {
    const result = await fetchApi('/operate-log/filters');
    if (result?.code === '200') {
      return result.data;
    }
    return { operationTypes: [], operationObjects: [] };
  } catch (err) {
    console.error('fetchOperationLogFilters error:', err);
    return { operationTypes: [], operationObjects: [] };
  }
};
