package com.xxx.it.works.wecode.v2.modules.sdk.mapper;

import com.xxx.it.works.wecode.v2.modules.sdk.entity.SdkVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * SDK版本 Mapper
 */
@Mapper
public interface SdkVersionMapper {

    /**
     * 插入SDK版本记录
     */
    int insert(SdkVersionEntity entity);

    /**
     * 选择性更新（仅更新非空字段）
     */
    int updateByPrimaryKeySelective(SdkVersionEntity entity);

    /**
     * 全字段更新（null 也写入，用于编辑场景将字段更新为空）
     */
    int updateByPrimaryKey(SdkVersionEntity entity);

    /**
     * 按主键查询
     */
    SdkVersionEntity selectByPrimaryKey(@Param("id") Long id);

    /**
     * 按主键查询（含关联权限名称）
     */
    SdkVersionEntity selectDetailByPrimaryKey(@Param("id") Long id);

    /**
     * 分页查询（含关联权限名称）
     */
    List<SdkVersionEntity> selectPage(@Param("status") Integer status,
                                       @Param("versionName") String versionName,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    /**
     * 分页计数
     */
    long countByPage(@Param("status") Integer status,
                     @Param("versionName") String versionName);

    /**
     * 查重：(versionName, permissionId) 联合唯一，不区分状态
     *
     * @param versionName  版本号
     * @param permissionId 权限ID（可为空）
     * @param excludeId    排除的版本ID（编辑时排除自身，可为空）
     * @return 匹配记录数
     */
    int countDuplicate(@Param("versionName") String versionName,
                       @Param("permissionId") Long permissionId,
                       @Param("excludeId") Long excludeId);
}
