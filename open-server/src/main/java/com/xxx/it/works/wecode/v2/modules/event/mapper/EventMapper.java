package com.xxx.it.works.wecode.v2.modules.event.mapper;

import com.xxx.it.works.wecode.v2.modules.event.entity.Event;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 事件 Mapper
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Mapper
public interface EventMapper {

    /**
     * 插入事件
     */
    int insert(Event event);

    /**
     * 根据ID查询事件
     */
    Event selectById(@Param("id") Long id);

    /**
     * 根据ID查询事件（包含分类名称）
     */
    Event selectByIdWithCategoryName(@Param("id") Long id);

    /**
     * 分页查询事件列表
     * 
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）
     * @param keyword 搜索关键词（可选）
     * @param offset 偏移量
     * @param pageSize 每页大小
     * @return 事件列表
     */
    List<Event> selectList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize);

    /**
     * 统计事件数量
     */
    long countList(
            @Param("categoryId") Long categoryId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);

    /**
     * 根据 Topic 查询事件
     */
    Event selectByTopic(@Param("topic") String topic);

    /**
     * 更新事件
     */
    int update(Event event);

    /**
     * 删除事件
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计事件被订阅的数量
     */
    int countSubscriptionsByEventId(@Param("eventId") Long eventId);
}
