import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, message, Tooltip } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import BindEamapSelect from '../BindEamapSelect/BindEamapSelect';
import IconPicker from '../IconPicker/IconPicker';

import './CreateAppModal.m.less';

/**
 * 创建应用弹窗
 *
 * 功能：
 * - 应用图标选择（预设/上传）
 * - 中英文名称和描述输入
 * - 绑定 EAMAP 应用服务
 *
 * 图标选择器抽取为公共组件 IconPicker，与 BasicInfo 编辑态共用
 *
 * @param {Object} props - 组件属性
 * @param {boolean} props.visible - 弹窗是否可见
 * @param {Function} props.onCancel - 取消回调
 * @param {Function} props.onOk - 确认回调
 * @param {Array} props.eamapOptions - EAMAP 选项列表
 * @param {boolean} props.confirmLoading - 提交中状态
 */
function CreateAppModal(props) {
  const { visible, onCancel, onOk, eamapOptions = [], confirmLoading = false } = props;
  const [form] = Form.useForm();
  // 当前选中的图标 fileId（预设图标 fileId / 上传后返回的真实 fileId）
  const [iconId, setIconId] = useState('');
  // 自定义上传图标的 URL
  const [iconUrl, setIconUrl] = useState('');

  // 弹窗打开时：重置表单和图标
  useEffect(() => {
    if (visible) {
      form.resetFields();
      setIconId('');
      setIconUrl('');
    }
  }, [visible, form]);

  // 提交
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!iconId) {
        message.warning('请选择应用图标');
        return;
      }
      onOk({
        ...values,
        icon: iconId,
      });
    } catch (error) {
      // form validation failed
    }
  };

  return (
    <Modal
      title="创建WeLink应用"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="创建"
      cancelText="取消"
      width={600}
      destroyOnClose
      confirmLoading={confirmLoading}
      centered
      bodyStyle={{ paddingTop: 16 }}
    >
      <Form form={form} layout="vertical" className="create-app-form">
        {/* 图标选择 — 使用公共 IconPicker */}
          <Form.Item label={<span><span className="required-mark">*</span> 应用图标</span>}>
          <IconPicker
            value={iconId}
            uploadedUrl={iconUrl}
            onChange={(fileId, url) => {
              setIconId(fileId);
              setIconUrl(url);
            }}
          />
        </Form.Item>

        <Form.Item
          name="chineseName"
          label="中文名称"
          rules={[{ required: true, message: '应用中文名不能为空' }, { max: 255, message: '不超过255字符' }]}
        >
          <Input placeholder="请输入应用中文名称" maxLength={255} showCount />
        </Form.Item>

        <Form.Item
          name="englishName"
          label="英文名称"
          rules={[{ required: true, message: '应用英文名不能为空' }, { max: 255, message: '不超过255字符' }]}
        >
          <Input placeholder="请输入应用英文名称" maxLength={255} showCount />
        </Form.Item>

        <Form.Item
          name="chineseDesc"
          label="中文描述"
          rules={[{ max: 2000, message: '描述不超过2000字符' }]}
        >
          <Input.TextArea placeholder="请输入中文应用描述" rows={2} maxLength={2000} showCount />
        </Form.Item>

        <Form.Item
          name="englishDesc"
          label="英文描述"
          rules={[{ max: 2000, message: '描述不超过2000字符' }]}
        >
          <Input.TextArea placeholder="请输入英文应用描述" rows={2} maxLength={2000} showCount />
        </Form.Item>

        <Form.Item
          name="eamap"
          label={
            <span>
              绑定到应用服务
              <Tooltip
                overlayStyle={{ maxWidth: 'none' }}
                color="#eef4ff"
                title={<span className="eamap-tooltip-text">开放平台按照应用服务维度做权限隔离，如需申请API权限等开放能力需要先绑定应用服务，如无权限请前往应用中心查询对应责任人</span>}
              >
                <QuestionCircleOutlined className="eamap-question-icon" />
              </Tooltip>
            </span>
          }
          rules={[{ required: true, message: '请选择EAMAP' }]}
        >
          <BindEamapSelect
            eamapOptions={eamapOptions}
            placeholder="选择应用服务"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default CreateAppModal;
