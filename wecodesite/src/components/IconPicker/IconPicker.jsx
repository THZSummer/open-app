import React, { useState, useEffect } from 'react';
import { Upload, message, Spin } from 'antd';
import { PlusOutlined, EyeOutlined, CloseOutlined } from '@ant-design/icons';
import { validateFile, validateImageDimensions } from '../../utils/common';
import { FILE_VALIDATION } from '../../utils/constants';
import { uploadImage, fetchDefaultIcons } from '../../pages/AppList/thunk';

import './IconPicker.m.less';

/**
 * 图标选择器组件 — CreateAppModal 和 BasicInfo 编辑态共用
 *
 * 布局（纵向）：
 * 1. 上传方格（未上传时显示 + 号，上传后显示图片 + 预览/删除按钮）
 * 2. 图片要求文案
 * 3. "默认" 标签 + 预设图标列表
 *
 * @param {string} value - 当前选中的图标 fileId
 * @param {string} uploadedUrl - 已上传的图标 URL
 * @param {function} onChange - (fileId, url) => void
 */
function IconPicker({ value, uploadedUrl, onChange }) {
  const [uploading, setUploading] = useState(false);
  const [localUploadedUrl, setLocalUploadedUrl] = useState(uploadedUrl || '');
  const [presetIcons, setPresetIcons] = useState([]);
  const [presetLoading, setPresetLoading] = useState(false);

  // 同步外部 uploadedUrl
  useEffect(() => {
    setLocalUploadedUrl(uploadedUrl || '');
  }, [uploadedUrl]);

  // 组件挂载时从后端加载预设图标
  useEffect(() => {
    loadPresetIcons();
  }, []);

  const loadPresetIcons = async () => {
    setPresetLoading(true);
    try {
      const result = await fetchDefaultIcons();
      if (result?.code === '200' && Array.isArray(result.data)) {
        setPresetIcons(result.data);
      }
    } catch {
      console.warn('加载预设图标失败');
    } finally {
      setPresetLoading(false);
    }
  };

  // 判断当前选中的是否为上传的自定义图标
  const isUploaded = value && !presetIcons.some(p => p.fileId === value) && localUploadedUrl;

  // 获取当前展示的图片 URL
  const currentImageUrl = isUploaded
    ? localUploadedUrl
    : (presetIcons.find(p => p.fileId === value)?.url || '');

  // 选择预设图标
  const handlePresetSelect = (fileId, url) => {
    setLocalUploadedUrl('');
    onChange && onChange(fileId, url || '');
  };

  // 上传自定义图标
  const handleIconUpload = async (file) => {
    const rawFile = file.originFileObj || file;

    const fileValidation = validateFile(rawFile, FILE_VALIDATION.icon);
    if (!fileValidation.valid) {
      message.error(fileValidation.message);
      return false;
    }

    const dimensionValidation = await validateImageDimensions(rawFile, FILE_VALIDATION.icon.width, FILE_VALIDATION.icon.height);
    if (!dimensionValidation.valid) {
      message.error(dimensionValidation.message || FILE_VALIDATION.icon.dimensionMessage);
      return false;
    }

    setUploading(true);
    try {
      const formData = new FormData();
      formData.append('file', rawFile);
      const result = await uploadImage(1, formData);
      if (result?.code === '200' && result.data) {
        setLocalUploadedUrl(result.data.url);
        onChange && onChange(result.data.fileId, result.data.url);
        message.success('图标上传成功');
      } else {
        message.error('图标上传失败');
      }
    } catch {
      message.error('图标上传失败');
    } finally {
      setUploading(false);
    }
    return false;
  };

  // 清除选择（回到 + 号状态，可重新上传）
  const handleRemove = (e) => {
    e.stopPropagation();
    setLocalUploadedUrl('');
    onChange && onChange('', '');
  };

  // 预览图片 - 新标签页打开
  const handlePreview = (e) => {
    e.stopPropagation();
    if (currentImageUrl) {
      window.open(currentImageUrl, '_blank');
    }
  };

  return (
    <div className="icon-picker-wrapper">
      {/* 上传方格 */}
      {currentImageUrl ? (
        <div className="icon-upload-box has-image">
          <img src={currentImageUrl} alt="icon" className="icon-upload-img" />
          <div className="icon-upload-overlay">
            <span className="icon-upload-action" onClick={handlePreview}>
              <EyeOutlined />
            </span>
            <span className="icon-upload-action" onClick={handleRemove}>
              <CloseOutlined />
            </span>
          </div>
        </div>
      ) : (
        <Upload
          showUploadList={false}
          beforeUpload={() => false}
          accept="image/png,image/jpeg,image/jpg"
          onChange={({ file }) => handleIconUpload(file)}
        >
          <div className="icon-upload-box">
            {uploading ? <Spin size="small" /> : <PlusOutlined className="icon-upload-plus" />}
          </div>
        </Upload>
      )}

      {/* 图片要求文案 */}
      <div className="icon-requirements">
        图片要求：128×128px，PNG、JPG、JPEG格式，小于100K
      </div>

      {/* 默认图标 */}
      <div className="icon-preset-section">
        <span className="icon-preset-label">默认</span>
        <div className="icon-preset-list">
          {presetLoading ? (
            <Spin size="small" />
          ) : (
            presetIcons.map((preset) => (
              <div
                key={preset.fileId}
                className={`icon-preset-item ${value === preset.fileId && !isUploaded ? 'selected' : ''}`}
                title={preset.fileId}
                onClick={() => handlePresetSelect(preset.fileId, preset.url)}
              >
                <img src={preset.url} alt="" className="icon-preset-img" />
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}

export default IconPicker;
