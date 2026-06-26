/**
 * ========================================
 * 更多配置抽屉
 * ========================================
 *
 * 支持限流配置与缓存配置。
 * 视觉：分组卡片 + 进度可视化 + 暗色拼接预览面板。
 */

import React, { useState, useEffect } from 'react';
import { Drawer, InputNumber, Switch, Select, Button, Space, message } from 'antd';
import { ThunderboltOutlined, DatabaseOutlined } from '@ant-design/icons';
import '../FlowEditorV2.m.less';

const { Option } = Select;

/**
 * 构建缓存 Key 的完整触发器入参引用路径
 *
 * @param {Object} params 参数对象
 * @param {string} params.triggerNodeId 触发器节点 ID
 * @param {string} params.carrier 参数位置
 * @param {string} params.paramName 参数名称
 * @returns {string} 缓存 Key 引用路径
 */
export const buildCacheKeyValue = (params) => {
  // params.triggerNodeId / params.carrier / params.paramName
  const { triggerNodeId, carrier, paramName } = params;
  return `${triggerNodeId}.input.${carrier}.${paramName}`;
};

/**
 * 更多配置抽屉
 *
 * @param {Object} props
 * @param {boolean} props.visible 是否显示
 * @param {number} props.rateLimit 当前限流值
 * @param {boolean} props.cacheEnabled 是否开启缓存
 * @param {number} props.cacheTime 缓存时间（秒）
 * @param {Array<string>} props.cacheKeys 缓存 Key 列表（存储完整引用路径）
 * @param {Array} props.triggerInputParams 触发器入参列表（用于缓存 Key 选择）
 * @param {string} props.triggerNodeId 触发器节点 ID
 * @param {number} props.rateLimitMax 限流上限
 * @param {number} props.cacheTimeMax 缓存时间上限
 * @param {Function} props.onClose 关闭回调
 * @param {Function} props.onSave 保存回调 (config) => void
 */
