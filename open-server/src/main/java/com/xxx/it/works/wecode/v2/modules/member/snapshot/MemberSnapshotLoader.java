package com.xxx.it.works.wecode.v2.modules.member.snapshot;

import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 成员快照加载器（审计日志）
 * 对应 TASK-016
 */
@Component
public class MemberSnapshotLoader implements EntitySnapshotLoader {

    @Autowired
    private AppMemberMapper memberMapper;

    @Override
    public List<String> supportedObjects() {
        return List.of("APP_MEMBER");
    }

    @Override
    public Object loadById(Long id) {
        return memberMapper.selectById(id);
    }
}
