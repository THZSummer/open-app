package com.xxx.it.works.wecode.v2.modules.app.service.impl;

import com.xxx.it.works.wecode.v2.common.constants.CommonConstants;
import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.enums.ResponseCodeEnum;
import com.xxx.it.works.wecode.v2.common.enums.StatusEnum;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.file.entity.FileEntity;
import com.xxx.it.works.wecode.v2.common.file.mapper.FileMapper;
import com.xxx.it.works.wecode.v2.common.file.service.FileV2Service;
import com.xxx.it.works.wecode.v2.common.file.vo.FileV2VO;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.app.constants.AppPropertyConstants;
import com.xxx.it.works.wecode.v2.modules.app.dto.BindEamapRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.CreateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateAppRequest;
import com.xxx.it.works.wecode.v2.modules.app.dto.UpdateVerifyTypeRequest;
import com.xxx.it.works.wecode.v2.modules.app.entity.App;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppIdentity;
import com.xxx.it.works.wecode.v2.modules.app.entity.AppProperty;
import com.xxx.it.works.wecode.v2.modules.app.entity.Eamap;
import com.xxx.it.works.wecode.v2.modules.app.enums.AppSubTypeEnum;
import com.xxx.it.works.wecode.v2.modules.app.enums.AppTypeEnum;
import com.xxx.it.works.wecode.v2.modules.app.enums.VerifyTypeEnum;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppIdentityMapper;
import com.xxx.it.works.wecode.v2.modules.app.mapper.AppMapper;
import com.xxx.it.works.wecode.v2.modules.app.mapper.EamapMapper;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContext;
import com.xxx.it.works.wecode.v2.modules.app.resolver.AppContextResolver;
import com.xxx.it.works.wecode.v2.modules.app.service.AppCommonService;
import com.xxx.it.works.wecode.v2.modules.app.service.AppService;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppBasicInfoVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppIdentityVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppListItemVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.AppVerifyTypeVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.EamapVO;
import com.xxx.it.works.wecode.v2.modules.app.vo.EmployeeInfoVO;
import com.xxx.it.works.wecode.v2.modules.employee.entity.Employee;
import com.xxx.it.works.wecode.v2.modules.employee.mapper.EmployeeMapper;
import com.xxx.it.works.wecode.v2.modules.lookup.mapper.LookupWhitelistMapper;
import com.xxx.it.works.wecode.v2.modules.member.entity.AppMember;
import com.xxx.it.works.wecode.v2.modules.member.enums.MemberTypeEnum;
import com.xxx.it.works.wecode.v2.modules.member.mapper.AppMemberMapper;
import com.xxx.it.works.wecode.v2.modules.member.util.MemberUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 应用服务实现
 *
 * @author SDDU Build Agent
 * @date 2026-06-06
 */
@Slf4j
@Service
public class AppServiceImpl implements AppService {

    /**
     * API 密钥格式：16 位字母+数字混合
     */
    private static final Pattern API_SECRET_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{16}$");

    @Autowired
    private AppMapper appMapper;

    @Autowired
    private AppMemberMapper appMemberMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private AppContextResolver appContextResolver;

    @Autowired
    private AppCommonService appCommonService;

    @Autowired
    private FileV2Service fileV2Service;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Autowired
    private EamapMapper eamapMapper;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private LookupWhitelistMapper lookupWhitelistMapper;

