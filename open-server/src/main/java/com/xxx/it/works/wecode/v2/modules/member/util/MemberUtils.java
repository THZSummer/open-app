package com.xxx.it.works.wecode.v2.modules.member.util;

import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;

/**
 * 成员相关工具类
 *
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public final class MemberUtils {

    private MemberUtils() {
    }

    /**
     * 从多条角色记录中取最高权限的记录
     * 优先级由 {@link MemberTypeEnum#getPriority()} 定义
     */
    public static AppMember getHighestRoleMember(List<AppMember> records) {
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }
        return records.stream()
                .max(Comparator.comparingInt(m -> {
                    MemberTypeEnum type = MemberTypeEnum.fromCode(m.getMemberType());
                    return type != null ? type.getPriority() : 0;
                }))
                .orElse(null);
    }
}
