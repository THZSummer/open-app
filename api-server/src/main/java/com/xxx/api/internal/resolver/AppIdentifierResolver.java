package com.xxx.api.internal.resolver;

import com.xxx.api.common.exception.BusinessException;
import com.xxx.api.internal.entity.AppEntity;
import com.xxx.api.internal.entity.AppPropertyEntity;
import com.xxx.api.internal.mapper.AppEntityMapper;
import com.xxx.api.internal.mapper.AppPropertyEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppIdentifierResolver {

    private final AppEntityMapper appEntityMapper;
    private final AppPropertyEntityMapper appPropertyEntityMapper;

    /**
     * 解析应用标识，返回内部 varchar app_id。
     * 优先按 appId 查 app_t，未匹配则按 hisAppId 查 app_p_t.eamap_app_code，
     * 均未匹配抛出 BusinessException(404)。
     */
    public String resolve(String appId, String hisAppId) {
        // 优先按 appId (varchar) 查
        if (appId != null && !appId.isBlank()) {
            AppEntity app = appEntityMapper.selectByAppId(appId);
            if (app != null) {
                log.debug("App resolved by appId: {} -> id={}", appId, app.getId());
                return app.getAppId();
            }
            log.debug("App not found by appId: {}, trying hisAppId", appId);
        }

        // 按 hisAppId (eamap_app_code) 查
        if (hisAppId != null && !hisAppId.isBlank()) {
            AppPropertyEntity prop = appPropertyEntityMapper.selectByEamapAppCode(hisAppId);
            if (prop != null) {
                AppEntity app = appEntityMapper.selectById(prop.getParentId());
                if (app != null) {
                    log.debug("App resolved by hisAppId: {} -> appId={}", hisAppId, app.getAppId());
                    return app.getAppId();
                }
            }
            log.debug("App not found by hisAppId: {}", hisAppId);
        }

        log.warn("No matching application found for appId={}, hisAppId={}", appId, hisAppId);
        throw BusinessException.notFound("应用不存在", "Application not found");
    }
}
