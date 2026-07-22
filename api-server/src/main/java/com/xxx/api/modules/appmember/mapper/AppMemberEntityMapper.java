package com.xxx.api.modules.appmember.mapper;

import com.xxx.api.modules.appmember.entity.AppMemberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppMemberEntityMapper {

    List<AppMemberEntity> selectByAppId(@Param("appId") Long appId);
}
