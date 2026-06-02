import React from 'react';
import { Drawer, Form, Input, Button } from 'antd';
import less from './ItemDetailDrawer.module.less';
import { FORM_VALIDATION_RULES } from '../constant';

const { TextArea } = Input;

/**
 * LookUp项详情抽屉组件
 * 提供查看和编辑项功能
 */
const ItemDetailDrawer = ({
  open,
  mode,
  title,
  loading,
  currentItem,
  classifyName,
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
                { label: '所属分类', value: classifyName },
                { label: '项编码', value: currentItem.itemCode },
                { label: '项名称', value: currentItem.itemName },
                { label: '项值', value: currentItem.itemValue },
                { label: '描述', value: currentItem.itemDesc },
                { label: '排序', value: currentItem.itemIndex }
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
            <div className={less.detailCardTitle}>⚙️ 扩展属性</div>
            <div className={less.detailCardBody}>
              {renderDetailRows([
                { label: '属性1', value: currentItem.itemAttr1 },
                { label: '属性2', value: currentItem.itemAttr2 },
                { label: '属性3', value: currentItem.itemAttr3 },
                { label: '属性4', value: currentItem.itemAttr4 },
                { label: '属性5', value: currentItem.itemAttr5 },
                { label: '属性6', value: currentItem.itemAttr6 }
              ])}
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
                name="itemCode"
                label="项编码"
                rules={FORM_VALIDATION_RULES.itemCode}
              >
                <Input placeholder="如: ADMIN" disabled={!!currentItem} />
              </Form.Item>
            </div>
            <div className={less.editFormItem}>
              <Form.Item
                name="itemName"
                label="项名称"
                rules={FORM_VALIDATION_RULES.itemName}
              >
                <Input placeholder="如: 管理员" />
              </Form.Item>
            </div>
          </div>
          <div className={less.editFormRow}>
            <div className={less.editFormItem}>
              <Form.Item
                name="itemValue"
                label="项值"
                rules={FORM_VALIDATION_RULES.itemValue}
              >
                <Input placeholder="如: 1" />
              </Form.Item>
            </div>
            <div className={less.editFormItem}>
              <Form.Item
                name="itemIndex"
                label="排序"
                rules={FORM_VALIDATION_RULES.itemIndex}
                initialValue={1}
              >
                <Input placeholder="正整数，数字越小越靠前" />
              </Form.Item>
            </div>
          </div>
          <Form.Item
            name="itemDesc"
            label="描述"
            rules={FORM_VALIDATION_RULES.itemDesc}
          >
            <TextArea rows={3} placeholder="请输入描述..." showCount maxLength={4000} />
          </Form.Item>
          <div className={less.extendedAttrsDivider}>
            <span className={less.extendedAttrsLabel}>
              扩展属性
            </span>
          </div>
          <Form.Item name="itemAttr1" label="属性1" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性1" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
          <Form.Item name="itemAttr2" label="属性2" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性2" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
          <Form.Item name="itemAttr3" label="属性3" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性3" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
          <Form.Item name="itemAttr4" label="属性4" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性4" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
          <Form.Item name="itemAttr5" label="属性5" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性5" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
          <Form.Item name="itemAttr6" label="属性6" rules={FORM_VALIDATION_RULES.itemAttr}>
            <TextArea rows={2} placeholder="扩展属性6" className={less.extendedAttrTextarea} showCount maxLength={4000} />
          </Form.Item>
        </Form>
      )}
    </Drawer>
  );
};

export default ItemDetailDrawer;
