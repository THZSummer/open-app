import { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Space,
  Card,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useApi } from '@/hooks';
import { Api, CreateApiParams, UpdateApiParams } from '@/services/api.service';
import { Category } from '@/services/category.service';

const { Option } = Select;

interface ApiFormProps {
  visible: boolean;
  api: Api | null;
  categoryTree: Category[];
  onSuccess: () => void;
  onCancel: () => void;
}

/**
 * API 注册/编辑表单
 */
const ApiForm: React.FC<ApiFormProps> = ({
  visible,
  api,
  categoryTree,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const { handleCreateApi, handleUpdateApi } = useApi();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible && api) {
      // 编辑模式，填充表单
      form.setFieldsValue({
        nameCn: api.nameCn,
        nameEn: api.nameEn,
        path: api.path,
        method: api.method,
        categoryId: api.categoryId,
        permissionNameCn: api.permission?.nameCn,
        permissionNameEn: api.permission?.nameEn,
        scope: api.permission?.scope,
        properties: api.properties || [],
      });
    } else {
      // 新建模式，重置表单
      form.resetFields();
    }
  }, [visible, api, form]);

  /**
   * 提交表单
   */
  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const data: CreateApiParams | UpdateApiParams = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        path: values.path,
        method: values.method,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: values.properties || [],
      };

      let result;
      if (api?.id) {
        // 编辑
        result = await handleUpdateApi(api.id, data);
      } else {
        // 新建
        result = await handleCreateApi(data as CreateApiParams);
      }

      if (result) {
        onSuccess();
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * 分类树扁平化（用于下拉选择）
   */
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
    <Modal
      title={api?.id ? '编辑 API' : '注册 API'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      width={800}
      confirmLoading={submitting}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        {/* 基本信息 */}
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label="API 名称（中文）"
            name="nameCn"
            rules={[{ required: true, message: '请输入 API 中文名称' }]}
          >
            <Input placeholder="请输入 API 中文名称" />
          </Form.Item>

          <Form.Item
            label="API 名称（英文）"
            name="nameEn"
            rules={[{ required: true, message: '请输入 API 英文名称' }]}
          >
            <Input placeholder="请输入 API 英文名称" />
          </Form.Item>

          <Form.Item
            label="所属分类"
            name="categoryId"
            rules={[{ required: true, message: '请选择所属分类' }]}
          >
            <Select placeholder="请选择所属分类">
              {flattenCategories(categoryTree).map((cat) => (
                <Option key={cat.id} value={cat.id}>
                  {cat.nameCn}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            label="API 路径"
            name="path"
            rules={[
              { required: true, message: '请输入 API 路径' },
              { pattern: /^\//, message: '路径必须以 / 开头' },
            ]}
          >
            <Input placeholder="例如：/api/v1/messages" />
          </Form.Item>

          <Form.Item
            label="HTTP 方法"
            name="method"
            rules={[{ required: true, message: '请选择 HTTP 方法' }]}
          >
            <Select placeholder="请选择 HTTP 方法">
              <Option value="GET">GET</Option>
              <Option value="POST">POST</Option>
              <Option value="PUT">PUT</Option>
              <Option value="DELETE">DELETE</Option>
              <Option value="PATCH">PATCH</Option>
            </Select>
          </Form.Item>
        </Card>

        {/* 权限信息 */}
        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label="权限名称（中文）"
            name="permissionNameCn"
            rules={[{ required: true, message: '请输入权限中文名称' }]}
          >
            <Input placeholder="请输入权限中文名称" />
          </Form.Item>

          <Form.Item
            label="权限名称（英文）"
            name="permissionNameEn"
            rules={[{ required: true, message: '请输入权限英文名称' }]}
          >
            <Input placeholder="请输入权限英文名称" />
          </Form.Item>

          <Form.Item
            label="Scope 标识"
            name="scope"
            rules={[
              { required: true, message: '请输入 Scope 标识' },
              {
                pattern: /^api:[a-z0-9_-]+:[a-z0-9_-]+$/,
                message: 'Scope 格式不正确，示例：api:im:send-message',
              },
            ]}
            extra="格式：api:{模块}:{资源标识}，例如：api:im:send-message"
          >
            <Input placeholder="api:im:send-message" disabled={!!api?.id} />
          </Form.Item>
        </Card>

        {/* 扩展属性 */}
        <Card title="扩展属性" size="small">
          <Form.List name="properties">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'propertyName']}
                      rules={[{ required: true, message: '请输入属性名' }]}
                    >
                      <Input placeholder="属性名，如：descriptionCn" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'propertyValue']}
                      rules={[{ required: true, message: '请输入属性值' }]}
                    >
                      <Input placeholder="属性值" style={{ width: 300 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Form.Item>
                  <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                    添加属性
                  </Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
};

export default ApiForm;
