package com.xxx.it.works.wecode.v2.modules.member.mapper;

import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用成员 Mapper
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Mapper
public interface AppMemberMapper {

    List<AppMember> selectByAppId(
            @Param("appId") Long appId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countByAppId(@Param("appId") Long appId);

    List<AppMember> selectByAppIdAndAccountId(
            @Param("appId") Long appId,
            @Param("accountId") String accountId);

    /**
     * 按 appIds + accountId 批量查询（如批量查多个应用下某用户的成员记录）
     */
    List<AppMember> selectByAppIdsAndAccountId(
            @Param("appIds") List<Long> appIds,
            @Param("accountId") String accountId);

    AppMember selectById(@Param("id") Long id);

    int insert(AppMember member);

    int update(AppMember member);

    int deleteById(@Param("id") Long id);

    /**
     * 按 appId + accountId + memberType 查询单条记录（精确匹配角色）
     */
    AppMember selectByAppIdAccountIdAndType(
            @Param("appId") Long appId,
            @Param("accountId") String accountId,
            @Param("memberType") Integer memberType);

    /**
     * 按 appIds + memberType 批量查询（如批量查多个应用的 Owner）
     */
    List<AppMember> selectByAppIdsAndMemberType(
            @Param("appIds") List<Long> appIds,
            @Param("memberType") Integer memberType);

    /**
     * 按 appId + accountIds + memberType 批量查询（校验角色重复）
     */
    List<AppMember> selectByAppIdAndAccountIdsAndType(
            @Param("appId") Long appId,
            @Param("accountIds") List<String> accountIds,
            @Param("memberType") Integer memberType);

    /**
     * 批量插入成员
     */
    int insertBatch(@Param("list") List<AppMember> members);
}
