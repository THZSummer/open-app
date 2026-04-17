import React, { useState } from 'react';
import { Modal, Form, Input, Upload, Button, Select } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload';
import styles from './CreateAppModal.module.less';

interface CreateAppModalProps {
  visible: boolean;
  onCancel: () => void;
  onSubmit: (values: any) => void;
  defaultIcons: string[];
  eamapOptions: { value: string; label: string }[];
}

const CreateAppModal: React.FC<CreateAppModalProps> = ({ visible, onCancel, onSubmit, defaultIcons, eamapOptions }) => {
  const [form] = Form.useForm();
  const [selectedIcon, setSelectedIcon] = useState<string>('');
  const [fileList, setFileList] = useState<UploadFile[]>([]);

  const handleIconClick = (icon: string) => {
    setSelectedIcon(icon);
    form.setFieldValue('icon', icon);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      onSubmit(values);
      form.resetFields();
      setSelectedIcon('');
      setFileList([]);
    });
  };

  const handleCancel = () => {
    form.resetFields();
    setSelectedIcon('');
    setFileList([]);
    onCancel();
  };

  return (
    <Modal title="创建应用" open={visible} onOk={handleSubmit} onCancel={handleCancel} width={600} destroyOnClose>
      <Form form={form} layout="vertical">
        <Form.Item label="应用图标" name="icon" rules={[{ required: true, message: '请选择或上传图标' }]}>
          <div className={styles.iconSection}>
            <div className={styles.preview}>{selectedIcon ? <span className={styles.selectedIcon}>{selectedIcon}</span> : <span className={styles.placeholder}>预览</span>}</div>
            <Upload fileList={fileList} onChange={({ fileList }) => setFileList(fileList)} maxCount={1} accept="image/*">
              <Button icon={<UploadOutlined />}>上传图片</Button>
            </Upload>
          </div>
          <div className={styles.defaultIcons}>
            {defaultIcons.map((icon) => (
              <div key={icon} className={`${styles.iconItem} ${selectedIcon === icon ? styles.selected : ''}`} onClick={() => handleIconClick(icon)}>{icon}</div>
            ))}
          </div>
        </Form.Item>
        <Form.Item label="中文名称" name="nameZh" rules={[{ required: true, message: '请输入中文名称' }]}><Input placeholder="请输入中文名称" /></Form.Item>
        <Form.Item label="英文名称" name="nameEn" rules={[{ required: true, message: '请输入英文名称' }]}><Input placeholder="请输入英文名称" /></Form.Item>
        <Form.Item label="中文应用描述" name="descZh"><Input.TextArea rows={2} placeholder="请输入中文描述" /></Form.Item>
        <Form.Item label="英文应用描述" name="descEn"><Input.TextArea rows={2} placeholder="请输入英文描述" /></Form.Item>
        <Form.Item label="绑定到应用服务" name="eamap"><Select placeholder="请选择应用服务" options={eamapOptions} allowClear /></Form.Item>
      </Form>
    </Modal>
  );
};

export default CreateAppModal;
