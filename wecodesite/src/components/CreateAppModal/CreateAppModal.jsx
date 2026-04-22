import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Upload, Button, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import BindEamapSelect from '../BindEamapSelect/BindEamapSelect';
import './CreateAppModal.m.less';

function CreateAppModal({ visible, onCancel, onOk, defaultIcons = [], eamapOptions = [] }) {
  const [form] = Form.useForm();
  const [selectedIcon, setSelectedIcon] = useState('');
  const [uploadedIcon, setUploadedIcon] = useState(null);

  useEffect(() => {
    if (visible) {
      form.resetFields();
      setSelectedIcon('');
      setUploadedIcon(null);
    }
  }, [visible, form]);

  const handleIconSelect = (icon) => {
    setSelectedIcon(icon);
    setUploadedIcon(null);
    form.setFieldsValue({ icon });
  };

  const handleUploadChange = (info) => {
    const file = info.file.originFileObj || info.file;
    if (file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        setUploadedIcon(e.target.result);
        setSelectedIcon('');
        form.setFieldsValue({ icon: e.target.result });
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!values.icon) {
        message.error('请选择或上传应用图标');
        return;
      }
      onOk(values);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  const displayIcon = uploadedIcon || selectedIcon;

  return (
    <Modal
      title="创建应用"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="创建"
      width={600}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          icon: '',
        }}
      >
        <Form.Item
          name="icon"
          label="应用图标"
          rules={[{ required: true, message: '请选择或上传应用图标' }]}
        >
          <div className="icon-section">
            <div className="icon-preview-area">
              {displayIcon ? (
                <div className="icon-preview">
                  {uploadedIcon ? (
                    <img src={displayIcon} alt="uploaded" className="uploaded-icon" />
                  ) : (
                    <span className="emoji-icon">{displayIcon}</span>
                  )}
                </div>
              ) : (
                <div className="icon-placeholder">图标预览</div>
              )}
              <Upload
                showUploadList={false}
                beforeUpload={() => false}
                onChange={handleUploadChange}
                accept="image/*"
              >
                <Button icon={<UploadOutlined />}>上传图片</Button>
              </Upload>
            </div>
            <div className="default-icons">
              <div className="default-icons-label">默认图标（点击选择）</div>
              <div className="default-icons-grid">
                {defaultIcons.map((icon, index) => (
                  <div
                    key={index}
                    className={`default-icon-item ${selectedIcon === icon ? 'selected' : ''}`}
                    onClick={() => handleIconSelect(icon)}
                  >
                    {icon}
                  </div>
                ))}
              </div>
            </div>
          </div>
        </Form.Item>

        <Form.Item
          name="chineseName"
          label="中文名称"
          rules={[{ required: true, message: '请输入应用中文名称' }]}
        >
          <Input placeholder="请输入应用中文名称" maxLength={50} />
        </Form.Item>

        <Form.Item
          name="englishName"
          label="英文名称"
          rules={[{ required: true, message: '请输入应用英文名称' }]}
        >
          <Input placeholder="请输入应用英文名称" maxLength={100} />
        </Form.Item>

        <Form.Item
          name="chineseDesc"
          label="中文应用描述"
        >
          <Input.TextArea placeholder="请输入中文应用描述" rows={3} maxLength={500} />
        </Form.Item>

        <Form.Item
          name="englishDesc"
          label="英文应用描述"
        >
          <Input.TextArea placeholder="请输入英文应用描述" rows={3} maxLength={1000} />
        </Form.Item>

        <Form.Item
          name="eamap"
          label="绑定到应用服务"
        >
          <BindEamapSelect
            eamapOptions={eamapOptions}
            placeholder="请选择要绑定的EAMAP（可选）"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default CreateAppModal;