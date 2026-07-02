#!/usr/bin/env python3
"""Apply all four file changes for Tasks A-D."""

import re

# ============================================================
# Task A: EntityCacheManager.java
# ============================================================

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/cache/EntityCacheManager.java', 'r') as f:
    a_content = f.read()

# 1. Add imports after 'import java.util.concurrent.ThreadLocalRandom;\n'
imports_to_add = """import com.xxx.it.works.wecode.v2.modules.ratelimit.RateLimitConfig;
import com.xxx.it.works.wecode.v2.common.config.OpenplatformLookupRepository;
import com.xxx.it.works.wecode.v2.common.config.LookupItemEntity;
import java.util.LinkedHashMap;
import java.util.Map;
"""

a_content = a_content.replace(
    'import java.util.concurrent.ThreadLocalRandom;\n',
    'import java.util.concurrent.ThreadLocalRandom;\n' + imports_to_add,
    1
)

# 2. Add lookupRepository field
a_content = a_content.replace(
    '    private final OpFlowReadRepository flowReadRepository;\n',
    '    private final OpFlowReadRepository flowReadRepository;\n    private final OpenplatformLookupRepository lookupRepository;\n',
    1
)

# 3. Modify constructor parameters and body
old_constructor = """    public EntityCacheManager(ReactiveRedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper,
                               OpFlowVersionReadRepository flowVersionReadRepository,
                               OpFlowReadRepository flowReadRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.flowReadRepository = flowReadRepository;
    }"""

new_constructor = """    public EntityCacheManager(ReactiveRedisTemplate<String, String> redisTemplate,
                               ObjectMapper objectMapper,
                               OpFlowVersionReadRepository flowVersionReadRepository,
                               OpFlowReadRepository flowReadRepository,
                               OpenplatformLookupRepository lookupRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.flowReadRepository = flowReadRepository;
        this.lookupRepository = lookupRepository;
    }"""

a_content = a_content.replace(old_constructor, new_constructor, 1)

# 4. Add new cache methods before '    // ===== 内部方法 ====='
new_cache_methods = """    // ===== RateLimitConfig 缓存 (TTL 60s) =====

    private static final Duration RATELIMIT_TTL = Duration.ofSeconds(60);

    /**
     * 获取限流配置: Redis cache-aside, miss 时从 FlowVersion 提取并回写
     *
     * @param flowId 连接流ID
     * @return RateLimitConfig
     */
    public Mono<RateLimitConfig> getRateLimitConfig(Long flowId) {
        String key = ratelimitKey(flowId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, RateLimitConfig.class));
                    } catch (Exception e) {
                        log.warn("RateLimitConfig cache deserialize failed: flowId={}", flowId);
                        return Mono.<RateLimitConfig>empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> loadRateLimitFromDb(flowId)));
    }

    /**
     * 失效限流配置缓存
     */
    public Mono<Boolean> invalidateRateLimitCache(Long flowId) {
        return redisTemplate.opsForValue().delete(ratelimitKey(flowId));
    }

    private Mono<RateLimitConfig> loadRateLimitFromDb(Long flowId) {
        // 通过已缓存的 FlowVersion 获取限流配置
        return getFlowVersionByFlowId(flowId)
                .map(this::extractRateLimitConfig)
                .flatMap(config -> {
                    try {
                        String json = objectMapper.writeValueAsString(config);
                        return redisTemplate.opsForValue().set(ratelimitKey(flowId), json, RATELIMIT_TTL)
                                .thenReturn(config)
                                .onErrorResume(e -> Mono.just(config));
                    } catch (Exception e) {
                        return Mono.just(config);
                    }
                })
                .defaultIfEmpty(new RateLimitConfig("qps", 1000, 1000));
    }

    @SuppressWarnings("unchecked")
    private Mono<RateLimitConfig> extractRateLimitConfig(FlowVersionEntity version) {
        try {
            Map<String, Object> config = version.parseOrchestrationConfigAsMap(objectMapper);
            if (config == null) return Mono.empty();
            Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
            if (flowConfig == null) return Mono.empty();
            Object rlObj = flowConfig.get("rateLimitConfig");
            if (!(rlObj instanceof Map)) return Mono.empty();
            Map<String, Object> rl = (Map<String, Object>) rlObj;
            String mode = rl.get("mode") instanceof String ? (String) rl.get("mode") : "qps";
            int maxQps = rl.get("maxQps") instanceof Number ? ((Number) rl.get("maxQps")).intValue() : 1000;
            int maxCon = rl.get("maxConcurrency") instanceof Number ? ((Number) rl.get("maxConcurrency")).intValue() : 1000;
            return Mono.just(new RateLimitConfig(mode, Math.max(1, maxQps), Math.max(1, maxCon)));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    /**
     * 按 flowId 获取 FlowVersion (不走 versionId 缓存, 用 flowId 查)
     */
    private Mono<FlowVersionEntity> getFlowVersionByFlowId(Long flowId) {
        return flowVersionReadRepository.findByFlowId(flowId).next();
    }

    // ===== Lookup 配置缓存 (TTL 5min) =====

    private static final Duration LOOKUP_TTL = Duration.ofMinutes(5);
    private static final String LOOKUP_PATH = "CEC.Open";

    /**
     * 获取 Lookup 配置: Redis cache-aside, miss 时从 DB 查并回写
     *
     * @param classifyCode 分类编码
     * @return item_code → item_value 的 Map
     */
    public Mono<Map<String, String>> getLookupConfig(String classifyCode) {
        String key = lookupKey(classifyCode);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> {
                    try {
                        Map<String, String> map = objectMapper.readValue(json,
                                new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {});
                        log.debug("Lookup cache hit: classifyCode={}", classifyCode);
                        return Mono.just(map);
                    } catch (Exception e) {
                        log.warn("Lookup cache deserialize failed: classifyCode={}", classifyCode);
                        return Mono.<Map<String, String>>empty();
                    }
                })
                .switchIfEmpty(Mono.defer(() -> loadLookupFromDb(classifyCode)));
    }

    private Mono<Map<String, String>> loadLookupFromDb(String classifyCode) {
        return lookupRepository.findByPathAndClassifyCode(LOOKUP_PATH, classifyCode)
                .collectList()
                .map(items -> {
                    Map<String, String> result = new LinkedHashMap<>();
                    for (LookupItemEntity item : items) {
                        if (item.getItemCode() != null) {
                            result.put(item.getItemCode(), item.getItemValue());
                        }
                    }
                    return result;
                })
                .flatMap(map -> {
                    if (map.isEmpty()) return Mono.just(map);
                    try {
                        String json = objectMapper.writeValueAsString(map);
                        return redisTemplate.opsForValue().set(lookupKey(classifyCode), json, LOOKUP_TTL)
                                .thenReturn(map)
                                .onErrorResume(e -> Mono.just(map));
                    } catch (Exception e) {
                        return Mono.just(map);
                    }
                })
                .onErrorResume(e -> {
                    log.warn("Lookup DB load failed: classifyCode={}", classifyCode, e);
                    return Mono.just(new LinkedHashMap<>());
                });
    }

    /**
     * 失效 Lookup 配置缓存
     */
    public Mono<Boolean> invalidateLookupCache(String classifyCode) {
        return redisTemplate.opsForValue().delete(lookupKey(classifyCode));
    }

"""

