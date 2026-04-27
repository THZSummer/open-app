import React, { useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
} from 'antd';

function CategoryFormModal({
  visible,
  isEditing,
  parentCategory,
  initialValues,
  onClose,
  onSubmit,
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);

  useEffect(() => {
    if (visible) {
      if (isEditing && initialValues) {
        form.setFieldsValue({
          categoryAlias: initialValues.categoryAlias,
          nameCn: initialValues.nameCn,
          nameEn: initialValues.nameEn,
          sortOrder: initialValues.sortOrder,
        });
      } else {
        form.resetFields();
        if (parentCategory) {
          form.setFieldsValue({
            parentId: parentCategory.id,
          });
        } else {
          form.setFieldsValue({
            sortOrder: 0,
          });
        }
      }
    }
  }, [visible, isEditing, parentCategory, initialValues, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const data = isEditing
        ? {
            nameCn: values.nameCn,
            nameEn: values.nameEn,
            sortOrder: values.sortOrder,
          }
        : {
            categoryAlias: values.categoryAlias,
            nameCn: values.nameCn,
            nameEn: values.nameEn,
            parentId: values.parentId,
            sortOrder: values.sortOrder,
          };

      await onSubmit(data);
      form.resetFields();
    } catch (error) {
      console.error('Validation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={isEditing ? '编辑分类' : (parentCategory ? '新增子分类' : '新增一级分类')}
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        {!parentCategory && !isEditing && (
          <Form.Item
            label="分类别名"
            name="categoryAlias"
            tooltip="一级分类需设置别名，用于区分不同权限树"
            rules={[{ required: true, message: '一级分类必须设置别名' }]}
          >
            <Input placeholder="如：app_type_a" disabled={!!parentCategory} />
          </Form.Item>
        )}

        <Form.Item label="父分类ID" name="parentId" hidden>
          <Input disabled />
        </Form.Item>

        <Form.Item
          label="中文名称"
          name="nameCn"
          rules={[{ required: true, message: '请输入中文名称' }]}
        >
          <Input placeholder="请输入中文名称" />
        </Form.Item>

        <Form.Item
          label="英文名称"
          name="nameEn"
          rules={[{ required: true, message: '请输入英文名称' }]}
        >
          <Input placeholder="请输入英文名称" />
        </Form.Item>

        <Form.Item label="排序" name="sortOrder" initialValue={0}>
          <InputNumber min={0} placeholder="排序序号" style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default CategoryFormModal;
