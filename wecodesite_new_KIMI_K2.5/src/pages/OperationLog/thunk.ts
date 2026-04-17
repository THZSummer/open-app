import { mockLogs, mockOperationTypes, mockOperationObjects } from './mock';

export const fetchOperationLogList = async (filters: {
  account?: string;
  object?: string;
  startTime?: string;
  endTime?: string;
  page: number;
  pageSize: number;
}): Promise<{ list: typeof mockLogs; total: number }> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      let filtered = mockLogs;
      if (filters.account) {
        filtered = filtered.filter(log => log.account.includes(filters.account!));
      }
      if (filters.object && filters.object !== '全部') {
        filtered = filtered.filter(log => log.object === filters.object);
      }
      const start = (filters.page - 1) * filters.pageSize;
      const end = start + filters.pageSize;
      resolve({
        list: filtered.slice(start, end),
        total: filtered.length,
      });
    }, 500);
  });
};

export const fetchOperationLogFilters = async (): Promise<{
  operationTypes: string[];
  operationObjects: string[];
}> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        operationTypes: mockOperationTypes,
        operationObjects: mockOperationObjects,
      });
    }, 200);
  });
};