a_content = a_content.replace(
    '    // ===== 内部方法 =====',
    new_cache_methods + '    // ===== 内部方法 =====',
    1
)

# 5. Add key builder methods after flowVersionKey
key_builders = """
    private String ratelimitKey(Long flowId) {
        return "cp:entity:ratelimit:" + flowId;
    }

    private String lookupKey(String classifyCode) {
        return "cp:entity:lookup:" + classifyCode;
    }
"""

a_content = a_content.replace(
    '    private String flowVersionKey(Long versionId) {\n        return "cp:entity:flowversion:" + versionId;\n    }',
    '    private String flowVersionKey(Long versionId) {\n        return "cp:entity:flowversion:" + versionId;\n    }' + key_builders,
    1
)

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/cache/EntityCacheManager.java', 'w') as f:
    f.write(a_content)

print("Task A: EntityCacheManager.java updated.")

# ============================================================
# Task B: RateLimitConfigReader.java
# ============================================================

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/ratelimit/RateLimitConfigReader.java', 'r') as f:
    b_content = f.read()

# 1. Add EntityCacheManager import after the ConnectorApiPropertyService import
b_content = b_content.replace(
    'import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;\n',
    'import com.xxx.it.works.wecode.v2.common.config.ConnectorApiPropertyService;\nimport com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;\n',
    1
)

# 2. Remove unused imports
b_content = b_content.replace('import com.xxx.it.works.wecode.v2.modules.flow.entity.FlowVersionEntity;\n', '', 1)
b_content = b_content.replace('import com.xxx.it.works.wecode.v2.modules.flow.repository.OpFlowVersionReadRepository;\n', '', 1)
b_content = b_content.replace('import com.fasterxml.jackson.databind.ObjectMapper;\n', '', 1)
b_content = b_content.replace('import java.util.Map;\n', '', 1)

