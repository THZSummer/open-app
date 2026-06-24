import React, { useEffect } from 'react';
import { Modal, Form, Tooltip } from 'antd';
import { QuestionCircleOutlined } from '@ant-design/icons';
import BindEamapSelect from '../BindEamapSelect/BindEamapSelect';

/**
 * 绑定应用服务弹窗
 *
 * @param {Object} props - 组件属性
 * @param {boolean} props.visible - 弹窗是否可见
 * @param {Function} props.onCancel - 取消回调
 * @param {Function} props.onOk - 确认回调
 * @param {string} props.appId - 应用ID
 * @param {Array} props.eamapOptions - EAMAP 选项列表
 * @param {string} props.currentEamap - 当前绑定的 EAMAP
 */
function BindEamapModal(props) {
  const { visible, onCancel, onOk, appId, eamapOptions = [], currentEamap } = props;
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      form.resetFields();
      form.setFieldsValue({ eamap: currentEamap });
    }
  }, [visible, form, currentEamap]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      onOk(values.eamap);
    } catch (error) {
    }
  };

  return (
    <Modal
      title="绑定到应用服务"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确认绑定"
      cancelText="取消"
      width={480}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
      >
        <Form.Item
          name="eamap"
          label={
            <span>
              绑定到应用服务
              <Tooltip
                overlayStyle={{ maxWidth: 'none' }}
                color="#eef4ff"
                title={<span style={{ color: '#1f2329', whiteSpace: 'nowrap' }}>开放平台按照应用服务维度做权限隔离，如需申请API权限等开放能力需要先绑定应用服务，如无权限请前往应用中心查询对应责任人</span>}
              >
                <QuestionCircleOutlined style={{ color: '#8f959e', marginLeft: 4, cursor: 'pointer' }} />
              </Tooltip>
            </span>
          }
          rules={[{ required: true, message: '请选择应用服务' }]}
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

export default BindEamapModal;