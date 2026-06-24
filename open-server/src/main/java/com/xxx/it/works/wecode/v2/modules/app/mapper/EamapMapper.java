package com.xxx.it.works.wecode.v2.modules.app.mapper;

import com.xxx.it.works.wecode.v2.modules.app.entity.Eamap;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * EAMAP Mapper
 */
@Mapper
public interface EamapMapper {

    List<Eamap> selectAll();

    /** 分页查询 */
    List<Eamap> selectList(int offset, int pageSize);

    /** 总数 */
    long count();

    Eamap selectByEamapAppCode(String eamapAppCode);
}
