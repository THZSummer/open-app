package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.mapper;

import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.entity.AppPropertyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 机器人绑定 Mapper
 *
 * <p>操作 openplatform_app_p_t 表，所有查询显式列出字段，禁止 SELECT *</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Mapper
public interface ChatbotBindMapper {

    /**
     * 根据应用业务 ID 查询应用主键 ID
     *
     * @param appId 应用业务 ID（openplatform_app_t.app_id）
     * @return 应用主键 ID（不存在返回 null）
     */
    Long selectAppPkIdByAppId(@Param("appId") String appId);

    /**
     * 查询应用已绑定的机器人账号列表
     *
     * @param appPkId 应用主键 ID
     * @return 绑定记录列表（按 create_time DESC 排序）
     */
    List<AppPropertyEntity> selectBoundAccounts(@Param("appPkId") Long appPkId);

    /**
     * 统计应用已绑定的机器人账号数量
     *
     * @param appPkId 应用主键 ID
     * @return 绑定数量
     */
    int countBoundAccounts(@Param("appPkId") Long appPkId);

    /**
     * 检查指定账号是否已绑定
     *
     * @param appPkId   应用主键 ID
     * @param accountId 机器人账号 ID
     * @return 已存在的记录（不存在返回 null）
     */
    AppPropertyEntity selectByAppPkIdAndAccountId(@Param("appPkId") Long appPkId,
                                                   @Param("accountId") String accountId);

    /**
     * 插入绑定记录
     *
     * @param entity 绑定记录（id 由 IdGeneratorStrategy 生成）
     * @return 影响行数
     */
    int insert(AppPropertyEntity entity);

    /**
     * 删除绑定记录（硬删除）
     *
     * @param appPkId   应用主键 ID
     * @param accountId 机器人账号 ID
     * @return 影响行数
     */
    int deleteByAppPkIdAndAccountId(@Param("appPkId") Long appPkId,
                                     @Param("accountId") String accountId);
}
