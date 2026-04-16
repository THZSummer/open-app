import React, { useState } from 'react';
import { Button, Input, Radio, Upload, message } from 'antd';
import { EyeInvisibleOutlined, EyeTwoTone, CopyOutlined } from '@ant-design/icons';
import './BasicInfo.m.less';
import mockData from './mock';

const { TextArea } = Input;
const { Group: RadioGroup } = Radio;

const BasicInfo = () => {
  const [appInfo] = useState(mockData.appInfo);
  const [editMode, setEditMode] = useState({
    basic: false,
    auth: false
  });
  const [appSecretVisible, setAppSecretVisible] = useState(false);

  const handleCopyAppSecret = () => {
    navigator.clipboard.writeText(appInfo.appSecret).then(() => {
      message.success('复制成功');
    });
  };

  const handleEdit = (section) => {
    setEditMode(prev => ({ ...prev, [section]: true }));
  };

  const handleSave = (section) => {
    setEditMode(prev => ({ ...prev, [section]: false }));
    message.success('保存成功');
  };

  const handleCancel = (section) => {
    setEditMode(prev => ({ ...prev, [section]: false }));
  };

  return (
    <div className="basicInfo">
      <h1 className="title">凭证和基础信息</h1>
      <p className="description">管理应用凭证、安全配置和回调地址</p>

      {/* 应用凭证 */}
      <div className="section">
        <div className="sectionHeader">
          <h2 className="sectionTitle">应用凭证</h2>
        </div>
        <div className="card">
          <div className="infoRow">
            <span className="label">APP ID:</span>
            <span className="value">{appInfo.appId}</span>
          </div>
          <div className="infoRow">
            <span className="label">APP Secret:</span>
            <div className="secretWrapper">
              <span className="value">
                {appSecretVisible ? appInfo.appSecret : '**************'}
              </span>
              <Button
                type="text"
                icon={appSecretVisible ? <EyeInvisibleOutlined /> : <EyeTwoTone />}
                onClick={() => setAppSecretVisible(!appSecretVisible)}
              />
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={handleCopyAppSecret}
              />
            </div>
          </div>
        </div>
      </div>

      {/* 基础信息 */}
      <div className="section">
        <div className="sectionHeader">
          <h2 className="sectionTitle">基础信息</h2>
          {!editMode.basic && (
            <Button type="link" onClick={() => handleEdit('basic')}>
              编辑
            </Button>
          )}
        </div>
        <div className="card">
          <div className="formRow">
            <span className="label">应用图标:</span>
            <div className="uploadWrapper">
              {appInfo.icon ? (
                <div className="iconPreview">
                  <img src={appInfo.icon} alt="应用图标" />
                </div>
              ) : (
                <div className="iconPlaceholder">预览64x64</div>
              )}
              {editMode.basic && (
                <Upload>
                  <Button>上传</Button>
                </Upload>
              )}
            </div>
          </div>
          <div className="formRow">
            <span className="label">中文名称:</span>
            {editMode.basic ? (
              <Input value={appInfo.name.zh} />
            ) : (
              <span className="value">{appInfo.name.zh}</span>
            )}
          </div>
          <div className="formRow">
            <span className="label">英文名称:</span>
            {editMode.basic ? (
              <Input value={appInfo.name.en} />
            ) : (
              <span className="value">{appInfo.name.en}</span>
            )}
          </div>
          <div className="formRow">
            <span className="label">中文描述:</span>
            {editMode.basic ? (
              <TextArea rows={3} value={appInfo.description.zh} />
            ) : (
              <span className="value">{appInfo.description.zh}</span>
            )}
          </div>
          <div className="formRow">
            <span className="label">英文描述:</span>
            {editMode.basic ? (
              <TextArea rows={3} value={appInfo.description.en} />
            ) : (
              <span className="value">{appInfo.description.en}</span>
            )}
          </div>
          <div className="formRow">
            <span className="label">功能示意图:</span>
            <div className="screenshotWrapper">
              {appInfo.screenshot ? (
                <div className="screenshotPreview">
                  <img src={appInfo.screenshot} alt="功能示意图" />
                </div>
              ) : (
                <div className="screenshotPlaceholder">--</div>
              )}
              {editMode.basic && (
                <Upload>
                  <Button>上传</Button>
                </Upload>
              )}
            </div>
          </div>
          {editMode.basic && (
            <div className="formActions">
              <Button onClick={() => handleSave('basic')}>保存</Button>
              <Button onClick={() => handleCancel('basic')}>取消</Button>
            </div>
          )}
        </div>
      </div>

      {/* 认证方式 */}
      <div className="section">
        <div className="sectionHeader">
          <h2 className="sectionTitle">认证方式</h2>
          {!editMode.auth && (
            <Button type="link" onClick={() => handleEdit('auth')}>
              编辑
            </Button>
          )}
        </div>
        <div className="card">
          <RadioGroup
            value={appInfo.authType}
            disabled={!editMode.auth}
          >
            <Radio value="cookie">Cookie</Radio>
            <Radio value="signature">数字签名</Radio>
            <Radio value="soaHeader">SOAHeader</Radio>
            <Radio value="soaUrl">SOAURL</Radio>
          </RadioGroup>
          {editMode.auth && (
            <div className="formActions">
              <Button onClick={() => handleSave('auth')}>保存</Button>
              <Button onClick={() => handleCancel('auth')}>取消</Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default BasicInfo;