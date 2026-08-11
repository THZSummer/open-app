-- 连接流执行结果缓存写入脚本 (connector-api)
-- 原子化: SET 业务数据 + SADD 索引 (一次往返, 保证写缓存与索引一致)
-- 兼容 Redis 单机 + 集群模式
--
-- 集群约束: KEYS[1] 与 KEYS[2] 前缀均为 "cp:cache:flow:" ,
--           按 Redis Cluster hash 规则落在同一 slot, 无 CROSSSLOT 风险
--
-- KEYS[1]: 业务数据 key (e.g., cp:cache:flow:{flowId}:{cacheKey})
-- KEYS[2]: 索引 key     (e.g., cp:cache:flow:keys:{flowId})
-- ARGV[1]: json 值
-- ARGV[2]: TTL (秒)
-- 返回: 1 = 成功

redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2])
redis.call('SADD', KEYS[2], KEYS[1])
return 1
