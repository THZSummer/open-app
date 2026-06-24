import { useEffect, useRef } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { ROUTE_VERSION_MAP, NEW_PAGE_ROUTES, fetchWhitelist } from '../configs/web.config';
import { queryParams } from '../utils/common';

/**
 * 灰度发布路由守卫 Hook（基于应用白名单）
 *
 * 对应 frontend-design.md §18「新旧 UI 动态切换」
 *
 * 核心逻辑：
 *   1. 监听 location.pathname 变化
 *   2. 判断当前路径是否在新旧路由映射表内
 *   3. 拉取应用白名单（带缓存），提取 appId 列表
 *   4. 判断规则：
 *      - 白名单为空 → 所有应用走新页面（灰度全量）
 *      - 白名单非空 → isInWhitelist = appIds.includes(currentAppId)
 *   5. 4 个分支：
 *      - isInWhitelist/useNewPage && 当前是新页面 → 不动
 *      - isInWhitelist/useNewPage && 当前是旧页面 → 跳新页面
 *      - !isInWhitelist/useNewPage && 当前是新页面 → 跳旧页面
 *      - !isInWhitelist/useNewPage && 当前是旧页面 → 不动
 *   6. 跳转时保留 search 参数（避免 appId 等丢失）
 *   7. 同一 pathname 不重复触发
 *   8. 应用列表页（无 appId 参数）→ 始终走新页面
 */
export function useRouteWhitelistGuard() {
  const location = useLocation();
  const navigate = useNavigate();
  const lastPathnameRef = useRef(null);

  useEffect(() => {
    const pathname = location.pathname;
    // 同一 pathname 不重复处理
    if (lastPathnameRef.current === pathname) return;
    lastPathnameRef.current = pathname;

    // 判断当前路径是否在映射表中
    // oldPath: 如果当前是新页面，找到对应的旧路径；如果当前是旧路径，oldPath 就是自身
    const oldPath = Object.keys(ROUTE_VERSION_MAP).find(
      (k) => ROUTE_VERSION_MAP[k] === pathname
    ) || (Object.keys(ROUTE_VERSION_MAP).includes(pathname) ? pathname : null);
    const isNewPage = NEW_PAGE_ROUTES.includes(pathname);
    const isOldPageInMap = !!oldPath;

    // 既不是新页面，也不在映射表的旧路径里 → 不处理（如 /api-management、/admin/*）
    if (!isNewPage && !isOldPageInMap) return;

    const run = async () => {
      // 从 URL 获取当前 appId
      const currentAppId = queryParams('appId');

      // 应用列表页（无 appId）→ 始终走新页面
      if (!currentAppId) {
        if (!isNewPage && oldPath) {
          navigate(ROUTE_VERSION_MAP[oldPath] + location.search, { replace: true });
          lastPathnameRef.current = ROUTE_VERSION_MAP[oldPath];
        }
        return;
      }

      // 拉取应用白名单（已解析为 appId 数组）
      const appIds = await fetchWhitelist();

      // 核心判断（严格按 frontend-design.md §18.3）：
      //   - 白名单为空 → 所有应用走新页面（灰度全量）
      //   - 白名单非空 → useNewPage = appIds.includes(currentAppId)
      const useNewPage = appIds.length === 0 || appIds.includes(currentAppId);

      // 同向不动
      if (isNewPage === useNewPage) return;

      // 否则切换
      const target = isNewPage ? oldPath : ROUTE_VERSION_MAP[oldPath];
      if (target) {
        navigate(target + location.search, { replace: true });
        lastPathnameRef.current = target;
      }
    };
    run();
  }, [location.pathname, location.search, navigate]);
}
