-- Redis Token Bucket 限流脚本 (connector-api)
-- KEYS[1]: 限流 key (e.g., cp:ratelimit:qps:{flowId}:{second})
-- ARGV[1]: 最大令牌数 (QPS 限制)
-- ARGV[2]: TTL (秒, 建议 2s for QPS)
-- 返回: 1 = 允许, 0 = 拒绝

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- 获取当前令牌数
local current = redis.call('GET', key)
if current == false then
    -- 首次请求, 初始化桶
    redis.call('SET', key, max_tokens - 1, 'EX', ttl)
    return 1  -- allowed
else
    current = tonumber(current)
    if current > 0 then
        redis.call('DECR', key)
        return 1  -- allowed
    else
        return 0  -- rejected
    end
end
