import React from 'react';
import { Form, Input, Button, Space } from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';

function ApprovalNodesConfig({ form, mode = 'view', needApproval }) {
  if (needApproval !== 1) {
    return null;
  }

  return (
    <Form.Item label="审批节点配置" required>
      <Form.List name="resourceNodes">
        {(fields, { add, remove }) => (
          <>
            {fields.map(({ key, name, ...restField }) => (
              <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                <Form.Item
                  {...restField}
                  name={[name, 'userId']}
                  rules={[{ required: true, message: '请输入审批人ID' }]}
                >
                  <Input placeholder="审批人用户ID" style={{ width: 150 }} disabled={mode === 'view'} />
                </Form.Item>
                <Form.Item
                  {...restField}
                  name={[name, 'userName']}
                  rules={[{ required: true, message: '请输入审批人姓名' }]}
                >
                  <Input placeholder="审批人姓名" style={{ width: 150 }} disabled={mode === 'view'} />
                </Form.Item>
                <Form.Item
                  {...restField}
                  name={[name, 'order']}
                  rules={[{ required: true, message: '请输入审批顺序' }]}
                >
                  <Input placeholder="审批顺序" style={{ width: 80 }} disabled={mode === 'view'} />
                </Form.Item>
                {mode !== 'view' && <MinusCircleOutlined onClick={() => remove(name)} />}
              </Space>
            ))}
            {mode !== 'view' && (
              <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                添加审批节点
              </Button>
            )}
          </>
        )}
      </Form.List>
    </Form.Item>
  );
}

export default ApprovalNodesConfig;