    @Autowired
    private AppIdentityMapper appIdentityMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveApp(CreateAppRequest request) {
        // 1. 校验
        validateAppName(request.getNameCn(), request.getNameEn(), null, null);
        validateEamapBinding(request.getEamapAppCode());

        String currentUserId = UserContextHolder.getUserId();
        String appId = generateAppId();

        // 2. 插入应用主表
        App app = buildAppEntity(appId, request, currentUserId);
        appMapper.insert(app);

        // 3. 插入属性表（EAMAP + verifyType）
        saveAppProperties(app.getId(), request.getEamapAppCode());

        // 4. 插入成员表（创建者成为 Owner）
        saveOwnerMember(app.getId(), currentUserId);

        // 5. 保存凭证
        saveAppIdentity(app.getId(), appId, currentUserId);

        // 6. 通知卡片服务
        appCommonService.notifyCardService(appId, CommonConstants.EVENT_CREATE);

        log.info("App created successfully: appId={}", appId);
        return appId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateApp(String appId, UpdateAppRequest request) {
        // 权限校验 + 获取应用
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        // 校验名称唯一性（名称未变则跳过）
        validateAppName(request.getNameCn(), request.getNameEn(), app.getAppNameCn(), app.getAppNameEn());

        // 更新主表
        app.setIconId(request.getIconId());
        app.setAppNameCn(request.getNameCn());
        app.setAppNameEn(request.getNameEn());
        app.setAppDescCn(request.getDescCn());
        app.setAppDescEn(request.getDescEn());
        appMapper.update(app);

        // 更新示意图属性（先删后增）
        appMapper.deletePropertyByName(app.getId(), AppPropertyConstants.PROP_DIAGRAM_ID_LIST);
        if (!CollectionUtils.isEmpty(request.getDiagramIdList())) {
            appMapper.insertProperty(createProperty(app.getId(), AppPropertyConstants.PROP_DIAGRAM_ID_LIST, String.join(",", request.getDiagramIdList())));
        }

        appCommonService.notifyCardService(appId, CommonConstants.EVENT_UPDATE);
    }

    @Override
    public AppBasicInfoVO getAppBasicInfo(String appId) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        AppBasicInfoVO vo = new AppBasicInfoVO();
        vo.setAppId(app.getAppId());
        vo.setNameCn(app.getAppNameCn());
        vo.setNameEn(app.getAppNameEn());
        vo.setDescCn(app.getAppDescCn());
        vo.setDescEn(app.getAppDescEn());
        vo.setAppType(app.getAppType());
        vo.setAppSubType(app.getAppSubType());
        vo.setStatus(app.getStatus());

        // 图标
        vo.setIcon(fileV2Service.buildFileVO(app.getIconId()));

        // 属性
        List<AppProperty> props = appMapper.selectPropertiesByParentId(app.getId());
        for (AppProperty p : props) {
            if (AppPropertyConstants.PROP_EAMAP_CODE.equals(p.getPropertyName())) {
                vo.setEamapAppCode(p.getPropertyValue());
                // 从 EAMAP 表查名称
                if (StringUtils.hasText(p.getPropertyValue())) {
                    Eamap eamap = eamapMapper.selectByEamapAppCode(p.getPropertyValue());
                    if (Objects.nonNull(eamap)) {
                        vo.setEamapAppName(eamap.getNameCn());
                    }
                }
            }
            if (AppPropertyConstants.PROP_DIAGRAM_ID_LIST.equals(p.getPropertyName())) {
                List<FileV2VO> diagrams = Arrays.stream(p.getPropertyValue().split(","))
                        .filter(id -> !id.isEmpty())
                        .map(fileV2Service::buildFileVO)
                        .collect(Collectors.toList());
                vo.setDiagramIdList(diagrams);
            }
        }

        return vo;
    }

