package com.xxx.it.works.wecode.v2.modules.flow.mapper;

import com.xxx.it.works.wecode.v2.modules.flow.entity.Flow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 连接流 Mapper 接口
 * <p>
 * 连接流基本信息 CRUD + 启停
 * </p>
 */
@Mapper
public interface OpFlowMapper {

    /**
     * 插入连接流
     */
    int insert(Flow flow);

    /**
     * 根据 ID 查询连接流
     */
    Flow selectById(@Param("id") Long id);

    /**
     * 更新连接流基本信息
     */
    int update(Flow flow);

    /**
     * 删除连接流
     */
    int deleteById(@Param("id") Long id);

    /**
     * 查询全部连接流列表（无分页）
     */
    List<Flow> selectAll(
            @Param("lifecycleStatus") Integer lifecycleStatus,
            @Param("keyword") String keyword,
            @Param("appId") Long appId
    );

    /**
     * 分页查询连接流列表
     */
    List<Flow> selectList(
            @Param("lifecycleStatus") Integer lifecycleStatus,
            @Param("keyword") String keyword,
            @Param("appId") Long appId,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    /**
     * 统计连接流总数
     */
    Long countList(
            @Param("lifecycleStatus") Integer lifecycleStatus,
            @Param("keyword") String keyword,
            @Param("appId") Long appId
    );

    /**
     * 更新连接流生命周期状态
     */
    int updateLifecycleStatus(
            @Param("id") Long id,
            @Param("lifecycleStatus") Integer lifecycleStatus,
            @Param("lastUpdateTime") java.util.Date lastUpdateTime,
            @Param("lastUpdateBy") String lastUpdateBy
    );

    /**
     * 部署连接流（绑定版本）
     */
    int deploy(
            @Param("id") Long id,
            @Param("deployedVersionId") Long deployedVersionId,
            @Param("deployedVersionNumber") Integer deployedVersionNumber,
            @Param("lastUpdateTime") java.util.Date lastUpdateTime,
            @Param("lastUpdateBy") String lastUpdateBy
    );
}