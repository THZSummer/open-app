/**
 * ========================================
 * 连接器编辑页 - Mock数据
 * ========================================
 *
 * 提供连接器编辑相关API的mock数据
 */

import { mockConnectors } from '../Connector/mock';

/**
 * 生成标准的成功响应
 *
 * @param {Object} options - 响应配置
 * @param {Object} options.data - 响应数据
 * @param {string} [options.message='操作成功'] - 响应消息
 * @returns {Object}
 */
const generateSuccessResponse = ({
  data,
  message = '操作成功',
}) => {
  const response = { code: '200', message };
  if (data !== undefined) {
    response.data = data;
  }
  return response;
};

/**
 * 生成标准的错误响应
 *
 * @param {Object} options - 响应配置
 * @param {string} [options.code='500'] - 错误码
 * @param {string} [options.message='操作失败'] - 错误消息
 * @returns {Object}
 */
const generateErrorResponse = ({
  code = '500',
  message = '操作失败',
}) => {
  return { code, message };
};

/**
 * 获取连接器详情
 *
 * @param {string} id - 连接器ID
 * @returns {Promise<Object>}
 */
export const mockFetchConnectorDetail = async (id) => {
  await new Promise(resolve => setTimeout(resolve, 200));

  const connector = mockConnectors.find(item => item.id === id);

  if (!connector) {
    return generateErrorResponse({
      code: '404',
      message: '连接器不存在',
    });
  }

  return generateSuccessResponse({
    data: connector,
  });
};

/**
 * 创建连接器
 *
 * @param {Object} data - 连接器数据
 * @returns {Promise<Object>}
 */
export const mockCreateConnector = async (data) => {
  await new Promise(resolve => setTimeout(resolve, 400));

  const newConnector = {
    id: `connector-${Date.now()}`,
    ...data,
    triggers: data.triggers || [],
    actions: data.actions || [],
    createdAt: new Date().toLocaleString(),
    updatedAt: new Date().toLocaleString(),
  };

  mockConnectors.push(newConnector);

  return generateSuccessResponse({
    data: newConnector,
    message: '连接器创建成功',
  });
};

/**
 * 更新连接器
 *
 * @param {string} id - 连接器ID
 * @param {Object} data - 更新数据
 * @returns {Promise<Object>}
 */
export const mockUpdateConnector = async (id, data) => {
  await new Promise(resolve => setTimeout(resolve, 300));

  const index = mockConnectors.findIndex(item => item.id === id);

  if (index === -1) {
    return generateErrorResponse({
      code: '404',
      message: '连接器不存在',
    });
  }

  mockConnectors[index] = {
    ...mockConnectors[index],
    ...data,
    updatedAt: new Date().toLocaleString(),
  };

  return generateSuccessResponse({
    data: mockConnectors[index],
    message: '连接器更新成功',
  });
};
