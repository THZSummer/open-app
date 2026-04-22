import { operationTypes, operationObjects, mockAccounts, mockIps, mockResults } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const generateMockData = (page, pageSize, filters = {}) => {
  const total = 156;
  const data = [];

  for (let i = 0; i < pageSize; i++) {
    const seq = (page - 1) * pageSize + i + 1;
    const date = new Date();
    date.setDate(date.getDate() - Math.floor(Math.random() * 180));

    data.push({
      key: seq,
      id: seq,
      account: mockAccounts[Math.floor(Math.random() * mockAccounts.length)],
      operationType: operationTypes[Math.floor(Math.random() * operationTypes.length)].value,
      operationObject: operationObjects[Math.floor(Math.random() * operationObjects.length)].value,
      description: `操作描述_${seq}`,
      ip: mockIps[Math.floor(Math.random() * mockIps.length)],
      time: date.toISOString().slice(0, 19).replace('T', ' '),
      result: mockResults[Math.floor(Math.random() * mockResults.length)],
    });
  }

  return { data, total };
};

export const fetchOperationLogList = async ({ page, pageSize, filters = {} }) => {
  await delay(300);
  return generateMockData(page, pageSize, filters);
};

export const fetchOperationLogFilters = async () => {
  await delay(100);
  return {
    operationTypes,
    operationObjects,
  };
};