    @Override
    public ApiResponse<List<AppListItemVO>> getAppList(Integer curPage, Integer pageSize) {
        // 从请求上下文获取当前用户 accountId 和 tenantId
        String accountId = UserContextHolder.getUserId();
        String tenantId = CommonConstants.DEFAULT_TENANT_ID;

        int offset = (curPage - 1) * pageSize;
        List<App> apps = appMapper.selectListByAccountId(accountId, tenantId, offset, pageSize);
        long total = appMapper.countByAccountId(accountId, tenantId);

        List<Long> appIds = apps.stream().map(App::getId).collect(Collectors.toList());
        Map<Long, EmployeeInfoVO> ownerMap = getOwnerMap(appIds);

        List<AppListItemVO> list = apps.stream().map(app -> {
            AppListItemVO vo = new AppListItemVO();
            vo.setAppId(app.getAppId());
            vo.setNameCn(app.getAppNameCn());
            vo.setNameEn(app.getAppNameEn());

            vo.setIcon(fileV2Service.buildFileVO(app.getIconId()));

            vo.setAppType(app.getAppType());
            vo.setAppSubType(app.getAppSubType());
            vo.setStatus(app.getStatus());

            // EAMAP 绑定状态
            List<AppProperty> props = appMapper.selectPropertiesByParentId(app.getId());
            boolean eamapBound = props.stream()
                    .anyMatch(p -> AppPropertyConstants.PROP_EAMAP_CODE.equals(p.getPropertyName())
                            && StringUtils.hasText(p.getPropertyValue()));
            vo.setEamapBound(eamapBound);

            // Owner 信息（从批量查询结果取）
            EmployeeInfoVO owner = ownerMap.get(app.getId());
            vo.setOwner(owner != null ? owner : new EmployeeInfoVO());

            // 当前用户角色（多条记录取最高权限）
            List<AppMember> currentMemberRecords = appMemberMapper.selectByAppIdAndAccountId(app.getId(), accountId);
            AppMember currentMember = MemberUtils.getHighestRoleMember(currentMemberRecords);
            vo.setCurrentUserRole(Objects.nonNull(currentMember) ? currentMember.getMemberType() : null);

            vo.setLastUpdateTime(app.getLastUpdateTime().toString());
            return vo;
        }).collect(Collectors.toList());

        return ApiResponse.success(list, ApiResponse.buildPage(curPage, pageSize, total));
    }

