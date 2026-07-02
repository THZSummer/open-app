-- Redis Token Bucket 限流脚本 (connector-api)
-- 兼容 Redis 单机 + 集群模式 (单 key 操作, 无 CROSSSLOT 风险)
--
-- KEYS[1]: 限流 key (e.g., rate_limit:{flowId}:2025-07-02T12:00:05)
--          注意: key 中必须包含 {flowId} hash tag, 确保集群模式下
--          同一 flow 的 QPS/并发 key 落在同一 slot
-- ARGV[1]: 最大令牌数 (QPS 限制)
-- ARGV[2]: TTL (秒, 1s for QPS 秒级桶)
-- 返回: 1 = 允许, 0 = 拒绝

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

-- 获取当前令牌数
local current = redis.call('GET', key)
if current == false then
    -- 首次请求, 初始化桶 (SET + EX 原子操作)
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
