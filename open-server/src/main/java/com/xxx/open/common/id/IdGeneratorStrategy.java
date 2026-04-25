package com.xxx.open.common.id;

/**
 * ID 生成器策略接口
 * 
 * <p>支持不同环境下的 ID 生成策略，实现策略模式</p>
 * 
 * @author SDDU Build Agent
 * @version 1.0.0
 */
public interface IdGeneratorStrategy {

    /**
     * 生成下一个 ID
     * 
     * @return 唯一 ID
     */
    long nextId();

    /**
     * 判断当前策略是否支持指定的环境
     * 
     * @param activeProfile 当前激活的环境配置
     * @return true-支持, false-不支持
     */
    boolean supports(String activeProfile);
}
