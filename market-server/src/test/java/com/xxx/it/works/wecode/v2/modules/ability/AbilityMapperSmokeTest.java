package com.xxx.it.works.wecode.v2.modules.ability;

import com.xxx.it.works.wecode.v2.MarketServerApplication;
import com.xxx.it.works.wecode.v2.modules.ability.entity.AbilityEntity;
import com.xxx.it.works.wecode.v2.modules.ability.mapper.AbilityMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AbilityMapper 重构冒烟测试
 *
 * <p>验证模块重组后 AbilityMapper 可正常注入、DB 查询功能正常。
 * 使用 @ActiveProfiles("dev") 复用 dev 环境 DataSource + DevMyBatisConfig，
 * 避免因 test profile 下 DevMyBatisConfig 不激活导致 MyBatis 不可用。</p>
 */
@SpringBootTest(classes = MarketServerApplication.class)
@ActiveProfiles("dev")
class AbilityMapperSmokeTest {

    @Autowired
    private AbilityMapper abilityMapper;

    /**
     * 验证 AbilityMapper Bean 注入成功
     */
    @Test
    void shouldInjectAbilityMapper() {
        assertNotNull(abilityMapper, "AbilityMapper should be injected");
    }

    /**
     * 验证 selectByIds 对不存在的 ID 返回空列表而非异常
     */
    @Test
    void shouldSelectByIdsWithNonExistentIds() {
        // 使用一个极大值 ID，确保 DB 中不存在
        List<AbilityEntity> result = abilityMapper.selectByIds(Arrays.asList(99999999L));
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Non-existent ids should return empty result");
    }

    /**
     * 验证 selectByIds 对存在的数据可正常查询
     *
     * <p>注意：MyBatis <foreach> 不支持空集合（会产生 IN () 语法错误），
     * 但业务调用方在调用前已判空，此限制不影响功能。</p>
     */
    @Test
    void shouldSelectByIdsWithExistingIds() {
        // 查询 ID=1 的能力（需要 DB 中有此数据，测试环境通常有初始数据）
        List<AbilityEntity> result = abilityMapper.selectByIds(Arrays.asList(1L));
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "查询应返回已有能力数据");
        // 如果 DB 中没有 ID=1 的数据，返回空列表也是正常行为
        // 重点是查询不抛异常，Mapper 映射正常
    }
}
