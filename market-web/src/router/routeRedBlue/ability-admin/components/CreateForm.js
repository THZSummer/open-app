/**
 * 能力目录 — 新增表单弹窗组件
 *
 * 提供能力创建表单，包含字段输入、文件上传、前端校验及提交。
 * 对应 FR-002：平台管理员在 market-web 新增 ability 类型。
 */
import React, { useState, useEffect, useRef } from 'react';
import {
  Modal, Form, Input, InputNumber, Select, Upload, message,
} from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import ImgCrop from 'antd-img-crop';
import { uploadFile, createAbility } from '../thunk';
import less from '../index.module.less';

const { TextArea } = Input;

/**
 * 将图片文件缩放至指定尺寸
 * @param {File} file - 原始图片文件
 * @param {number} width - 目标宽度
 * @param {number} height - 目标高度
 * @returns {Promise<File>} 缩放后的文件
 */
const resizeImage = (file, width, height) => {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => {
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(img, 0, 0, width, height);
      canvas.toBlob((blob) => {
        if (!blob) {
          reject(new Error('Canvas toBlob failed'));
          return;
        }
        const resizedFile = new File([blob], file.name, { type: file.type });
        resolve(resizedFile);
      }, file.type);
    };
    img.onerror = () => reject(new Error('Image load failed'));
    img.src = URL.createObjectURL(file);
  });
};

/** 图片格式校验 */
const ACCEPTED_ICON_TYPES = ['image/png', 'image/jpeg', 'image/svg+xml'];
const ACCEPTED_DIAGRAM_TYPES = ['image/png', 'image/jpeg'];

/** 图标大小限制：200KB */
const ICON_MAX_SIZE = 200 * 1024;
/** 示意图大小限制：500KB */
const DIAGRAM_MAX_SIZE = 500 * 1024;

/** 加载类型选项 */
const LOAD_TYPE_OPTIONS = [
  { label: '路由加载', value: 1 },
  { label: '微前端加载', value: 2 },
];

/**
 * 创建能力表单弹窗
 *
 * @param {Object} props
 * @param {boolean} props.open - 是否显示
 * @param {Function} props.onClose - 关闭回调
 * @param {Function} props.onSuccess - 创建成功后回调（用于刷新列表）
 */
