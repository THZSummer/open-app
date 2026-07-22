import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Spin } from 'antd';
import * as Icons from '@ant-design/icons';
import { fetchSubscribedAbilities, fetchAbilityList, addAbility } from './thunk';
import { ABILITY_TYPE_MAP, ABILITY_SCENE_MAP } from '../../utils/constants';
import { ABILITY_CHANGED_EVENT } from '../../components/Layout/Sidebar/Sidebar';
import { useSelector } from 'react-redux';
import { useRoleGuard } from '../../hooks/useRoleGuard';

import './Capabilities.m.less';

/**
 * 根据后端返回的 icon 名称动态渲染 Ant Design Icon 组件
 */
function DynamicIcon({ iconName, size = 12 }) {
  if (!iconName) return null;
  const IconComp = Icons[iconName];
  if (!IconComp) return null;
  return <IconComp style={{ fontSize: size }} />;
}

/**
 * 应用能力页
 *
 * 布局：右侧内容区整体结构
 *   - 标题：添加应用能力
 *   - 描述：你可以根据实际需求, 为应用开启服务...
 *   - Tab 切换：消息场景（目前只有一个）
 *   - 能力卡片网格
 */
function Capabilities() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const appId = searchParams.get('appId');
  const activeSubKey = searchParams.get('sub') || 'add';

  // 页面级权限守卫
  const { loading: roleTypeLoading } = useRoleGuard(appId);
  const { appBaseInfo } = useSelector(state => state.app);

  const [abilities, setAbilities] = useState([]);
  const [subscribedAbilities, setSubscribedAbilities] = useState([]);
  const [loading, setLoading] = useState(false);
  // 当前选中的场景 Tab
  const sceneKeys = Object.keys(ABILITY_SCENE_MAP);
  const [activeScene, setActiveScene] = useState(sceneKeys[0] || 'message');

  useEffect(() => {
    if (!appId || roleTypeLoading) {
      if (!appId) navigate('/');
      return;
    }
    if (!appBaseInfo) return;
    if (appBaseInfo.appType !== 1) {
      navigate(`/basic-info?appId=${appId}`);
      return;
    }
    loadAbilities();
  }, [appId, roleTypeLoading, appBaseInfo]);

  const loadAbilities = async () => {
    setLoading(true);
    try {
      const [subscribedRes, listRes] = await Promise.all([
        fetchSubscribedAbilities(appId),
        fetchAbilityList(appId),
      ]);
      if (subscribedRes?.code === '200') {
        setSubscribedAbilities(subscribedRes.data || []);
      }
      if (listRes?.code === '200') {
        const mapped = (listRes.data || [])
          .filter(a => a.abilityType !== 6)
          .map(a => ({ ...a, illustration: a.diagramUrl }));
        setAbilities(mapped);
      }
    } catch (error) {
      message.error('加载能力列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleAddAbility = async (abilityType) => {
    const result = await addAbility(appId, { abilityType });
    if (result?.code === '200') {
      message.success('能力添加成功');
      await loadAbilities();
      window.dispatchEvent(new CustomEvent(ABILITY_CHANGED_EVENT));
      navigate(`/capabilities?appId=${appId}&sub=${abilityType}`);
    } else {
      message.error(result?.messageZh || '添加失败');
    }
  };

  const isSubscribed = (type) => subscribedAbilities.some((a) => a.abilityType === type);

  const currentSubscribedAbility = subscribedAbilities.find(
    (a) => String(a.abilityType) === activeSubKey && a.abilityType !== 6
  );

  // 按场景分组能力卡片
  const groupedAbilities = Object.entries(ABILITY_SCENE_MAP).map(([sceneKey, scene]) => ({
    key: sceneKey,
    name: scene.name,
    description: scene.description,
    abilities: abilities.filter((a) => scene.types.includes(a.abilityType)),
  }));

  const currentGroup = groupedAbilities.find((g) => g.key === activeScene);

  return (
    <div className="capabilities-page">
      {activeSubKey === 'add' ? (
        <div className="content-card">
          <div className="capabilities-header">
            <h2 className="capabilities-title">添加应用能力</h2>
            <p className="capabilities-desc">你可以根据实际需求, 为应用开启服务。单个应用可开启多种能力, 一个能力可用于一个或多个场景</p>
          </div>

          {/* 场景 Tab */}
          <div className="capabilities-tabs">
            {groupedAbilities.map((group) => (
              <div
                key={group.key}
                className={`capabilities-tab ${activeScene === group.key ? 'active' : ''}`}
                onClick={() => setActiveScene(group.key)}
              >
                {group.name}
              </div>
            ))}
          </div>

          {/* 能力卡片 */}
          <Spin spinning={loading}>
            <div className="ability-grid">
              {currentGroup?.abilities.map((ability) => (
                <div key={ability.abilityType} className="ability-card">
                  {/* 示意图区域 */}
                  <div className="ability-image">
                    {ability.illustration ? (
                      <img src={ability.illustration} alt={ability.nameCn} />
                    ) : ability.iconUrl ? (
                      <img src={ability.iconUrl} alt={ability.nameCn} />
                    ) : (
                      <DynamicIcon iconName={ability.icon} size={20} />
                    )}
                  </div>
                  {/* 名称（带图标） */}
                  <div className="ability-name">
                    {ability.iconUrl ? (
                      <img src={ability.iconUrl} alt="" className="ability-name-icon" />
                    ) : (
                      <DynamicIcon iconName={ability.icon} size={12} />
                    )}
                    <span>{ability.nameCn}</span>
                  </div>
                  {/* 描述 */}
                  <div className="ability-desc">{ability.descCn}</div>
                  {/* 按钮 — 已添加显示"配置"，未添加显示"添加" */}
                  {isSubscribed(ability.abilityType) ? (
                    <button
                      className="btn-config"
                      onClick={() => navigate(`/capabilities?appId=${appId}&sub=${ability.abilityType}`)}
                    >
                      配置
                    </button>
                  ) : (
                    <button
                      className="btn-add"
                      onClick={() => handleAddAbility(ability.abilityType)}
                    >
                      添加
                    </button>
                  )}
                </div>
              ))}
            </div>
          </Spin>
        </div>
      ) : (
        // 已订阅能力配置页
        <div className="capability-config-page">
          <div className="info-card">
            <div className="info-card-header">
              <h3>{currentSubscribedAbility?.nameCn || ABILITY_TYPE_MAP[Number(activeSubKey)]?.text || '能力配置'}</h3>
            </div>
            <div className="info-card-body">
              <p style={{ color: '#8f959e', fontSize: 14 }}>
                {currentSubscribedAbility?.nameCn || '该能力'}已添加，配置页面由能力方提供。
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default Capabilities;
