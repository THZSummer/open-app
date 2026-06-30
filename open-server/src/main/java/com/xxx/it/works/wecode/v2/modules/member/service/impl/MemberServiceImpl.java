package com.xxx.it.works.wecode.v2.modules.member.service.impl;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.app.service.AppCommonService;
import com.xxx.it.works.wecode.v2.modules.employee.entity.Employee;
import com.xxx.it.works.wecode.v2.modules.employee.mapper.EmployeeMapper;
import com.xxx.it.works.wecode.v2.modules.member.dto.AddMemberRequest;
import com.xxx.it.works.wecode.v2.modules.member.dto.TransferOwnerRequest;
import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import com.xxx.it.works.wecode.v2.modules.member.service.MemberService;
import com.xxx.it.works.wecode.v2.modules.member.util.MemberUtils;
import com.xxx.it.works.wecode.v2.modules.member.vo.MemberVO;
import com.xxx.it.works.wecode.v2.modules.member.vo.UserDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 成员服务实现
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@Service
public class MemberServiceImpl implements MemberService {

    @Autowired
    private AppMemberMapper appMemberMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private AppContextResolver appContextResolver;

    @Autowired
    private AppCommonService appCommonService;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Override
    public ApiResponse<List<MemberVO>> getMemberList(String appId, Integer curPage, Integer pageSize) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        int offset = (curPage - 1) * pageSize;
        List<AppMember> members = appMemberMapper.selectByAppId(internalAppId, offset, pageSize);
        long total = appMemberMapper.countByAppId(internalAppId);

