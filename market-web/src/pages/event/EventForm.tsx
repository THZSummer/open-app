import { useEffect } from 'react';
import { Modal, Form, Input, Select, Button, Space, Card } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { useEvent } from '@/hooks';
import { Event, CreateEventParams } from '@/services/event.service';
import { Category } from '@/services/category.service';

const { Option } = Select;

interface EventFormProps {
  visible: boolean;
  event: Event | null;
  categoryTree: Category[];
  onSuccess: () => void;
  onCancel: () => void;
}

const EventForm: React.FC<EventFormProps> = ({ visible, event, categoryTree, onSuccess, onCancel }) => {
  const [form] = Form.useForm();
  const { loading, handleCreateEvent, handleUpdateEvent } = useEvent();

  useEffect(() => {
    if (visible && event) {
      form.setFieldsValue({
        nameCn: event.nameCn,
        nameEn: event.nameEn,
        topic: event.topic,
        categoryId: event.categoryId,
        permissionNameCn: event.permission?.nameCn,
        permissionNameEn: event.permission?.nameEn,
        scope: event.permission?.scope,
        properties: event.properties || [],
      });
    } else {
      form.resetFields();
    }
  }, [visible, event, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const data: CreateEventParams = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        topic: values.topic,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: values.properties || [],
      };

      let result;
      if (event?.id) {
        result = await handleUpdateEvent(event.id, data);
      } else {
        result = await handleCreateEvent(data);
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
    <Modal title={event?.id ? '编辑事件' : '注册事件'} open={visible} onOk={handleSubmit} onCancel={onCancel} width={800} confirmLoading={loading} destroyOnClose>
      <Form form={form} layout="vertical">
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item label="事件名称（中文）" name="nameCn" rules={[{ required: true, message: '请输入事件中文名称' }]}>
            <Input placeholder="请输入事件中文名称" />
          </Form.Item>
          <Form.Item label="事件名称（英文）" name="nameEn" rules={[{ required: true, message: '请输入事件英文名称' }]}>
            <Input placeholder="请输入事件英文名称" />
          </Form.Item>
          <Form.Item label="所属分类" name="categoryId" rules={[{ required: true, message: '请选择所属分类' }]}>
            <Select placeholder="请选择所属分类">
              {flattenCategories(categoryTree).map((cat) => (
                <Option key={cat.id} value={cat.id}>{cat.nameCn}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item label="Topic" name="topic" rules={[{ required: true, message: '请输入 Topic' }, { pattern: /^[a-z0-9_.-]+$/, message: 'Topic 格式不正确，只能包含小写字母、数字、下划线、点和短横线' }]} extra="Topic 全局唯一，示例：im.message.received">
            <Input placeholder="im.message.received" disabled={!!event?.id} />
          </Form.Item>
        </Card>
        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item label="权限名称（中文）" name="permissionNameCn" rules={[{ required: true, message: '请输入权限中文名称' }]}>
            <Input placeholder="请输入权限中文名称" />
          </Form.Item>
          <Form.Item label="权限名称（英文）" name="permissionNameEn" rules={[{ required: true, message: '请输入权限英文名称' }]}>
            <Input placeholder="请输入权限英文名称" />
          </Form.Item>
          <Form.Item label="Scope 标识" name="scope" rules={[{ required: true, message: '请输入 Scope 标识' }, { pattern: /^event:[a-z0-9_-]+:[a-z0-9_-]+$/, message: 'Scope 格式不正确，示例：event:im:message-received' }]} extra="格式：event:{模块}:{资源标识}，例如：event:im:message-received">
            <Input placeholder="event:im:message-received" disabled={!!event?.id} />
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

export default EventForm;
