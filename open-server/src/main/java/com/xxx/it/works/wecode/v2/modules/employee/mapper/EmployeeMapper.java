package com.xxx.it.works.wecode.v2.modules.employee.mapper;

import com.xxx.it.works.wecode.v2.modules.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 人员信息 Mapper
 *
 * @author SDDU Build Agent
 */
@Mapper
public interface EmployeeMapper {

    /**
     * 根据 welinkId 列表批量查询人员信息
     */
    List<Employee> selectByWelinkIds(@Param("welinkIds") List<String> welinkIds);

    /**
     * 根据 welinkId 查询单个人员
     */
    Employee selectByWelinkId(@Param("welinkId") String welinkId);

    /**
     * 按关键字模糊搜索（匹配 w3Account/chineseName/englishName）
     */
    List<Employee> searchByKeyword(@Param("keyword") String keyword);
}