        List<MemberVO> list = members.stream().map(m -> {
            MemberVO vo = new MemberVO();
            vo.setId(String.valueOf(m.getId()));
            vo.setAccountId(m.getAccountId());
            vo.setMemberNameCn(m.getMemberNameCn());
            vo.setMemberNameEn(m.getMemberNameEn());
            vo.setMemberType(m.getMemberType());
            vo.setCreatedAt(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());

        fillW3Accounts(members, list);

        return ApiResponse.success(list, ApiResponse.buildPage(curPage, pageSize, total));
    }

    /**
     * 批量填充成员的 w3 账号
     *
     * @param members 成员记录列表
     * @param vos     对应的 VO 列表（按相同顺序）
     */
    private void fillW3Accounts(List<AppMember> members, List<MemberVO> vos) {
        List<String> accountIds = members.stream()
                .map(AppMember::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (accountIds.isEmpty()) {
            return;
        }
        List<Employee> employees = employeeMapper.selectByWelinkIds(accountIds);
        Map<String, String> w3Map = employees.stream()
                .collect(Collectors.toMap(Employee::getWelinkId, Employee::getW3Account, (a, b) -> a));
        for (MemberVO vo : vos) {
            vo.setW3Account(w3Map.getOrDefault(vo.getAccountId(), ""));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMembers(String appId, AddMemberRequest request) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();

        // 校验操作人角色权限
        AppMember operator = getOperator(internalAppId);
        if (Objects.isNull(operator)) {
            log.warn("Failed to add member: no permission, appId={}, role={}", appId, request.getRole());
            throw BusinessException.of(ResponseCodeEnum.NO_MEMBER_OPERATION_PERMISSION);
        }
        validateMemberOperationPermission(operator.getMemberType(), request.getRole());

        log.info("Adding members: appId={}, operator={}, targetRole={}, targetAccounts={}", appId, operator.getAccountId(), request.getRole(), request.getAccountIds());

        // 批量校验角色重复
        List<AppMember> existingMembers = appMemberMapper.selectByAppIdAndAccountIdsAndType(
                internalAppId, request.getAccountIds(), request.getRole());
        if (!existingMembers.isEmpty()) {
            String duplicateAccountId = existingMembers.get(0).getAccountId();
            throw new BusinessException(
                    ResponseCodeEnum.MEMBER_ALREADY_HAS_ROLE.getCode(),
                    duplicateAccountId + " " + ResponseCodeEnum.MEMBER_ALREADY_HAS_ROLE.getMessageZh(),
                    duplicateAccountId + " " + ResponseCodeEnum.MEMBER_ALREADY_HAS_ROLE.getMessageEn());
        }

        // 遍历校验 + 组装成员列表
        String currentAccountId = UserContextHolder.getUserId();
        List<AppMember> members = new ArrayList<>();
        for (String accountId : request.getAccountIds()) {
            if (!StringUtils.hasText(accountId)) {
                throw BusinessException.of(ResponseCodeEnum.MEMBER_ACCOUNT_INVALID);
            }
            members.add(buildMember(internalAppId, accountId, request.getRole(), currentAccountId));
        }

        // 批量插入成员表
        appMemberMapper.insertBatch(members);

        log.info("Members added successfully: appId={}, count={}", appId, request.getAccountIds().size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMember(String appId, String id) {
        AppContext delCtx = appContextResolver.resolveAndValidate(appId);
        Long delInternalAppId = delCtx.getInternalId();

        // 校验操作人权限
        AppMember operator = getOperator(delInternalAppId);
        if (Objects.isNull(operator)) {
            throw BusinessException.of(ResponseCodeEnum.NO_MEMBER_OPERATION_PERMISSION);
        }

        // 查询目标成员记录
        Long memberId = Long.parseLong(id);
        AppMember member = appMemberMapper.selectById(memberId);
        if (Objects.isNull(member)) {
            throw BusinessException.of(ResponseCodeEnum.MEMBER_NOT_FOUND);
        }

        validateMemberOperationPermission(operator.getMemberType(), member.getMemberType());

        // 按主键删除单条记录
        appMemberMapper.deleteById(memberId);
        log.info("Member deleted successfully: appId={}, memberId={}, memberType={}", appId, memberId, member.getMemberType());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(String appId, TransferOwnerRequest request) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        Long internalAppId = ctx.getInternalId();
        String fromAccountId = UserContextHolder.getUserId();
        String toAccountId = request.getToAccountId();

        // 校验操作人是 Owner
        AppMember fromOwnerRecord = appMemberMapper.selectByAppIdAccountIdAndType(
                internalAppId, fromAccountId, MemberTypeEnum.OWNER.getCode());
        if (Objects.isNull(fromOwnerRecord)) {
            throw BusinessException.of(ResponseCodeEnum.NO_TRANSFER_PERMISSION);
        }

        // 新增目标用户的 Owner 记录（若已有其他角色记录，保留不动）
        appMemberMapper.insert(buildMember(internalAppId, toAccountId, MemberTypeEnum.OWNER.getCode(), fromAccountId));

        // 删除原操作人的 Owner 记录（其他角色记录保留）
        appMemberMapper.deleteById(fromOwnerRecord.getId());

        // 通知卡片服务
        appCommonService.notifyCardService(appId, CommonConstants.EVENT_TRANSFER_OWNER);
        log.info("Owner transferred successfully: appId={}, from={}, to={}", appId, fromAccountId, toAccountId);
    }

    @Override
    public ApiResponse<List<UserDataVO>> searchUsers(String appId, String keyword) {
        appContextResolver.resolveAndValidate(appId);

        String kw = StringUtils.hasText(keyword) ? keyword : "";
        List<Employee> employees = employeeMapper.searchByKeyword(kw);

        List<UserDataVO> result = new ArrayList<>();
        for (Employee emp : employees) {
            UserDataVO vo = new UserDataVO();
            vo.setWelinkId(emp.getWelinkId());
            vo.setMemberNameCn(emp.getChineseName());
            vo.setMemberNameEn(emp.getEnglishName());
            vo.setDeptName(emp.getDepartment());
            vo.setW3Account(emp.getW3Account());
            result.add(vo);
        }

        return ApiResponse.success(result, ApiResponse.buildPage(1, result.size(), result.size()));
    }

    /**
     * 校验操作人对目标角色的操作权限（添加/删除共用）
     *
     * <p>权限矩阵：</p>
     * <pre>
     *   操作人角色    可操作的目标角色              越权错误码
     *   ─────────────────────────────────────────────────────
     *   Developer(0)  无（不能操作任何成员）        ADD→403200 / DELETE→403202
     *   Admin(2)      Developer(0) 仅              ADD→403201 / DELETE→403203
     *   Owner(1)      Developer(0), Admin(2)       Owner→409204(ADD) / 409201(DELETE)
     * </pre>
     *
     * @param operatorRole 操作人角色 member_type
     * @param targetRole   目标角色 member_type
     */
    private void validateMemberOperationPermission(int operatorRole, int targetRole) {

        // Developer：无权任何成员操作
        if (operatorRole == MemberTypeEnum.DEVELOPER.getCode()) {
            throw BusinessException.of(ResponseCodeEnum.NO_MEMBER_OPERATION_PERMISSION);
        }

        // Admin：只能操作 Developer
        if (operatorRole == MemberTypeEnum.ADMIN.getCode()
                && targetRole != MemberTypeEnum.DEVELOPER.getCode()) {
            throw BusinessException.of(ResponseCodeEnum.NO_MEMBER_OPERATION_PERMISSION);
        }

        // 目标是 Owner：Owner 不能直接添加或删除，只能通过转移流程
        if (targetRole == MemberTypeEnum.OWNER.getCode()) {
            throw BusinessException.of(ResponseCodeEnum.CANNOT_OPERATE_OWNER);
        }
    }

    /**
     * 获取当前登录用户在该应用中的最高角色成员记录
     *
     * @param internalAppId 应用内部 ID
     * @return 最高角色记录，非成员返回 null
     */
    private AppMember getOperator(Long internalAppId) {
        String currentAccountId = UserContextHolder.getUserId();
        List<AppMember> records = appMemberMapper.selectByAppIdAndAccountId(internalAppId, currentAccountId);
        return MemberUtils.getHighestRoleMember(records);
    }

    /**
     * 构建成员记录（添加成员 / 转移 Owner 复用）
     */
    private AppMember buildMember(Long internalAppId, String accountId, Integer memberType, String operatorAccountId) {
        Employee emp = employeeMapper.selectByWelinkId(accountId);
        if (ObjectUtils.isEmpty(emp) || !StringUtils.hasText(emp.getChineseName())) {
            throw BusinessException.of(ResponseCodeEnum.MEMBER_ACCOUNT_INVALID);
        }
        AppMember member = new AppMember();
        member.setId(idGenerator.nextId());
        member.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        member.setAppId(internalAppId);
        member.setAccountId(accountId);
        member.setMemberNameCn(emp.getChineseName());
        member.setMemberNameEn(emp.getEnglishName());
        member.setMemberType(memberType);
        member.setStatus(StatusEnum.ENABLED.getCode());
        member.setCreateBy(operatorAccountId);
        member.setLastUpdateBy(operatorAccountId);
        return member;
    }
}
