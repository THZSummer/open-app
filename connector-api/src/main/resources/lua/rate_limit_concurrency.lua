-- Redis 并发限流脚本 (connector-api)
-- 原子操作: INCR + 首次EXPIRE, 消除两步竞态
--
-- 集群兼容: 单 key 操作 (KEYS[1]), 不触发 CROSSSLOT
-- Hash tag: key 中使用 {flowId} 确保同一 flow 落同一 slot
--
-- KEYS[1]: 限流 key (e.g., cp:ratelimit:concurrency:{flowId})
-- ARGV[1]: 最大并发数
-- ARGV[2]: TTL (秒, 防止 key 泄漏)
-- 返回: >=1 = 当前并发数(允许), -1 = 超限(拒绝)

local key = KEYS[1]
local max_concurrency = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

local current = redis.call('INCR', key)

if current == 1 then
    -- 首次, 设置 TTL 防泄漏
    redis.call('EXPIRE', key, ttl)
end

if current <= max_concurrency then
    return current  -- 允许, 返回当前并发数
else
    -- 超限, 回退计数器
    redis.call('DECR', key)
    return -1  -- 拒绝
end
