package com.xxx.it.works.wecode.v2.modules.app.resolver;

/**
 * 应用上下文解析器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>1. 外部长ID转换为内部短ID</li>
 *   <li>2. 校验当前用户对该应用的访问权限</li>
 * </ul>
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface AppContextResolver {

    /**
     * 解析并校验应用访问权限
     *
     * @param externalAppId 外部业务ID（长ID）
     * @return 应用上下文信息
     * @throws AppAccessException 当ID无效或无访问权限时抛出
     */
    AppContext resolveAndValidate(String externalAppId);

    /**
     * 将内部ID转换为外部ID
     *
     * @param internalId 内部主键ID（短ID）
     * @return 外部业务ID（长ID）
     */
    String toExternalId(Long internalId);
}