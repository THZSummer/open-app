package com.xxx.api.scope.mapper;

import com.xxx.api.scope.entity.UserAuthorization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户授权 Mapper 接口
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface UserAuthorizationMapper {

    /**
     * 插入用户授权
     */
    int insert(UserAuthorization authorization);

    /**
     * 根据ID查询用户授权
     */
    UserAuthorization selectById(@Param("id") Long id);

    /**
     * 根据用户ID和应用ID查询授权
     */
    UserAuthorization selectByUserIdAndAppId(
            @Param("userId") String userId,
            @Param("appId") Long appId);

    /**
     * 查询用户授权列表
     */
    List<UserAuthorization> selectList(
            @Param("userId") String userId,
            @Param("appId") Long appId,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计用户授权数量
     */
    Long countList(
            @Param("userId") String userId,
            @Param("appId") Long appId,
            @Param("keyword") String keyword);

    /**
     * 更新用户授权
     */
    int update(UserAuthorization authorization);

    /**
     * 撤销授权（设置 revoked_at）
     */
    int revokeById(@Param("id") Long id);

    /**
     * 删除用户授权
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询应用的有效授权列表（未过期、未撤销）
     */
    List<UserAuthorization> selectValidAuthorizationsByAppId(@Param("appId") Long appId);

    /**
     * 查询用户对应用的有效授权
     */
    UserAuthorization selectValidAuthorization(
            @Param("userId") String userId,
            @Param("appId") Long appId);
}
