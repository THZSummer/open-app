/**
 * 能力目录管理 API 接口
 *
 * 提供能力目录列表查询等接口调用。
 * 接口基础路径：/service/open/v2/ability/admin
 */
import API_CONFIG from '../../../configs/web.config';
import { fetchApi } from '../../../utils/webFetch';

/**
 * 获取能力目录列表
 *
 * @param {Object} [params={}] - 查询参数
 * @param {number} [params.curPage] - 当前页码
 * @param {number} [params.pageSize] - 每页条数
 * @param {string} [params.keyword] - 搜索关键词
 * @param {string} [params.sortField] - 排序字段
 * @param {string} [params.sortOrder] - 排序方向 asc|desc
 * @returns {Promise<Object>} 能力列表数据
 */
export const getAbilityList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.ABILITY_LIST, {
      method: 'GET',
      params,
    });
    return result;
  } catch (err) {
    return {
      code: '500',
      messageZh: '请求失败',
      messageEn: 'Request failed',
      data: [],
      page: { curPage: 1, pageSize: 20, total: 0 },
    };
  }
};
