package com.xxx.api.internal.mapper;

import com.xxx.api.internal.entity.AppMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppMemberEntityMapper {
    List<AppMemberEntity> selectByAppIdAndAccountId(
            @Param("appId") Long appId,
            @Param("accountId") String accountId);
}
