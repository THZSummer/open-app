import React, { useState, useEffect } from 'react';
import { Button, Input, Select, Radio, Checkbox, message } from 'antd';
const { TextArea } = Input;
import { ArrowLeftOutlined } from '@ant-design/icons';
import { useNavigate, useLocation } from 'react-router-dom';
import './VersionForm.m.less';
import mockData from './mock';

const { Option } = Select;
const { Group: RadioGroup } = Radio;

const VersionForm = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');
  const versionId = searchParams.get('versionId');
  const [version, setVersion] = useState({
    version: '',
    description: '',
    releaseType: 'minor',
    isPublic: false,
    capabilities: []
  });

  useEffect(() => {
    // 如果是查看版本详情，加载版本数据
    if (versionId) {
      const versionData = mockData.versions.find(v => v.id === versionId);
      if (versionData) {
        setVersion({
          ...versionData,
          releaseType: 'minor',
          isPublic: false,
          capabilities: ['bot', 'web']
        });
      }
    }
  }, [versionId]);

  const handleBack = () => {
    const params = new URLSearchParams();
    if (appId) params.append('appId', appId);
    navigate(`/version-release?${params.toString()}`);
  };

  const handleSave = () => {
    message.success('保存成功');
  };

  const handleSubmit = () => {
    message.success('提交成功');
    handleBack();
  };

  const handleCancel = () => {
    handleBack();
  };

  return (
    <div className="versionForm">
      <Button
        type="text"
        icon={<ArrowLeftOutlined />}
        onClick={handleBack}
        className="backButton"
      >
        返回版本列表
      </Button>
      <h1 className="title">
        {versionId ? '查看版本' : '创建版本'}
      </h1>
      <div className="card">
        <div className="formRow">
          <span className="label">版本号：</span>
          <Input
            value={version.version}
            onChange={(e) => setVersion({ ...version, version: e.target.value })}
            disabled={!!versionId}
          />
        </div>
        <div className="formRow">
          <span className="label">发布类型：</span>
          <RadioGroup
            value={version.releaseType}
            onChange={(e) => setVersion({ ...version, releaseType: e.target.value })}
            disabled={!!versionId}
          >
            <Radio value="major">重大更新</Radio>
            <Radio value="minor">次要更新</Radio>
            <Radio value="patch">补丁更新</Radio>
          </RadioGroup>
        </div>
        <div className="formRow">
          <span className="label">版本描述：</span>
          <TextArea
            rows={4}
            value={version.description}
            onChange={(e) => setVersion({ ...version, description: e.target.value })}
            disabled={!!versionId}
            placeholder="请描述本次版本的主要变更"
          />
        </div>
        <div className="formRow">
          <span className="label">发布范围：</span>
          <Checkbox
            checked={version.isPublic}
            onChange={(e) => setVersion({ ...version, isPublic: e.target.checked })}
            disabled={!!versionId}
          >
            公开发布
          </Checkbox>
        </div>
        <div className="formRow">
          <span className="label">包含能力：</span>
          <Select
            mode="multiple"
            value={version.capabilities}
            onChange={(values) => setVersion({ ...version, capabilities: values })}
            disabled={!!versionId}
            style={{ width: 300 }}
          >
            <Option value="bot">机器人</Option>
            <Option value="web">网页应用</Option>
            <Option value="miniapp">小程序</Option>
            <Option value="widget">小组件</Option>
          </Select>
        </div>
        <div className="formActions">
          <Button onClick={handleSave} disabled={!!versionId}>
            保存草稿
          </Button>
          <Button type="primary" onClick={handleSubmit} disabled={!!versionId}>
            提交发布
          </Button>
          <Button onClick={handleCancel}>
            取消
          </Button>
        </div>
      </div>
    </div>
  );
};

export default VersionForm;