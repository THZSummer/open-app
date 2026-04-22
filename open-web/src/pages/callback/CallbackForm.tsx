import { useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space, Card } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useCallbackManager } from '@/hooks';
import { Callback, CreateCallbackParams } from '@/services/callback.service';
import { Category } from '@/services/category.service';

const { Option } = Select;

interface CallbackFormProps {
  visible: boolean;
  callback: Callback | null;
  categoryTree: Category[];
  onSuccess: () => void;
  onCancel: () => void;
}

const CallbackForm: React.FC<CallbackFormProps> = ({ visible, callback, categoryTree, onSuccess, onCancel }) => {
  const [form] = Form.useForm();
  const { loading, handleCreateCallback, handleUpdateCallback } = useCallbackManager();

  useEffect(() => {
    if (visible && callback) {
      form.setFieldsValue({
        nameCn: callback.nameCn,
        nameEn: callback.nameEn,
        categoryId: callback.categoryId,
        permissionNameCn: callback.permission?.nameCn,
        permissionNameEn: callback.permission?.nameEn,
        scope: callback.permission?.scope,
        properties: callback.properties || [],
      });
    } else {
      form.resetFields();
    }
  }, [visible, callback, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const data: CreateCallbackParams = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: values.properties || [],
      };

      let result;
      if (callback?.id) {
        result = await handleUpdateCallback(callback.id, data);
      } else {
        result = await handleCreateCallback(data);
      }

      if (result) {
        onSuccess();
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    }
  };

  const flattenCategories = (categories: Category[], level = 0): Category[] => {
    return categories.reduce((acc: Category[], cat: Category) => {
      const prefix = '—'.repeat(level);
      acc.push({ ...cat, nameCn: `${prefix}${prefix ? ' ' : ''}${cat.nameCn}` });
      if (cat.children && cat.children.length > 0) {
        acc.push(...flattenCategories(cat.children, level + 1));
      }
      return acc;
    }, []);
  };

  return (
    <Modal title={callback?.id ? '编辑回调' : '注册回调'} open={visible} onOk={handleSubmit} onCancel={onCancel} width={800} confirmLoading={loading} destroyOnClose>
      <Form form={form} layout="vertical">
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item label="回调名称（中文）" name="nameCn" rules={[{ required: true, message: '请输入回调中文名称' }]}>
            <Input placeholder="请输入回调中文名称" />
          </Form.Item>
          <Form.Item label="回调名称（英文）" name="nameEn" rules={[{ required: true, message: '请输入回调英文名称' }]}>
            <Input placeholder="请输入回调英文名称" />
          </Form.Item>
          <Form.Item label="所属分类" name="categoryId" rules={[{ required: true, message: '请选择所属分类' }]}>
            <Select placeholder="请选择所属分类">
              {flattenCategories(categoryTree).map((cat) => (
                <Option key={cat.id} value={cat.id}>{cat.nameCn}</Option>
              ))}
            </Select>
          </Form.Item>
        </Card>
        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item label="权限名称（中文）" name="permissionNameCn" rules={[{ required: true, message: '请输入权限中文名称' }]}>
            <Input placeholder="请输入权限中文名称" />
          </Form.Item>
          <Form.Item label="权限名称（英文）" name="permissionNameEn" rules={[{ required: true, message: '请输入权限英文名称' }]}>
            <Input placeholder="请输入权限英文名称" />
          </Form.Item>
          <Form.Item label="Scope 标识" name="scope" rules={[{ required: true, message: '请输入 Scope 标识' }, { pattern: /^callback:[a-z0-9_-]+:[a-z0-9_-]+$/, message: 'Scope 格式不正确，示例：callback:approval:completed' }]} extra="格式：callback:{模块}:{资源标识}，例如：callback:approval:completed">
            <Input placeholder="callback:approval:completed" disabled={!!callback?.id} />
          </Form.Item>
        </Card>
        <Card title="扩展属性" size="small">
          <Form.List name="properties">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item {...restField} name={[name, 'propertyName']} rules={[{ required: true, message: '请输入属性名' }]}>
                      <Input placeholder="属性名" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item {...restField} name={[name, 'propertyValue']} rules={[{ required: true, message: '请输入属性值' }]}>
                      <Input placeholder="属性值" style={{ width: 300 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Form.Item>
                  <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>添加属性</Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
};

export default CallbackForm;