    /**
     * 批量查询应用的 Owner 信息
     * <p>一次查 member 表 + 一次查 employee 表，替代逐个应用 N*2 次查询</p>
     *
     * @param appIds 应用内部 ID 列表
     * @return appId -> EmployeeInfoVO 映射（没有 owner 的 appId 不在 map 中）
     */
    private Map<Long, EmployeeInfoVO> getOwnerMap(List<Long> appIds) {
        if (CollectionUtils.isEmpty(appIds)) {
            return Collections.emptyMap();
        }

        // 1. 批量查 owner member 记录
        List<AppMember> ownerMembers = appMemberMapper.selectByAppIdsAndMemberType(
                appIds, MemberTypeEnum.OWNER.getCode());

        if (CollectionUtils.isEmpty(ownerMembers)) {
            return Collections.emptyMap();
        }

        // 2. 批量查 employee w3Account
        List<String> welinkIds = ownerMembers.stream()
                .map(AppMember::getAccountId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Employee> empMap = CollectionUtils.isEmpty(welinkIds)
                ? Collections.emptyMap()
                : employeeMapper.selectByWelinkIds(welinkIds).stream()
                .collect(Collectors.toMap(Employee::getWelinkId, e -> e, (a, b) -> a));

        // 3. 组装 appId -> EmployeeInfoVO
        return ownerMembers.stream().collect(Collectors.toMap(
                AppMember::getAppId,
                m -> {
                    EmployeeInfoVO vo = new EmployeeInfoVO();
                    vo.setWelinkId(m.getAccountId());
                    vo.setMemberNameCn(m.getMemberNameCn());
                    vo.setMemberNameEn(m.getMemberNameEn());
                    Employee emp = empMap.get(m.getAccountId());
                    if (Objects.nonNull(emp)) {
                        vo.setW3Account(emp.getW3Account());
                    }
                    return vo;
                },
                (a, b) -> a
        ));
    }

    @Override
    public ApiResponse<List<EamapVO>> getEamapList(Integer curPage, Integer pageSize) {
        int offset = (curPage - 1) * pageSize;
        List<Eamap> eamaps = eamapMapper.selectList(offset, pageSize);
        long total = eamapMapper.count();
        List<EamapVO> list = eamaps.stream()
                .map(e -> new EamapVO(e.getEamapAppCode(), e.getNameCn()))
                .collect(Collectors.toList());
        return ApiResponse.success(list, ApiResponse.buildPage(curPage, pageSize, total));
    }

    @Override
    public List<FileV2VO> getDefaultIcons() {
        List<String> iconIds = lookupWhitelistMapper.selectItemValuesByClassifyCode("app.default.icon");
        List<FileV2VO> list = new ArrayList<>();
        for (String fileId : iconIds) {
            FileEntity file = fileMapper.selectByFileId(fileId);
            if (Objects.nonNull(file)) {
                // 用 fileV2Service 构建 VO，确保 url 前缀与当前配置一致
                list.add(fileV2Service.buildFileVO(fileId));
            }
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVerifyType(String appId, UpdateVerifyTypeRequest request) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        validateVerifyType(request);

        // 更新 verify_type
        String verifyTypeStr = request.getVerifyType().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        appMapper.deletePropertyByName(app.getId(), AppPropertyConstants.PROP_VERIFY_TYPE);
        appMapper.insertProperty(createProperty(app.getId(), AppPropertyConstants.PROP_VERIFY_TYPE, verifyTypeStr));

        // 更新 api_secret
        appMapper.deletePropertyByName(app.getId(), AppPropertyConstants.PROP_API_SECRET);
        if (request.getVerifyType().contains(VerifyTypeEnum.DIGITAL_SIGNATURE.getCode()) && Objects.nonNull(request.getApiSecret())) {
            appMapper.insertProperty(createProperty(app.getId(), AppPropertyConstants.PROP_API_SECRET, encryptApiSecret(request.getApiSecret())));
        }

        appCommonService.notifyCardService(appId, CommonConstants.EVENT_UPDATE_VERIFY_TYPE);
    }

    private void validateVerifyType(UpdateVerifyTypeRequest request) {
        // 白名单校验：数据字典控制是否允许多选
        String multiselectValue = appMapper.selectDictionaryValue(CommonConstants.DICT_CATEGORY_APP, CommonConstants.DICT_VERIFY_TYPE_MULTI_SWITCH);
        if (CommonConstants.FLAG_FALSE.equalsIgnoreCase(multiselectValue)) {
            if (request.getVerifyType().size() > 1) {
                throw new BusinessException(
                        ResponseCodeEnum.VERIFY_TYPE_SINGLE_ONLY.getCode(),
                        ResponseCodeEnum.VERIFY_TYPE_SINGLE_ONLY.getMessageZh(),
                        ResponseCodeEnum.VERIFY_TYPE_SINGLE_ONLY.getMessageEn()
                );
            }
        }

        // 校验 verifyType 合法性（枚举校验）
        for (Integer vt : request.getVerifyType()) {
            if (!VerifyTypeEnum.isValidCode(vt)) {
                throw new BusinessException(
                        ResponseCodeEnum.VERIFY_TYPE_INVALID.getCode(),
                        ResponseCodeEnum.VERIFY_TYPE_INVALID.getMessageZh(),
                        ResponseCodeEnum.VERIFY_TYPE_INVALID.getMessageEn()
                );
            }
        }

        // 校验 SOAHeader(1) 和 SOAURL(3) 互斥
        if (request.getVerifyType().contains(VerifyTypeEnum.SOA_HEADER.getCode()) && request.getVerifyType().contains(VerifyTypeEnum.SOA_URL.getCode())) {
            throw new BusinessException(
                    ResponseCodeEnum.VERIFY_TYPE_MUTUALLY_EXCLUSIVE.getCode(),
                    ResponseCodeEnum.VERIFY_TYPE_MUTUALLY_EXCLUSIVE.getMessageZh(),
                    ResponseCodeEnum.VERIFY_TYPE_MUTUALLY_EXCLUSIVE.getMessageEn()
            );
        }

        // 校验 apiSecret
        if (request.getVerifyType().contains(VerifyTypeEnum.DIGITAL_SIGNATURE.getCode())) {
            if (!StringUtils.hasText(request.getApiSecret())) {
                throw new BusinessException(
                        ResponseCodeEnum.API_SECRET_REQUIRED.getCode(),
                        ResponseCodeEnum.API_SECRET_REQUIRED.getMessageZh(),
                        ResponseCodeEnum.API_SECRET_REQUIRED.getMessageEn()
                );
            }
            if (!API_SECRET_PATTERN.matcher(request.getApiSecret()).matches()) {
                throw new BusinessException(
                        ResponseCodeEnum.API_SECRET_FORMAT_ERROR.getCode(),
                        ResponseCodeEnum.API_SECRET_FORMAT_ERROR.getMessageZh(),
                        ResponseCodeEnum.API_SECRET_FORMAT_ERROR.getMessageEn()
                );
            }
        }
    }

    @Override
    public AppIdentityVO getAppIdentity(String appId) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        // 从 identity 凭证表查询 ak/sk
        AppIdentity identity = appIdentityMapper.selectByAppId(app.getId());
        if (Objects.isNull(identity)) {
            throw new BusinessException(
                    ResponseCodeEnum.APP_IDENTITY_NOT_FOUND.getCode(),
                    ResponseCodeEnum.APP_IDENTITY_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.APP_IDENTITY_NOT_FOUND.getMessageEn()
            );
        }
        AppIdentityVO vo = new AppIdentityVO();
        vo.setAk(identity.getAk());
        vo.setSk(decryptSk(identity.getPrivateKey()));
        return vo;
    }

    @Override
    public AppVerifyTypeVO getVerifyType(String appId) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        List<AppProperty> props = appMapper.selectPropertiesByParentId(app.getId());
        String verifyTypeStr = CommonConstants.DEFAULT_VERIFY_TYPE;
        String apiSecret = null;
        for (AppProperty p : props) {
            if (AppPropertyConstants.PROP_VERIFY_TYPE.equals(p.getPropertyName())) {
                verifyTypeStr = p.getPropertyValue();
            }
            if (AppPropertyConstants.PROP_API_SECRET.equals(p.getPropertyName())) {
                apiSecret = decryptApiSecret(p.getPropertyValue());
            }
        }

        List<Integer> verifyTypes = Arrays.stream(verifyTypeStr.split(","))
                .map(Integer::parseInt)
                .collect(Collectors.toList());

        AppVerifyTypeVO vo = new AppVerifyTypeVO();
        vo.setVerifyType(verifyTypes);
        if (verifyTypes.contains(VerifyTypeEnum.DIGITAL_SIGNATURE.getCode())) {
            vo.setApiSecret(apiSecret);
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveEamapBinding(String appId, BindEamapRequest request) {
        AppContext ctx = appContextResolver.resolveAndValidate(appId);
        App app = ctx.getApp();

        // 校验是否为存量个人应用
        if (!Objects.equals(app.getAppType(), AppTypeEnum.PERSONAL.getCode()) || !Objects.equals(app.getAppSubType(), AppSubTypeEnum.LEGACY_PERSONAL.getCode())) {
            throw new BusinessException(
                    ResponseCodeEnum.APP_TYPE_NOT_SUPPORTED.getCode(),
                    ResponseCodeEnum.APP_TYPE_NOT_SUPPORTED.getMessageZh(),
                    ResponseCodeEnum.APP_TYPE_NOT_SUPPORTED.getMessageEn()
            );
        }

        // 校验 EAMAP 绑定条件
        validateEamapBinding(request.getEamapAppCode());

        // 更新应用类型为业务应用
        app.setAppType(AppTypeEnum.BUSINESS.getCode());
        app.setAppSubType(AppSubTypeEnum.BUSINESS_STANDARD.getCode());
        appMapper.update(app);

        // 更新 EAMAP 属性
        appMapper.deletePropertyByName(app.getId(), AppPropertyConstants.PROP_EAMAP_CODE);
        appMapper.insertProperty(createProperty(app.getId(), AppPropertyConstants.PROP_EAMAP_CODE, request.getEamapAppCode()));

        appCommonService.notifyCardService(appId, CommonConstants.EVENT_BIND_EAMAP);
    }

    @Override
    public Integer getCurrentRole(String appId) {
        // 从成员表查询当前用户角色
        App app = appMapper.selectByAppId(appId);
        if (Objects.isNull(app)) {
            throw new BusinessException(
                    ResponseCodeEnum.APP_NOT_FOUND.getCode(),
                    ResponseCodeEnum.APP_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.APP_NOT_FOUND.getMessageEn()
            );
        }

        // 从 UserContextHolder 获取当前用户 accountId
        String currentAccountId = UserContextHolder.getUserId();
        List<AppMember> currentMemberRecords = appMemberMapper.selectByAppIdAndAccountId(
                app.getId(), currentAccountId);
        // 多条记录取最高权限
        AppMember currentMember = MemberUtils.getHighestRoleMember(currentMemberRecords);
        return Objects.nonNull(currentMember) ? currentMember.getMemberType() : null;
    }

    /**
     * 校验应用名称唯一性
     *
     * @param nameCn      中文名称
     * @param nameEn      英文名称
     * @param originNameCn 原中文名称（更新时传当前值，名称未变则跳过；新增时传 null）
     * @param originNameEn 原英文名称（同上）
     */
    private void validateAppName(String nameCn, String nameEn, String originNameCn, String originNameEn) {
        if (!Objects.equals(nameCn, originNameCn) && appMapper.countByNameCn(nameCn) > 0) {
            throw new BusinessException(
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getCode(),
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getMessageZh(),
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getMessageEn()
            );
        }
        if (!Objects.equals(nameEn, originNameEn) && appMapper.countByNameEn(nameEn) > 0) {
            throw new BusinessException(
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getCode(),
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getMessageZh(),
                    ResponseCodeEnum.APP_NAME_DUPLICATED.getMessageEn()
            );
        }
    }

    /**
     * 构建应用主表实体
     */
    private App buildAppEntity(String appId, CreateAppRequest request, String currentUserId) {
        App app = new App();
        app.setId(idGenerator.nextId());
        app.setAppId(appId);
        app.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        app.setIconId(request.getIconId());
        app.setAppNameCn(request.getNameCn());
        app.setAppNameEn(request.getNameEn());
        app.setAppDescCn(request.getDescCn());
        app.setAppDescEn(request.getDescEn());
        app.setAppType(AppTypeEnum.BUSINESS.getCode());
        app.setAppSubType(AppSubTypeEnum.BUSINESS_STANDARD.getCode());
        app.setStatus(StatusEnum.ENABLED.getCode());
        app.setCreateBy(currentUserId);
        app.setLastUpdateBy(currentUserId);
        return app;
    }

    /**
     * 保存应用属性（EAMAP 编码 + 默认认证方式）
     */
    private void saveAppProperties(Long appInternalId, String eamapAppCode) {
        List<AppProperty> props = new ArrayList<>();
        props.add(createProperty(appInternalId, AppPropertyConstants.PROP_EAMAP_CODE, eamapAppCode));
        props.add(createProperty(appInternalId, AppPropertyConstants.PROP_VERIFY_TYPE, CommonConstants.DEFAULT_VERIFY_TYPE));
        appMapper.batchInsertProperties(props);
    }

    /**
     * 将创建者添加为应用 Owner
     */
    private void saveOwnerMember(Long appInternalId, String currentUserId) {
        Employee ownerEmp = employeeMapper.selectByWelinkId(currentUserId);
        if (Objects.isNull(ownerEmp)) {
            throw new BusinessException(
                    ResponseCodeEnum.EMPLOYEE_NOT_FOUND.getCode(),
                    ResponseCodeEnum.EMPLOYEE_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.EMPLOYEE_NOT_FOUND.getMessageEn()
            );
        }
        AppMember ownerMember = new AppMember();
        ownerMember.setId(idGenerator.nextId());
        ownerMember.setAppId(appInternalId);
        ownerMember.setAccountId(currentUserId);
        ownerMember.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        ownerMember.setMemberNameCn(ownerEmp.getChineseName());
        ownerMember.setMemberNameEn(ownerEmp.getEnglishName());
        ownerMember.setMemberType(MemberTypeEnum.OWNER.getCode());
        ownerMember.setStatus(StatusEnum.ENABLED.getCode());
        ownerMember.setCreateBy(currentUserId);
        ownerMember.setLastUpdateBy(currentUserId);
        appMemberMapper.insert(ownerMember);
    }

    private String generateAppId() {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(CommonConstants.APP_ID_DATE_FORMAT));
        int random = new Random().nextInt(9000) + 1000; // 1000~9999
        return timestamp + random;
    }

    /**
     * 保存应用凭证（ak/sk）
     */
    private void saveAppIdentity(Long internalAppId, String appId, String operatorId) {
        String ak = CommonConstants.AK_PREFIX + appId;
        String sk = CommonConstants.SK_PREFIX + appId + "_" + System.currentTimeMillis();
        AppIdentity identity = new AppIdentity();
        identity.setId(idGenerator.nextId());
        identity.setAppId(internalAppId);
        identity.setPublicKey(ak);
        identity.setPrivateKey(sk);
        identity.setKeyVersion("v1");
        identity.setKitVersion("v1");
        identity.setAk(ak);
        identity.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        identity.setStatus(StatusEnum.ENABLED.getCode());
        identity.setCreateBy(operatorId);
        identity.setLastUpdateBy(operatorId);
        appIdentityMapper.insert(identity);
        log.info("Credentials saved: appId={}, ak={}", appId, ak);
    }

    /**
     * 校验 EAMAP 绑定条件：未被绑定 + 编码存在 + 当前用户是负责人
     *
     * @param eamapAppCode EAMAP 编码
     * @return EAMAP 实体
     */
    private Eamap validateEamapBinding(String eamapAppCode) {
        // 校验 EAMAP 是否已被其他应用绑定
        if (appMapper.countByPropertyNameAndValue(AppPropertyConstants.PROP_EAMAP_CODE, eamapAppCode) > 0) {
            throw new BusinessException(
                    ResponseCodeEnum.EAMAP_ALREADY_BOUND.getCode(),
                    ResponseCodeEnum.EAMAP_ALREADY_BOUND.getMessageZh(),
                    ResponseCodeEnum.EAMAP_ALREADY_BOUND.getMessageEn()
            );
        }

        // 校验 EAMAP 编码存在
        Eamap eamap = eamapMapper.selectByEamapAppCode(eamapAppCode);
        if (Objects.isNull(eamap)) {
            throw new BusinessException(
                    ResponseCodeEnum.EAMAP_NOT_FOUND.getCode(),
                    ResponseCodeEnum.EAMAP_NOT_FOUND.getMessageZh(),
                    ResponseCodeEnum.EAMAP_NOT_FOUND.getMessageEn()
            );
        }

        // 校验当前用户是 EAMAP 负责人
        String currentUserId = UserContextHolder.getUserId();
        if (!currentUserId.equals(eamap.getOwnerAccountId())) {
            throw new BusinessException(
                    ResponseCodeEnum.NOT_EAMAP_OWNER.getCode(),
                    ResponseCodeEnum.NOT_EAMAP_OWNER.getMessageZh(),
                    ResponseCodeEnum.NOT_EAMAP_OWNER.getMessageEn()
            );
        }

        return eamap;
    }

    private AppProperty createProperty(Long parentId, String name, String value) {
        String currentUser = UserContextHolder.getUserId();
        AppProperty prop = new AppProperty();
        prop.setId(idGenerator.nextId());
        prop.setParentId(parentId);
        prop.setPropertyName(name);
        prop.setPropertyValue(value);
        prop.setTenantId(CommonConstants.DEFAULT_TENANT_ID);
        prop.setStatus(StatusEnum.ENABLED.getCode());
        prop.setCreateBy(currentUser);
        prop.setLastUpdateBy(currentUser);
        return prop;
    }

    /**
     * 加密 apiSecret
     */
    private String encryptApiSecret(String plainText) {
        return plainText;
    }

    /**
     * 解密 apiSecret
     */
    private String decryptApiSecret(String cipherText) {
        return cipherText;
    }

    /**
     * 解密 sk
     */
    private String decryptSk(String cipherText) {
        return cipherText;
    }

}
