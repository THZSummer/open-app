package com.xxx.it.works.wecode.v2.modules.member.service;

import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.member.dto.AddMemberRequest;
import com.xxx.it.works.wecode.v2.modules.member.dto.TransferOwnerRequest;
import com.xxx.it.works.wecode.v2.modules.member.vo.MemberVO;
import com.xxx.it.works.wecode.v2.modules.member.vo.UserDataVO;

import java.util.List;

/**
 * 成员服务接口
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
public interface MemberService {

    ApiResponse<List<MemberVO>> getMemberList(String appId, Integer curPage, Integer pageSize);

    void addMembers(String appId, AddMemberRequest request);

    void deleteMember(String appId, String id);

    void transferOwner(String appId, TransferOwnerRequest request);

    ApiResponse<List<UserDataVO>> searchUsers(String appId, String keyword);
}
