/**
 * 能力目录管理 API 接口
 *
 * 提供能力目录列表查询等接口调用。
 * 接口基础路径：/service/open/v2/ability/admin
 */
import API_CONFIG from '../../../configs/web.config';
import { fetchApi } from '../../../utils/webFetch';

/**
 * 能力列表查询参数
 */
export interface AbilityListParams {
  curPage?: number;
  pageSize?: number;
  keyword?: string;
  sortField?: string;
  sortOrder?: 'asc' | 'desc';
}

/**
 * 能力列表项
 */
export interface AbilityListItem {
  abilityType: number;
  nameCn: string;
  nameEn: string;
  descCn: string;
  descEn: string;
  icon: string;
  iconUrl: string;
  exampleDiagram: string;
  exampleDiagramUrl: string;
  orderNum: number;
  entryUrl: string;
  hidden: number;
  routePath: string;
  aliasName: string;
  requireRelease: number;
  loadType: number;
  createTime: string;
  updateBy: string;
  updateTime: string;
}

/**
 * 分页信息
 */
export interface PageInfo {
  curPage: number;
  pageSize: number;
  total: number;
}

/**
 * 列表查询响应
 */
export interface AbilityListResponse {
  code: string;
  message: string;
  data: AbilityListItem[];
  page: PageInfo;
}

/**
 * 获取能力目录列表
 *
 * @param params - 查询参数，包含分页、关键词搜索和排序
 * @returns 能力列表数据
 */
export const getAbilityList = async (
  params: AbilityListParams = {}
): Promise<AbilityListResponse> => {
  try {
    const result = await fetchApi(API_CONFIG.ABILITY_LIST, {
      method: 'GET',
      params,
    });
    return result as AbilityListResponse;
  } catch (err) {
    return {
      code: '500',
      message: '请求失败',
      data: [],
      page: { curPage: 1, pageSize: 20, total: 0 },
    };
  }
};
