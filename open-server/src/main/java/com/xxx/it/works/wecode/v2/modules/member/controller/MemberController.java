package com.xxx.it.works.wecode.v2.modules.member.controller;

import com.xxx.it.works.wecode.v2.common.annotation.AuditLog;
import com.xxx.it.works.wecode.v2.common.enums.OperateEnum;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.member.dto.AddMemberRequest;
import com.xxx.it.works.wecode.v2.modules.member.dto.TransferOwnerRequest;
import com.xxx.it.works.wecode.v2.modules.member.service.MemberService;
import com.xxx.it.works.wecode.v2.modules.member.vo.MemberVO;
import com.xxx.it.works.wecode.v2.modules.member.vo.UserDataVO;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 成员管理控制器
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/service/open/v2/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    /**
     * 2.1 获取应用成员列表
     */
    @GetMapping("/list")
    public ApiResponse<List<MemberVO>> getMemberList(
            @RequestParam @NotBlank String appId,
            @RequestParam(defaultValue = "1") Integer curPage,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return memberService.getMemberList(appId, curPage, pageSize);
    }

    /**
     * 2.2 添加成员
     */
    @AuditLog(value = OperateEnum.ADD_APP_MEMBER)
    @PostMapping("")
    public ApiResponse<Void> addMembers(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated AddMemberRequest request) {
        memberService.addMembers(appId, request);
        return ApiResponse.success();
    }

    /**
     * 2.3 删除成员
     */
    @AuditLog(value = OperateEnum.DELETE_APP_MEMBER)
    @DeleteMapping("")
    public ApiResponse<Void> deleteMember(
            @RequestParam @NotBlank String appId,
            @RequestParam @NotBlank String id) {
        memberService.deleteMember(appId, id);
        return ApiResponse.success();
    }

    /**
     * 2.4 转移 Owner
     */
    @AuditLog(value = OperateEnum.TRANSFER_APP_OWNER)
    @PostMapping("/transfer-owner")
    public ApiResponse<Void> transferOwner(
            @RequestParam @NotBlank String appId,
            @RequestBody @Validated TransferOwnerRequest request) {
        memberService.transferOwner(appId, request);
        return ApiResponse.success();
    }

    /**
     * 2.5 搜索可添加的用户
     */
    @GetMapping("/search-users")
    public ApiResponse<List<UserDataVO>> searchUsers(
            @RequestParam @NotBlank String appId,
            @RequestParam String keyword) {
        return memberService.searchUsers(appId, keyword);
    }
}
