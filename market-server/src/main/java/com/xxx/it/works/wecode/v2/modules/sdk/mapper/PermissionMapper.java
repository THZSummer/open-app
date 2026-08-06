package com.xxx.it.works.wecode.v2.modules.sdk.mapper;

import com.xxx.it.works.wecode.v2.modules.sdk.vo.SdkPermissionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限查询 Mapper（共库联查 open 的权限表）
 */
@Mapper
public interface PermissionMapper {

    /**
     * 按 categoryAlias 查询 SDK 权限分类下的启用权限列表
     */
    List<SdkPermissionVO> selectByCategoryAlias(@Param("categoryAlias") String categoryAlias);

    /**
     * 校验某权限ID是否存在于 SDK 权限分类下
     */
    int countByIdAndCategoryAlias(@Param("id") Long id,
                                  @Param("categoryAlias") String categoryAlias);
}
