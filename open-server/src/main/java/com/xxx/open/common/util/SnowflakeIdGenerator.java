package com.xxx.open.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 雪花 ID 生成器
 * 
 * <p>生成全局唯一的 64 位 ID，结构：</p>
 * <pre>
 * 0 - 41位时间戳 - 10位机器ID - 12位序列号
 * </pre>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {

    /**
     * 起始时间戳（2024-01-01 00:00:00）
     */
    private static final long EPOCH = 1704038400000L;

    /**
     * 机器 ID 所占位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心 ID 所占位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号所占位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器 ID 最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 数据中心 ID 最大值
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号最大值
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器 ID 左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心 ID 左移位数
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 机器 ID（默认 0）
     */
    private long workerId = 0L;

    /**
     * 数据中心 ID（默认 0）
     */
    private long datacenterId = 0L;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成 ID 的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 生成下一个 ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(
                    String.format("时钟回拨 %d 毫秒", lastTimestamp - timestamp));
        }

        // 同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 阻塞到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 设置机器 ID
     */
    public void setWorkerId(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    String.format("机器 ID 超出范围 [0, %d]", MAX_WORKER_ID));
        }
        this.workerId = workerId;
    }

    /**
     * 设置数据中心 ID
     */
    public void setDatacenterId(long datacenterId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    String.format("数据中心 ID 超出范围 [0, %d]", MAX_DATACENTER_ID));
        }
        this.datacenterId = datacenterId;
    }
}
