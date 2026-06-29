package com.xxx.it.works.wecode.v2.modules.app.mapper;

import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用 Mapper
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Mapper
public interface AppMapper {

    App selectByAppId(@Param("appId") String appId);

    App selectById(@Param("id") Long id);

    int insert(App app);

    int update(App app);

    int countByNameCn(@Param("nameCn") String nameCn);

    int countByNameEn(@Param("nameEn") String nameEn);

    List<App> selectListByAccountId(
            @Param("accountId") String accountId,
            @Param("tenantId") String tenantId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize);

    long countByAccountId(
            @Param("accountId") String accountId,
            @Param("tenantId") String tenantId);

    List<AppProperty> selectPropertiesByParentId(@Param("parentId") Long parentId);

    /**
     * 批量查询多个父级的属性
     */
    List<AppProperty> selectPropertiesByParentIds(@Param("parentIds") List<Long> parentIds);

    int insertProperty(AppProperty property);

    int batchInsertProperties(@Param("list") List<AppProperty> list);

    int deletePropertyByName(
            @Param("parentId") Long parentId,
            @Param("propertyName") String propertyName);

    int countByPropertyNameAndValue(
            @Param("propertyName") String propertyName,
            @Param("propertyValue") String propertyValue);

    /**
     * 查询数据字典（openplatform_property_t）按 path + code
     */
    String selectDictionaryValue(@Param("path") String path, @Param("code") String code);
}
