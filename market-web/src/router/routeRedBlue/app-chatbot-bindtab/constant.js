/**
 * 机器人绑定模块常量
 */

/**
 * 错误码到消息的映射
 */
export const ERROR_CODE_MAP = {
  40001: '应用不存在',
  40002: '账号无效',
  40003: '超过最大可绑定数量',
  40004: '该账号已绑定',
  40005: '绑定记录不存在',
  40006: '通讯录服务暂不可用',
};

/**
 * 根据错误码获取错误消息
 * @param {string|number} code - 错误码
 * @returns {string} 错误消息
 */
export const getErrorMessage = (code) => {
  return ERROR_CODE_MAP[code] || '操作失败';
};

/**
 * 属性名称常量
 */
export const PROPERTY_NAME = {
  SINGLE_CHATBOT_ACCOUNT: 'single_chatbot_account',
};