# 3. Replace constructor and fields
old_b_fields = """    private final ObjectMapper objectMapper;
    private final OpFlowVersionReadRepository flowVersionReadRepository;
    private final ConnectorApiPropertyService propertyService;

    public RateLimitConfigReader(ObjectMapper objectMapper,
                                  OpFlowVersionReadRepository flowVersionReadRepository,
                                  ConnectorApiPropertyService propertyService) {
        this.objectMapper = objectMapper;
        this.flowVersionReadRepository = flowVersionReadRepository;
        this.propertyService = propertyService;
    }"""

new_b_fields = """    private final EntityCacheManager entityCacheManager;
    private final ConnectorApiPropertyService propertyService;

    public RateLimitConfigReader(EntityCacheManager entityCacheManager,
                                  ConnectorApiPropertyService propertyService) {
        this.entityCacheManager = entityCacheManager;
        this.propertyService = propertyService;
    }"""

b_content = b_content.replace(old_b_fields, new_b_fields, 1)

# 4. Replace readFlowRateLimit method
old_read_method = """    public Mono<RateLimitConfig> readFlowRateLimit(Long flowId) {
        return flowVersionReadRepository.findByFlowId(flowId)
                .flatMap(this::extractRateLimitConfig)
                .switchIfEmpty(defaultConfig())
                .onErrorResume(e -> {
                    log.warn("Rate limit config read failed for flowId={}, using defaults", flowId, e);
                    return defaultConfig();
                });
    }"""

new_read_method = """    public Mono<RateLimitConfig> readFlowRateLimit(Long flowId) {
        return entityCacheManager.getRateLimitConfig(flowId)
                .switchIfEmpty(defaultConfig())
                .onErrorResume(e -> {
                    log.warn("Rate limit config read failed for flowId={}, using defaults", flowId, e);
                    return defaultConfig();
                });
    }"""

b_content = b_content.replace(old_read_method, new_read_method, 1)

# 5. Remove extractRateLimitConfig method (from the empty line after readFlowRateLimit to before defaultConfig)
old_extract = """    /**
     * 从 FlowVersionEntity 中提取 rateLimitConfig
     * §3.3.4: 从 flowConfig.rateLimitConfig 读取, ③存在用③不截断, ③不存在回退①
     */
    @SuppressWarnings("unchecked")
    private Mono<RateLimitConfig> extractRateLimitConfig(FlowVersionEntity entity) {
        try {
            Map<String, Object> config = entity.parseOrchestrationConfigAsMap(objectMapper);
            if (config == null || config.isEmpty()) {
                return defaultConfig();
            }

            Map<String, Object> flowConfig = (Map<String, Object>) config.get("flowConfig");
            if (flowConfig == null) {
                return defaultConfig();
            }

            Object rateLimitObj = flowConfig.get("rateLimitConfig");
            if (!(rateLimitObj instanceof Map)) {
                return defaultConfig();
            }
            Map<String, Object> rateLimitConfig = (Map<String, Object>) rateLimitObj;

            Object modeObj = rateLimitConfig.get("mode");
            String mode = (modeObj instanceof String) ? (String) modeObj : "qps";

            Object maxQpsObj = rateLimitConfig.get("maxQps");
            Object maxConObj = rateLimitConfig.get("maxConcurrency");

            // ③的 maxQps 和 maxConcurrency 都存在 → 直接用③，不读①
            if (maxQpsObj instanceof Number && maxConObj instanceof Number) {
                int maxQps = ((Number) maxQpsObj).intValue();
                if (maxQps < 1) maxQps = 1;
                int maxConcurrency = ((Number) maxConObj).intValue();
                if (maxConcurrency < 1) maxConcurrency = 1;
                return Mono.just(new RateLimitConfig(mode, maxQps, maxConcurrency));
            }

            // ③的某个字段不存在 → 该字段回退①
            return propertyService.getFlowMaxQps()
                    .zipWith(propertyService.getFlowMaxConcurrency())
                    .map(tuple -> {
                        int maxQps = tuple.getT1();
                        int maxConcurrency = tuple.getT2();
                        if (maxQpsObj instanceof Number) {
                            maxQps = ((Number) maxQpsObj).intValue();
                            if (maxQps < 1) maxQps = 1;
                        }
                        if (maxConObj instanceof Number) {
                            maxConcurrency = ((Number) maxConObj).intValue();
                            if (maxConcurrency < 1) maxConcurrency = 1;
                        }
                        return new RateLimitConfig(mode, maxQps, maxConcurrency);
                    });
        } catch (Exception e) {
            log.warn("Failed to parse rate limit config for flowId={}, using defaults", entity.getFlowId(), e);
            return defaultConfig();
        }
    }

"""

b_content = b_content.replace(old_extract, '', 1)

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/ratelimit/RateLimitConfigReader.java', 'w') as f:
    f.write(b_content)

