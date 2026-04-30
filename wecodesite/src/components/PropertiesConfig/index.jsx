import React from 'react';
import { Form, Input, Select, Button, Space, Card } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';

function PropertiesConfig({ form, mode = 'view', propertyPresets, title = '扩展属性（可选）' }) {
  return (
    <Card title={title} size="small">
      <Form.List name="properties">
        {(fields, { add, remove }) => {
          const formValues = form.getFieldValue('properties') || [];
          const usedPresets = formValues
            .map(item => item?.propertyName)
            .filter(name => name && name !== '__custom__');

          return (
            <>
              {fields.map(({ key, name, ...restField }) => (
                <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                  <Form.Item
                    {...restField}
                    name={[name, 'propertyName']}
                    rules={[{ required: true, message: '请选择或输入属性名' }]}
                  >
                    <Select
                      placeholder="选择属性"
                      style={{ width: 160 }}
                      disabled={mode === 'view'}
                      onChange={() => {
                        const properties = form.getFieldValue('properties');
                        properties[name].propertyValue = undefined;
                        form.setFieldsValue({ properties });
                      }}
                    >
                      {propertyPresets.map(preset => (
                        <Select.Option
                          key={preset.value}
                          value={preset.value}
                          disabled={preset.value !== '__custom__' && usedPresets.includes(preset.value)}
                        >
                          {preset.label}
                        </Select.Option>
                      ))}
                    </Select>
                  </Form.Item>

                  <Form.Item
                    noStyle
                    shouldUpdate={(prev, cur) =>
                      prev.properties?.[name]?.propertyName !== cur.properties?.[name]?.propertyName
                    }
                  >
                    {({ getFieldValue }) => {
                      const propertyName = getFieldValue(['properties', name, 'propertyName']);
                      if (propertyName === '__custom__') {
                        return (
                          <Form.Item
                            {...restField}
                            name={[name, 'customPropertyName']}
                            rules={[{ required: true, message: '请输入自定义属性名' }]}
                          >
                            <Input placeholder="自定义属性名" style={{ width: 140 }} disabled={mode === 'view'} />
                          </Form.Item>
                        );
                      }
                      return null;
                    }}
                  </Form.Item>

                  <Form.Item
                    noStyle
                    shouldUpdate={(prev, cur) =>
                      prev.properties?.[name]?.propertyName !== cur.properties?.[name]?.propertyName
                    }
                  >
                    {({ getFieldValue }) => {
                      const propertyName = getFieldValue(['properties', name, 'propertyName']);
                      const preset = propertyPresets.find(p => p.value === propertyName);
                      const isCustom = propertyName === '__custom__';

                      return (
                        <Form.Item
                          {...restField}
                          name={[name, 'propertyValue']}
                          rules={[{ required: true, message: '请输入属性值' }]}
                        >
                          <Input
                            placeholder={isCustom ? '属性值' : (preset?.placeholder || '属性值')}
                            style={{ width: 260 }}
                            disabled={mode === 'view'}
                          />
                        </Form.Item>
                      );
                    }}
                  </Form.Item>

                  {mode !== 'view' && <MinusCircleOutlined onClick={() => remove(name)} />}
                </Space>
              ))}
              {mode !== 'view' && (
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加属性
                </Button>
              )}
            </>
          );
        }}
      </Form.List>
    </Card>
  );
}

export default PropertiesConfig;
