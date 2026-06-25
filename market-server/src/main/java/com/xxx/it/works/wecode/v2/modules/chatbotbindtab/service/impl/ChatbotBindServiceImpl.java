package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.service.impl;

import com.xxx.it.works.wecode.v2.common.context.UserContextHolder;
import com.xxx.it.works.wecode.v2.common.exception.BusinessException;
import com.xxx.it.works.wecode.v2.common.id.IdGeneratorStrategy;
import com.xxx.it.works.wecode.v2.common.model.ApiResponse;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.client.WeContactClient;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.entity.AppPropertyEntity;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.mapper.ChatbotBindMapper;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.service.ChatbotBindService;
import com.xxx.it.works.wecode.v2.modules.chatbotbindtab.vo.ChatbotAccountVO;
import com.xxx.it.works.wecode.v2.modules.dictionary.entity.DictionaryEntity;
import com.xxx.it.works.wecode.v2.modules.dictionary.mapper.DictionaryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 机器人绑定服务实现
 *
 * <p>业务编排：通讯录 API 校验 → 数量上限校验 → 重复校验 → INSERT/DELETE</p>
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Slf4j
@Service
public class ChatbotBindServiceImpl implements ChatbotBindService {

    private static final String DICT_PATH = "CEC.Open";
    private static final String DICT_CODE = "single.chat.robot.max.number";
    private static final int DEFAULT_MAX_COUNT = 1;
    private static final String PROPERTY_NAME = "single_chatbot_account";

    @Autowired
    private ChatbotBindMapper chatbotBindMapper;

    @Autowired
    private DictionaryMapper dictionaryMapper;

    @Autowired
    private WeContactClient weContactClient;

    @Autowired
    private IdGeneratorStrategy idGenerator;

    @Override
    public ApiResponse<List<ChatbotAccountVO>> getBoundAccounts(String appId) {
        // 1. 通过 appId 查询应用主键 id
        Long appPkId = chatbotBindMapper.selectAppPkIdByAppId(appId);
        if (appPkId == null) {
            throw new BusinessException("40001", "应用不存在", "App not found");
        }

        // 2. 查询已绑定列表
        List<AppPropertyEntity> entities = chatbotBindMapper.selectBoundAccounts(appPkId);

        // 3. Entity -> VO 转换（Long -> String）
        List<ChatbotAccountVO> voList = new ArrayList<>();
        for (AppPropertyEntity entity : entities) {
            voList.add(toVO(entity));
        }

        return ApiResponse.success(voList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<ChatbotAccountVO> bindAccount(String appId, String accountId, String token) {
        // 1. 通过 appId 查询应用主键 id
        Long appPkId = chatbotBindMapper.selectAppPkIdByAppId(appId);
        if (appPkId == null) {
            throw new BusinessException("40001", "应用不存在", "App not found");
        }

        // 2. 调用通讯录 API 校验账号有效性
        try {
            boolean valid = weContactClient.validateAccount(accountId, token);
            if (!valid) {
                throw new BusinessException("40002", "账号无效", "Invalid account");
            }
        } catch (RestClientException e) {
            log.error("WeContact API call failed", e);
            throw new BusinessException("40006", "通讯录服务暂不可用", "Contact service unavailable", e);
        }

        // 3. 查询数据字典获取最大可绑定数量
        int maxCount = getMaxBindCount();

        // 4. 查询当前已绑定数量
        int currentCount = chatbotBindMapper.countBoundAccounts(appPkId);
        if (currentCount >= maxCount) {
            throw new BusinessException("40003", "超过最大可绑定数量", "Exceed max bind count");
        }

        // 5. 重复检查
        AppPropertyEntity existing = chatbotBindMapper.selectByAppPkIdAndAccountId(appPkId, accountId);
        if (existing != null) {
            throw new BusinessException("40004", "该账号已绑定", "Account already bound");
        }

        // 6. 生成雪花 ID 并插入
        AppPropertyEntity entity = new AppPropertyEntity();
        entity.setId(idGenerator.nextId());
        entity.setParentId(appPkId);
        entity.setPropertyName(PROPERTY_NAME);
        entity.setPropertyValue(accountId);
        entity.setStatus(1);
        entity.setCreateBy(UserContextHolder.getUserId());
        entity.setLastUpdateBy(UserContextHolder.getUserId());
        // tenantId 从应用记录继承 #ASSUMED
        entity.setTenantId("");

        chatbotBindMapper.insert(entity);

        // 7. 审计日志（预留人工实现）
        log.info("Bind account: appId={}, accountId={}, by={}", appId, accountId, entity.getCreateBy());

        // 8. Entity -> VO
        ChatbotAccountVO vo = toVO(entity);
        return ApiResponse.success(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> unbindAccount(String appId, String accountId) {
        // 1. 通过 appId 查询应用主键 id
        Long appPkId = chatbotBindMapper.selectAppPkIdByAppId(appId);
        if (appPkId == null) {
            throw new BusinessException("40001", "应用不存在", "App not found");
        }

        // 2. 查询绑定记录是否存在
        AppPropertyEntity existing = chatbotBindMapper.selectByAppPkIdAndAccountId(appPkId, accountId);
        if (existing == null) {
            throw new BusinessException("40005", "绑定记录不存在", "Binding record not found");
        }

        // 3. 硬删除
        chatbotBindMapper.deleteByAppPkIdAndAccountId(appPkId, accountId);

        // 4. 审计日志（预留人工实现）
        log.info("Unbind account: appId={}, accountId={}, by={}", appId, accountId, UserContextHolder.getUserId());

        return ApiResponse.success(null);
    }

    /**
     * 从数据字典读取最大可绑定数量
     */
    private int getMaxBindCount() {
        DictionaryEntity entity = dictionaryMapper.selectByPathAndCode(DICT_PATH, DICT_CODE);
        if (entity != null && entity.getValue() != null && !entity.getValue().isEmpty()) {
            try {
                return Integer.parseInt(entity.getValue());
            } catch (NumberFormatException e) {
                log.warn("Invalid max bind count value: {}, using default", entity.getValue());
            }
        }
        return DEFAULT_MAX_COUNT;
    }

    /**
     * Entity -> VO 转换（Long -> String，防 JS 精度丢失）
     */
    private ChatbotAccountVO toVO(AppPropertyEntity entity) {
        ChatbotAccountVO vo = new ChatbotAccountVO();
        vo.setId(String.valueOf(entity.getId()));
        vo.setAccountId(entity.getPropertyValue());
        vo.setBindTime(formatDate(entity.getCreateTime()));
        vo.setBindBy(entity.getCreateBy());
        return vo;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}
