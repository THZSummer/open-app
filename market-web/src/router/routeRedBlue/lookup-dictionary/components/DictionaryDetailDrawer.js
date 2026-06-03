import React from 'react';
import { Drawer, Form, Input, Button } from 'antd';
import less from './DictionaryDetailDrawer.module.less';
import { FORM_VALIDATION_RULES } from '../constant';

const { TextArea } = Input;

/**
 * 数据字典详情抽屉组件
 * 提供查看和编辑字典功能
 */
const DictionaryDetailDrawer = ({
  open,
  mode,
  title,
  loading,
  currentItem,
  onClose,
  onSubmit,
  detailForm
}) => {
  const isView = mode === 'view';
  
  /**
   * 渲染详情行
   * @param {Array} fields - 字段配置数组
   * @returns {JSX.Element[]} 渲染后的详情行元素
   */
  const renderDetailRows = (fields) => {
    return fields.map((field, index) => (
      <div className={less.detailRow} key={index}>
        <span className={less.detailLabel}>{field.label}</span>
        <span className={less.detailValue}>{field.value}</span>
      </div>
    ));
  };
  
  /**
   * 抽屉关闭处理
   */
  const handleClose = () => {
    detailForm?.resetFields();
    onClose?.();
  };
  
  /**
   * 表单提交处理
   */
  const handleSubmit = () => {
    detailForm.validateFields().then((values) => {
      onSubmit?.(values);
    });
  };
  
  return (
    <Drawer
      title={title}
      width={520}
      onClose={handleClose}
      open={open}
      destroyOnClose
      footer={
        <div className={less.drawerFooter}>
          {isView ? (
            <Button onClick={handleClose}>关闭</Button>
          ) : (
            <>
              <Button onClick={handleClose}>取消</Button>
              <Button type="primary" onClick={handleSubmit} loading={loading}>
                保存
              </Button>
            </>
          )}
        </div>
      }
    >
      {isView && currentItem ? (
        <div className={less.detailPanelBody}>
          <div className={less.detailCard}>
            <div className={less.detailCardTitle}>📋 基本信息</div>
            <div className={less.detailCardBody}>
              {renderDetailRows([
                { label: '编码', value: currentItem.code },
                { label: '名称', value: currentItem.name },
                { label: '值', value: currentItem.value },
                { label: '路径', value: currentItem.path },
                { label: '描述', value: currentItem.description }
              ])}
              <div className={less.detailRow}>
                <span className={less.detailLabel}>状态</span>
                <span className={less.detailValue}>
                  <span className={less.statusBadge}>
                    <span className={`${less.dot} ${currentItem.status === 1 ? less.dotActive : less.dotInactive}`} />
                    <span className={currentItem.status === 1 ? less.labelActive : less.labelInactive}>
                      {currentItem.status === 1 ? '有效' : '失效'}
                    </span>
                  </span>
                </span>
              </div>
            </div>
          </div>
          <div className={less.detailCard}>
            <div className={less.detailCardTitle}>📝 系统信息</div>
            <div className={less.detailCardBody}>
              {renderDetailRows([
                { label: '创建人', value: currentItem.createBy },
                { label: '创建时间', value: currentItem.createTime },
                { label: '修改人', value: currentItem.lastUpdateBy },
                { label: '修改时间', value: currentItem.lastUpdateTime }
              ])}
            </div>
          </div>
        </div>
      ) : (
        <Form 
          form={detailForm} 
          layout="vertical" 
          className={less.editForm}
        >
          <div className={less.editFormRow}>
            <div className={less.editFormItem}>
              <Form.Item
                name="code"
                label="编码"
                rules={FORM_VALIDATION_RULES.code}
              >
                <Input placeholder="如: USER_STATUS" disabled={!!currentItem} />
              </Form.Item>
            </div>
            <div className={less.editFormItem}>
              <Form.Item
                name="name"
                label="名称"
                rules={FORM_VALIDATION_RULES.name}
              >
                <Input placeholder="如: 用户状态" />
              </Form.Item>
            </div>
          </div>
          <div className={less.editFormRow}>
            <div className={less.editFormItem}>
              <Form.Item
                name="value"
                label="值"
                rules={FORM_VALIDATION_RULES.value}
              >
                <Input placeholder="如: active" />
              </Form.Item>
            </div>
            <div className={less.editFormItem}>
              <Form.Item
                name="path"
                label="路径"
                rules={FORM_VALIDATION_RULES.path}
              >
                <Input placeholder="如: system/user" />
              </Form.Item>
            </div>
          </div>
          <Form.Item
            name="description"
            label="描述"
            rules={FORM_VALIDATION_RULES.description}
          >
            <TextArea rows={3} placeholder="请输入描述..." showCount maxLength={4000} />
          </Form.Item>
        </Form>
      )}
    </Drawer>
  );
};

export default DictionaryDetailDrawer;