const CreateForm = ({ open, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [iconUploading, setIconUploading] = useState(false);
  const [diagramUploading, setDiagramUploading] = useState(false);

  // 存储上传后的 batchId
  const iconBatchIdRef = useRef(null);
  const diagramBatchIdRef = useRef(null);

  // 本地预览 URL
  const [iconPreview, setIconPreview] = useState(null);
  const [diagramPreview, setDiagramPreview] = useState(null);

  // 描述字符计数
  const [descLength, setDescLength] = useState(0);

  // loadType 决定 entryUrl/routePath/aliasName 是否联动必填
  const loadType = Form.useWatch('loadType', form);

  // 上传校验 & 调用
  const beforeIconUpload = (file) => {
    const isValidType = ACCEPTED_ICON_TYPES.includes(file.type);
    if (!isValidType) {
      message.error('图标仅支持 PNG/JPEG/SVG 格式');
      return Upload.LIST_IGNORE;
    }
    const isValidSize = file.size <= ICON_MAX_SIZE;
    if (!isValidSize) {
      message.error('图标大小不能超过 200KB');
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  const beforeDiagramUpload = (file) => {
    const isValidType = ACCEPTED_DIAGRAM_TYPES.includes(file.type);
    if (!isValidType) {
      message.error('示意图仅支持 PNG/JPEG 格式');
      return Upload.LIST_IGNORE;
    }
    const isValidSize = file.size <= DIAGRAM_MAX_SIZE;
    if (!isValidSize) {
      message.error('示意图大小不能超过 500KB');
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  /**
   * 自定义图标上传
   */
  const handleIconUpload = async (options) => {
    const { file, onSuccess: uploadSuccess, onError } = options;
    setIconUploading(true);
    try {
      // 缩放至 40×40PX 以满足后端要求
      const resized = await resizeImage(file, 40, 40);
      const result = await uploadFile(resized, 1);
      if (result && result.code === '200') {
        iconBatchIdRef.current = result.data?.batchId || result.batchId;
        setIconPreview(result.data?.showUrl || result.showUrl);
        uploadSuccess(result);
      } else {
        onError(result?.messageZh || '图标上传失败');
      }
    } catch (err) {
      onError('图标上传异常');
    } finally {
      setIconUploading(false);
    }
  };

  /**
   * 自定义示意图上传
   */
  const handleDiagramUpload = async (options) => {
    const { file, onSuccess: uploadSuccess, onError } = options;
    setDiagramUploading(true);
    try {
      const resized = await resizeImage(file, 520, 288);
      const result = await uploadFile(resized, 2);
      if (result && result.code === '200') {
        diagramBatchIdRef.current = result.data?.batchId || result.batchId;
        setDiagramPreview(result.data?.showUrl || result.showUrl);
        uploadSuccess(result);
      } else {
        onError(result?.messageZh || '示意图上传失败');
      }
    } catch (err) {
      onError('示意图上传异常');
    } finally {
      setDiagramUploading(false);
    }
  };

  /**
   * 处理关闭
   */
  const handleCancel = () => {
    form.resetFields();
    setIconPreview(null);
    setDiagramPreview(null);
    iconBatchIdRef.current = null;
    diagramBatchIdRef.current = null;
    setDescLength(0);
    onClose?.();
  };

  /**
   * 处理提交
   */
  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      // 图标 batchId 校验（必填）
      if (!iconBatchIdRef.current) {
        message.error('请上传能力图标');
        return;
      }

      setLoading(true);
      const payload = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        descCn: values.descCn,
        descEn: values.descEn,
        iconBatchId: iconBatchIdRef.current,
        diagramBatchId: diagramBatchIdRef.current || '',
        orderNum: values.orderNum,
        entryUrl: values.entryUrl,
        routePath: values.routePath,
        aliasName: values.aliasName || '',
        loadType: values.loadType || 1,
        abilityType: values.abilityType,
        // 后端给默认值的字段不传
      };

      const result = await createAbility(payload);
      if (result && result.code === '200') {
        message.success('能力添加成功');
        handleCancel();
        onSuccess?.();
      } else {
        message.error(result?.messageZh || result?.messageEn || '创建失败');
      }
    } catch (err) {
      // validateFields 校验失败时 err 为 Error，不做额外处理
      if (err?.errorFields) {
        // Ant Design 校验失败，已有表单内联提示
        return;
      }
      message.error('提交异常，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="添加能力"
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      okText="保存"
      cancelText="取消"
      destroyOnClose
      maskClosable={false}
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          loadType: 1,
          orderNum: 1,
        }}
      >
        {/* 第一行：能力标题 + 英文名 */}
        <div className={less.formRow}>
          <Form.Item
            name="nameCn"
            label={<span className={less.requiredLabel}>能力标题</span>}
            rules={[
              { required: true, message: '请输入能力标题' },
              { min: 2, message: '标题至少 2 个字符' },
              { max: 30, message: '标题不超过 30 个字符' },
            ]}
            className={less.formItemFlex}
          >
            <Input placeholder="请输入能力标题（2~30字符）" maxLength={30} />
          </Form.Item>
          <Form.Item
            name="nameEn"
            label={<span className={less.requiredLabel}>英文名</span>}
            rules={[
              { required: true, message: '请输入英文名' },
              { min: 2, message: '英文名至少 2 个字符' },
              { max: 30, message: '英文名不超过 30 个字符' },
            ]}
            className={less.formItemFlex}
          >
            <Input placeholder="如：assistant-square" maxLength={30} />
          </Form.Item>
        </div>

        {/* 第二行：能力描述 + 英文描述 */}
        <div className={less.formRow}>
          <Form.Item
            name="descCn"
            label={<span className={less.requiredLabel}>能力描述</span>}
            rules={[
              { required: true, message: '请输入能力描述' },
              { min: 5, message: '描述至少 5 个字符' },
              { max: 200, message: '描述不超过 200 个字符' },
            ]}
            className={less.formItemFlex}
            extra={<span className={less.charCounter}>{descLength} / 200 字符</span>}
          >
            <TextArea
              rows={3}
              placeholder="请输入能力描述（5~200字符）"
              maxLength={200}
              showCount={false}
              onChange={(e) => setDescLength(e.target.value.length)}
            />
          </Form.Item>
          <Form.Item
            name="descEn"
            label={<span className={less.requiredLabel}>英文描述</span>}
            rules={[
              { required: true, message: '请输入英文描述' },
              { min: 5, message: '英文描述至少 5 个字符' },
              { max: 200, message: '英文描述不超过 200 个字符' },
            ]}
            className={less.formItemFlex}
          >
            <TextArea rows={3} maxLength={200} placeholder="请输入英文描述" />
          </Form.Item>
        </div>

        {/* 图标上传 */}
        <Form.Item
          name="iconBatchId"
          label={<span className={less.requiredLabel}>能力图标</span>}
          rules={[
            { required: true, message: '请上传能力图标（PNG/SVG，40×40PX，≤200KB）' },
          ]}
        >
          <div className={less.uploadWrapper}>
            <ImgCrop
              rotationSlider
              aspect={1}
              quality={1}
              modalTitle="裁剪图标"
              modalOk="确认"
              modalCancel="取消"
            >
              <Upload
                listType="picture-card"
                showUploadList={false}
                customRequest={handleIconUpload}
                beforeUpload={beforeIconUpload}
                accept=".png,.jpeg,.jpg,.svg"
              >
                {iconPreview ? (
                  <img src={iconPreview} alt="图标预览" className={less.uploadPreviewImg} />
                ) : (
                  <div className={less.uploadPlaceholder}>
                    {iconUploading ? (
                      <LoadingOutlined />
                    ) : (
                      <PlusOutlined />
                    )}
                    <div className={less.uploadPlaceholderText}>点击上传</div>
                  </div>
                )}
              </Upload>
            </ImgCrop>
            <div className={less.uploadHint}>
              <div>尺寸：40 × 40 像素</div>
              <div>大小：不超过 200KB</div>
              <div>格式：PNG / JPEG / SVG</div>
            </div>
          </div>
        </Form.Item>

        {/* 示意图上传 */}
        <Form.Item
          name="diagramBatchId"
          label="示意图（非必填）"
        >
          <div className={less.uploadWrapper}>
            <ImgCrop
              rotationSlider
              aspect={520 / 288}
              quality={1}
              modalTitle="裁剪示意图"
              modalOk="确认"
              modalCancel="取消"
            >
              <Upload
                listType="picture-card"
                showUploadList={false}
                customRequest={handleDiagramUpload}
                beforeUpload={beforeDiagramUpload}
                accept=".png,.jpeg,.jpg"
              >
                {diagramPreview ? (
                  <img src={diagramPreview} alt="示意图预览" className={less.diagramPreviewImg} />
                ) : (
                  <div className={less.uploadPlaceholder}>
                    {diagramUploading ? (
                      <LoadingOutlined />
                    ) : (
                      <PlusOutlined />
                    )}
                    <div className={less.uploadPlaceholderText}>点击上传</div>
                  </div>
                )}
              </Upload>
            </ImgCrop>
            <div className={less.uploadHint}>
              <div>尺寸：520 × 288 像素</div>
              <div>大小：不超过 500KB</div>
              <div>格式：PNG / JPEG</div>
            </div>
          </div>
        </Form.Item>

        {/* 排序号 + 能力类型编码 */}
        <div className={less.formRow}>
          <Form.Item
            name="orderNum"
            label={<span className={less.requiredLabel}>排序号</span>}
            rules={[
              { required: true, message: '请输入排序号' },
              { type: 'number', min: 1, message: '排序号需 ≥ 1' },
            ]}
            className={less.formItemFlex}
          >
            <InputNumber
              precision={0}
              style={{ width: '100%' }}
              placeholder="正整数，越小越靠前"
            />
          </Form.Item>
          <Form.Item
            name="abilityType"
            label={<span className={less.requiredLabel}>能力类型编码</span>}
            rules={[
              { required: true, message: '请输入能力类型编码' },
              { type: 'number', message: '请输入数字' },
            ]}
            className={less.formItemFlex}
          >
            <InputNumber
              min={1}
              precision={0}
              style={{ width: '100%' }}
              placeholder="唯一数字编码"
            />
          </Form.Item>
        </div>

        {/* 访问地址 */}
        <Form.Item
          name="entryUrl"
          label={<span className={less.requiredLabel}>访问地址</span>}
          rules={[
            { required: true, message: '请输入访问地址' },
            {
              pattern: /^https?:\/\//,
              message: '地址需以 http:// 或 https:// 开头',
            },
          ]}
        >
          <Input placeholder="如：https://assistant.example.com" />
        </Form.Item>

        {/* 路由路径 */}
        <Form.Item
          name="routePath"
          label={<span className={less.requiredLabel}>路由路径</span>}
          rules={[
            { required: true, message: '请输入路由路径' },
            {
              pattern: /^\//,
              message: '路由路径需以 / 开头',
            },
          ]}
        >
          <Input placeholder="如：/assistant-square" />
        </Form.Item>

        {/* 别名 + 加载类型 */}
        <div className={less.formRow}>
          <Form.Item
            name="aliasName"
            label="别名"
            className={less.formItemFlex}
          >
            <Input placeholder="可选别名" />
          </Form.Item>
          <Form.Item
            name="loadType"
            label={<span className={less.requiredLabel}>加载类型</span>}
            rules={[{ required: true, message: '请选择加载类型' }]}
            className={less.formItemFlex}
          >
            <Select options={LOAD_TYPE_OPTIONS} placeholder="请选择加载类型" />
          </Form.Item>
        </div>

        {/* 微前端加载时 entryUrl/routePath/aliasName 提示 */}
        {loadType === 2 && (
          <div className={less.formHint}>
            微前端加载模式下，访问地址、路由路径、别名均为必填
          </div>
        )}
      </Form>
    </Modal>
  );
};

export default CreateForm;
