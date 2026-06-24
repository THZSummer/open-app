package com.xxx.it.works.wecode.v2.modules.member.snapshot;

import com.xxx.it.works.wecode.v2.common.util.CommonUtils;
import com.xxx.it.works.wecode.v2.common.snapshot.EntitySnapshotLoader;
import com.xxx.it.works.wecode.v2.modules.member.dto.AddMemberRequest;
import com.xxx.it.works.wecode.v2.modules.member.dto.TransferOwnerRequest;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 成员快照加载器
 *
 * <p>标准操作（DELETE）走 loadById 查 DB。
 * ADD_APP_MEMBER（批量操作）和 TRANSFER_APP_OWNER（多实体变更）无 resourceId，
 * override loadAfterData 从方法参数提取。</p>
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
    public Object loadAfterData(ProceedingJoinPoint joinPoint, Long resourceId, Object result) {
        // 先尝试从方法参数提取（ADD_APP_MEMBER / TRANSFER_APP_OWNER）
        AddMemberRequest addReq = CommonUtils.findArg(joinPoint, AddMemberRequest.class);
        if (addReq != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("memberType", addReq.getRole());
            MemberTypeEnum memberType = addReq.getRole() != null
                    ? MemberTypeEnum.fromCode(addReq.getRole()) : null;
            data.put("memberTypeDesc", memberType != null ? memberType.getDesc() : String.valueOf(addReq.getRole()));
            data.put("accountId", String.join(",", addReq.getAccountIds()));
            return data;
        }

        TransferOwnerRequest transferReq = CommonUtils.findArg(joinPoint, TransferOwnerRequest.class);
        if (transferReq != null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("accountId", transferReq.getToAccountId());
            return data;
        }

        // 标准路径：从 DB 查
        if (resourceId != null) {
            return loadById(resourceId);
        }
        return null;
    }

    @Override
    public Object loadById(Long id) {
        return memberMapper.selectById(id);
    }
}