print("Task B: RateLimitConfigReader.java updated.")

# ============================================================
# Task C: ConnectorApiPropertyService.java
# ============================================================

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/config/ConnectorApiPropertyService.java', 'r') as f:
    c_content = f.read()

# 1. Add EntityCacheManager import
c_content = c_content.replace(
    'import org.springframework.stereotype.Service;\n',
    'import com.xxx.it.works.wecode.v2.modules.cache.EntityCacheManager;\nimport org.springframework.stereotype.Service;\n',
    1
)

# 2. Remove unused imports
c_content = c_content.replace('import org.springframework.beans.factory.annotation.Qualifier;\n', '', 1)
c_content = c_content.replace('import org.springframework.data.redis.core.ReactiveRedisTemplate;\n', '', 1)
c_content = c_content.replace('import java.util.HashMap;\n', '', 1)

# 3. Remove CACHE_KEY_PREFIX constant line
c_content = c_content.replace('    // Redis cache key prefix — must match market-server CacheServiceV2\n    private static final String CACHE_KEY_PREFIX = "OPENPLATFORM:LOOK:UP:ITEM:";\n\n', '', 1)

# 4. Replace constructor and fields
old_c_fields = """    private final OpenplatformLookupRepository lookupRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public ConnectorApiPropertyService(OpenplatformLookupRepository lookupRepository,
                                       @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.lookupRepository = lookupRepository;
        this.redisTemplate = redisTemplate;
    }"""

new_c_fields = """    private final EntityCacheManager entityCacheManager;

    public ConnectorApiPropertyService(EntityCacheManager entityCacheManager) {
        this.entityCacheManager = entityCacheManager;
    }"""

c_content = c_content.replace(old_c_fields, new_c_fields, 1)

# 5. Replace loadPlatformDefaults
c_content = c_content.replace(
    '    public Mono<Map<String, String>> loadPlatformDefaults() {\n        return loadWithCache(PATH, CLASSIFY_PLATFORM_CONFIG);\n    }',
    '    public Mono<Map<String, String>> loadPlatformDefaults() {\n        return entityCacheManager.getLookupConfig(CLASSIFY_PLATFORM_CONFIG);\n    }',
    1
)

# 6. Remove loadWithCache and loadFromDb methods
old_load_block = """    /**
     * 带 Redis 缓存的 classify 加载。
     * 缓存 key: OPENPLATFORM:LOOK:UP:ITEM:{path}:{classifyCode}
     */
    private Mono<Map<String, String>> loadWithCache(String path, String classifyCode) {
        String cacheKey = CACHE_KEY_PREFIX + path + ":" + classifyCode;

        return redisTemplate.opsForValue()
                .get(cacheKey)
                .flatMap(cached -> loadFromDb(path, classifyCode))
                .switchIfEmpty(loadFromDb(path, classifyCode));
    }

    /**
     * 从 DB 加载 classify 下所有 item_code → item_value。
     * DB 不可用时返回空 Map（调用方使用硬编码默认值）。
     */
    private Mono<Map<String, String>> loadFromDb(String path, String classifyCode) {
        return lookupRepository.findByPathAndClassifyCode(path, classifyCode)
                .collectList()
                .map(items -> {
                    Map<String, String> result = new HashMap<>();
                    for (LookupItemEntity item : items) {
                        if (item.getItemCode() != null) {
                            result.put(item.getItemCode(), item.getItemValue());
                        }
                    }
                    return result;
                })
                .onErrorResume(e -> {
                    log.warn("Failed to load Lookup items (path={}, classifyCode={}), using defaults",
                            path, classifyCode, e);
                    return Mono.just(new HashMap<>());
                });
    }

"""

c_content = c_content.replace(old_load_block, '', 1)

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/common/config/ConnectorApiPropertyService.java', 'w') as f:
    f.write(c_content)

print("Task C: ConnectorApiPropertyService.java updated.")

# ============================================================
# Task D: FlowInvokeService.java
# ============================================================

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/service/FlowInvokeService.java', 'r') as f:
    d_content = f.read()

# Replace flowVersionReadRepository.findById(deployedVersionId) with entityCacheManager.getFlowVersion(deployedVersionId)
d_content = d_content.replace(
    'return flowVersionReadRepository.findById(deployedVersionId)',
    'return entityCacheManager.getFlowVersion(deployedVersionId)',
    1
)

with open('/home/usb/wks/open-app/connector-api/src/main/java/com/xxx/it/works/wecode/v2/modules/flow/service/FlowInvokeService.java', 'w') as f:
    f.write(d_content)

print("Task D: FlowInvokeService.java updated.")
print("\nAll four files updated successfully.")
