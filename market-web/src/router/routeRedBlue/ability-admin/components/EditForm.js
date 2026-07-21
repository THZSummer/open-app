/**
 * 能力目录 — 编辑表单弹窗组件
 *
 * 提供能力编辑表单，基于 CreateForm 扩展，支持数据回填、字段只读及提交更新。
 * 对应 FR-003：平台管理员在 market-web 编辑 ability 类型信息。
 */
import React, { useState, useEffect, useRef } from 'react';
import {
  Modal, Form, Input, InputNumber, Select, Upload, message,
} from 'antd';
import { PlusOutlined, LoadingOutlined } from '@ant-design/icons';
import ImgCrop from 'antd-img-crop';
import { uploadFile, updateAbility } from '../thunk';
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
 * 编辑能力表单弹窗
 *
 * @param {Object} props
 * @param {boolean} props.open - 是否显示
 * @param {Object|null} props.record - 当前编辑的能力记录数据
 * @param {Function} props.onClose - 关闭回调
 * @param {Function} props.onSuccess - 更新成功后回调（用于刷新列表）
 */
const EditForm = ({ open, record, onClose, onSuccess }) => {
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

  /**
   * 打开弹窗时根据 record 回填表单
   */
  useEffect(() => {
    if (open && record) {
      form.setFieldsValue({
        nameCn: record.nameCn || '',
        nameEn: record.nameEn || '',
        descCn: record.descCn || '',
        descEn: record.descEn || '',
        orderNum: record.orderNum ?? 1,
        abilityType: record.abilityType,
        entryUrl: record.entryUrl || '',
        routePath: record.routePath || '',
        aliasName: record.aliasName || '',
        loadType: record.loadType ?? 1,
      });
      setDescLength((record.descCn || '').length);

      // 回填图标/示意图预览
      if (record.iconUrl) {
        setIconPreview(record.iconUrl);
      }
      if (record.exampleDiagramUrl) {
        setDiagramPreview(record.exampleDiagramUrl);
      }

      // 重置 batchId 引用（无上传时沿用旧值，意味着不修改）
      iconBatchIdRef.current = null;
      diagramBatchIdRef.current = null;
    }
  }, [open, record, form]);

  /**
   * 关闭时重置状态
   */
  useEffect(() => {
    if (!open) {
      form.resetFields();
      setIconPreview(null);
      setDiagramPreview(null);
      iconBatchIdRef.current = null;
      diagramBatchIdRef.current = null;
      setDescLength(0);
    }
  }, [open, form]);

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
    onClose?.();
  };

  /**
   * 处理提交
   */
  const handleOk = async () => {
    try {
      const values = await form.validateFields();

      if (!record || !record.id) {
        message.error('缺少能力 ID，无法更新');
        return;
      }

      // 如果用户没有重新上传图标，则不传 batchId（后端保持原值）
      // 如果用户重新上传了，则传新的 batchId
      const payload = {
        id: record.id,
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        descCn: values.descCn,
        descEn: values.descEn,
        orderNum: values.orderNum,
        entryUrl: values.entryUrl,
        routePath: values.routePath,
        aliasName: values.aliasName || '',
        loadType: values.loadType || 1,
      };

      // 仅在用户重新上传时传 batchId
      if (iconBatchIdRef.current) {
        payload.iconBatchId = iconBatchIdRef.current;
      }
      if (diagramBatchIdRef.current) {
        payload.diagramBatchId = diagramBatchIdRef.current;
      }

      setLoading(true);
      const result = await updateAbility(payload);
      if (result && result.code === '200') {
        message.success('能力更新成功');
        handleCancel();
        onSuccess?.();
      } else {
        message.error(result?.messageZh || result?.messageEn || '更新失败');
      }
    } catch (err) {
      if (err?.errorFields) {
        return;
      }
      message.error('提交异常，请稍后重试');
    } finally {
      setLoading(false);
    }
  };

  const isMicroFrontend = loadType === 2;

  const entryUrlRules = [
    ...(isMicroFrontend ? [{ required: true, message: '微前端模式下访问地址为必填' }] : []),
    { pattern: /^https?:\/\/.+/, message: '请输入合法的 http 或 https 地址' },
  ];

  const routePathRules = [
    ...(isMicroFrontend ? [{ required: true, message: '微前端模式下路由路径为必填' }] : []),
    { pattern: /^\//, message: '路由路径需以 / 开头' },
  ];

  const aliasNameRules = [
    ...(isMicroFrontend ? [{ required: true, message: '微前端模式下别名为必填' }] : []),
  ];

  return (
    <Modal
      title="编辑能力"
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

        {/* 排序号 + 能力类型编码（禁用） */}
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
            className={less.formItemFlex}
          >
            <InputNumber
              disabled
              precision={0}
              style={{ width: '100%', color: '#999' }}
            />
          </Form.Item>
        </div>

        {/* 访问地址 */}
        <Form.Item
          name="entryUrl"
          label={isMicroFrontend ? <span className={less.requiredLabel}>访问地址</span> : "访问地址"}
          rules={entryUrlRules}
        >
          <Input placeholder="如：https://assistant.example.com" />
        </Form.Item>

        {/* 路由路径 */}
        <Form.Item
          name="routePath"
          label={isMicroFrontend ? <span className={less.requiredLabel}>路由路径</span> : "路由路径"}
          rules={routePathRules}
        >
          <Input placeholder="如：/assistant-square" />
        </Form.Item>

        {/* 别名 + 加载类型 */}
        <div className={less.formRow}>
          <Form.Item
            name="aliasName"
            label={isMicroFrontend ? <span className={less.requiredLabel}>别名</span> : "别名"}
            rules={aliasNameRules}
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

export default EditForm;
