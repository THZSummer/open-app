-- 连接流执行结果缓存写入脚本 (connector-api)
-- 原子化: SET 业务数据 + SADD 索引 (一次往返, 保证写缓存与索引一致)
-- 兼容 Redis 单机 + 集群模式
--
-- 集群约束: KEYS[1] 与 KEYS[2] 必须落在同一 slot, 否则 Redis Cluster 抛
-- CROSSSLOT 错误。调用方须保证两个 key 都含 {flowId} hash tag:
--   KEYS[1] = cp:cache:flow:{flowId}:{cacheKey}
--   KEYS[2] = cp:cache:flow:keys:{flowId}
-- Redis 对 hash tag 内的内容做 CRC16, 两 key 必同 slot, 无 CROSSSLOT 风险。
--
-- KEYS[1]: 业务数据 key
-- KEYS[2]: 索引 key
-- ARGV[1]: json 值
-- ARGV[2]: TTL (秒)
-- 返回: 1 = 成功

redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
redis.call('SADD', KEYS[2], KEYS[1])
return 1