const MoreConfigDrawer = (props) => {
  const {
    visible,
    editable = false,
    rateLimit,
    cacheEnabled,
    cacheTime,
    cacheKeys,
    triggerInputParams,
    triggerNodeId,
    rateLimitMax,
    cacheTimeMax,
    onClose,
    onSave,
  } = props;

  // 本地编辑暂存状态
  const [localRateLimit, setLocalRateLimit] = useState(rateLimit);
  const [localCacheEnabled, setLocalCacheEnabled] = useState(cacheEnabled);
  const [localCacheTime, setLocalCacheTime] = useState(cacheTime);
  const [localCacheKeys, setLocalCacheKeys] = useState(cacheKeys || []);

  // 抽屉打开时同步外部 props 到本地状态
  useEffect(() => {
    if (visible) {
      setLocalRateLimit(rateLimit);
      setLocalCacheEnabled(cacheEnabled);
      setLocalCacheTime(cacheTime);
      setLocalCacheKeys(cacheKeys || []);
    }
  }, [visible, rateLimit, cacheEnabled, cacheTime, cacheKeys]);

  /**
   * 添加一个空的缓存 Key 行
   */
  const handleAddCacheKey = () => {
    setLocalCacheKeys([...localCacheKeys, '']);
  };

  /**
   * 更新指定位置的缓存 Key
   *
   * @param {Object} params
   * @param {number} params.index 位置索引
   * @param {string} params.value 新值
   */
  const handleUpdateCacheKey = (params) => {
    // params.index / params.value
    const { index, value } = params;
    const next = [...localCacheKeys];
    next[index] = value;
    setLocalCacheKeys(next);
  };

  /**
   * 删除指定位置的缓存 Key
   * @param {number} index 位置索引
   */
  const handleRemoveCacheKey = (index) => {
    setLocalCacheKeys(localCacheKeys.filter((_, i) => i !== index));
  };

  /**
   * 保存配置
   */
  const handleSave = () => {
    if (localCacheEnabled) {
      if (!localCacheTime || localCacheTime <= 0) {
        message.error('请输入缓存时间');
        return;
      }
      if (localCacheTime > cacheTimeMax) {
        message.error(`上限为 ${cacheTimeMax} 秒`);
        return;
      }
      const validKeys = localCacheKeys.filter(Boolean);
      if (validKeys.length === 0) {
        message.error('至少配置一个缓存 Key');
        return;
      }
    }
    if (localRateLimit && localRateLimit > rateLimitMax) {
      message.error(`限流值不能超过应用上限 ${rateLimitMax}`);
      return;
    }
    onSave({
      rateLimit: localRateLimit,
      cacheEnabled: localCacheEnabled,
      cacheTime: localCacheTime,
      cacheKeys: localCacheKeys.filter(Boolean),
    });
  };

  // 触发器入参选项（合并 header/body/query 三类）
  const allTriggerParams = [
    ...(triggerInputParams?.header || []).map(p => ({ ...p, carrier: 'header' })),
    ...(triggerInputParams?.body || []).map(p => ({ ...p, carrier: 'body' })),
    ...(triggerInputParams?.query || []).map(p => ({ ...p, carrier: 'query' })),
  ];

  // 缓存 Key 拼接预览
  const cacheKeyPreview = localCacheKeys.filter(Boolean).join('&');

  return (
    <Drawer
      title={null}
      placement="right"
      width={560}
      open={visible}
      onClose={onClose}
      destroyOnClose
      footer={
        <Space style={{ float: 'right' }}>
          <Button onClick={onClose}>{editable ? '取消' : '关闭'}</Button>
          {editable && <Button type="primary" onClick={handleSave}>保存配置</Button>}
        </Space>
      }
    >
      {/* Hero 区 */}
      <div className="drawer-hero">
        <div className="drawer-hero-title">
          <span>更多配置</span>
        </div>
        <div className="drawer-hero-sub">配置当前连接流的运行级策略：限流与缓存</div>
      </div>

      {/* 限流配置 */}
      <div className="drawer-section">
        <div className="section-title">
          <ThunderboltOutlined style={{ color: '#3370ff' }} />
          限流配置
          <span className="section-title-extra">应用上限 {rateLimitMax}</span>
        </div>
        <InputNumber
          value={localRateLimit}
          min={0}
          max={rateLimitMax}
          style={{ width: '100%' }}
          placeholder="请输入限流值"
          addonAfter="QPS"
          disabled={!editable}
          onChange={(value) => setLocalRateLimit(value)}
        />
      </div>

      {/* 缓存配置 */}
      <div className={`drawer-section ${!localCacheEnabled ? 'section-disabled' : ''}`}>
        <div className="section-title">
          <DatabaseOutlined style={{ color: '#3370ff' }} />
          缓存配置
          <span className="section-title-extra" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <span>{localCacheEnabled ? '已开启' : '已关闭'}</span>
            <Switch
              size="small"
              checked={localCacheEnabled}
              disabled={!editable}
              onChange={(checked) => setLocalCacheEnabled(checked)}
            />
          </span>
        </div>

        {!localCacheEnabled ? (
          <div className="section-desc" style={{ marginBottom: 0 }}>
            开启后可配置缓存时间和缓存 Key，缓存命中时直接返回上次结果。
          </div>
        ) : (
          <>
            {/* 缓存时间 */}
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 13, color: '#4e5969', marginBottom: 6 }}>
                缓存时间 <span style={{ color: '#86909c' }}>· 单位秒，最大 {cacheTimeMax} 秒（15 天）</span>
              </div>
              <InputNumber
                value={localCacheTime}
                min={1}
                max={cacheTimeMax}
                step={1}
                style={{ width: '100%' }}
                placeholder="请输入缓存时间"
                addonAfter="秒"
                disabled={!editable}
                onChange={(value) => setLocalCacheTime(value)}
              />
            </div>

            {/* 缓存 Key */}
            <div>
              <div style={{ fontSize: 13, color: '#4e5969', marginBottom: 6 }}>
                缓存 Key <span style={{ color: '#86909c' }}>· 从触发器入参中选择字段，按顺序拼接</span>
              </div>
              {localCacheKeys.map((keyValue, idx) => (
                <div key={idx} className="cache-key-row">
                  <span className="cache-key-index">{idx + 1}</span>
                  <Select
                    style={{ flex: 1 }}
                    value={keyValue || undefined}
                    placeholder="从触发器入参中选择字段"
                    disabled={!editable}
                    onChange={(value) => handleUpdateCacheKey({ index: idx, value })}
                  >
                    {allTriggerParams.map(param => {
                      // 兼容 SchemaEditorV2 字段，并保存为完整触发器入参引用路径
                      const pName = param.paramName ?? param.name;
                      const cacheKeyValue = buildCacheKeyValue({
                        triggerNodeId,
                        carrier: param.carrier,
                        paramName: pName,
                      });
                      return (
                        <Option key={`${param.carrier}-${pName}`} value={cacheKeyValue}>
                          {pName}（{param.carrier}）
                        </Option>
                      );
                    })}
                  </Select>
                  <Button
                    danger
                    aria-label="删除"
                    disabled={!editable}
                    onClick={() => handleRemoveCacheKey(idx)}
                  >
                    删除
                  </Button>
                </div>
              ))}
              <Button
                type="dashed"
                disabled={!editable}
                onClick={handleAddCacheKey}
                style={{ width: '100%', marginTop: 4 }}
              >
                添加缓存 Key
              </Button>

              {/* 拼接预览 */}
              <div className="code-panel" data-label="cache key preview" style={{ marginTop: 12 }}>
                {cacheKeyPreview ? (
                  <pre>{cacheKeyPreview}</pre>
                ) : (
                  <span className="code-empty">// 暂未配置，添加缓存 Key 后此处展示拼接结果</span>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </Drawer>
  );
};

export default MoreConfigDrawer;
