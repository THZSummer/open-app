import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Card, Button, Form, Input, message, Upload, Modal } from 'antd';
import { EditOutlined, EyeOutlined, CloseOutlined, PlusOutlined } from '@ant-design/icons';
import { fetchAppDetail } from '../../store/appSlice';
import IconPicker from '../../components/IconPicker/IconPicker';
import { updateApp, uploadImage } from './thunk';
import './BasicInfoCard.m.less';
import { validateFile, validateImageDimensions, UPLOAD_IMAGE_TYPE } from '../../utils/common';
import { FILE_VALIDATION } from '../../utils/constants';
import { changeAppBaseInfo } from '../../../routes-redBlue/utils/common'

function BasicInfoCard() {
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId');
  const dispatch = useDispatch();
  // 从 Redux store 中获取应用基本信息，选择器必须为函数
  const { appBaseInfo: appData } = useSelector(state => state.app);
  const [editing, setEditing] = useState(false);
  const [editForm] = Form.useForm();
  const [diagram, setDiagram] = useState(null);
  const [iconId, setIconId] = useState('');
  const [iconUrl, setIconUrl] = useState('');
  const [diagramPreviewVisible, setDiagramPreviewVisible] = useState(false);
  const [exampleVisible, setExampleVisible] = useState(false);
  const [iconPreviewVisible, setIconPreviewVisible] = useState(false);

  useEffect(() => {
    if (!appData) return;
    setDiagram(appData.diagramIdList?.[0] || null);
    const savedIconId = appData.icon?.fileId || appData.iconId || '';
    setIconId(savedIconId);
    setIconUrl(appData.icon?.url || '');
    editForm.setFieldsValue({
      nameCn: appData.nameCn,
      nameEn: appData.nameEn,
      descCn: appData.descCn,
      descEn: appData.descEn,
    });
  }, []);

  const handleSave = async () => {
    try {
      const values = await editForm.validateFields();
      const finalIconId = iconId || appData.icon?.fileId || '';
      if (!finalIconId) {
        message.warning('请选择应用图标');
        return;
      }
      const payload = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        descCn: values.descCn || '',
        descEn: values.descEn || '',
        iconId: finalIconId,
        diagramIdList: diagram ? [diagram.fileId] : [],
      };
      const result = await updateApp(appId, payload);
      if (result?.code === '200') {
        message.success('保存成功');
        setEditing(false);

        const updatedData = {
          ...appData,
          nameCn: values.nameCn,
          nameEn: values.nameEn,
          descCn: values.descCn || '',
          descEn: values.descEn || '',
          icon: {
            fileId: finalIconId,
            url: iconUrl || appData.icon?.url || '',
          },
          iconId: finalIconId,
          diagramIdList: diagram ? [diagram] : [],
        };
        dispatch(fetchAppDetail(appId));
      } else {
        message.error(result?.messageZh || '保存失败');
      }
    } catch (error) {
      message.error('保存失败');
    }
  };

  const handleCancel = () => {
    setEditing(false);
    if (appData) {
      setDiagram(appData.diagramIdList?.[0] || null);
      setIconId(appData.icon?.fileId || appData.iconId || '');
      setIconUrl(appData.icon?.url || '');
      editForm.setFieldsValue({
        nameCn: appData.nameCn,
        nameEn: appData.nameEn,
        descCn: appData.descCn,
        descEn: appData.descEn,
      });
    }
  };

  const handleIconChange = (fileId, url) => {
    setIconId(fileId);
    setIconUrl(url);
  };

  const handleDiagramUpload = async (file) => {
    const fileValidation = validateFile(file, FILE_VALIDATION.diagram);
    if (!fileValidation.valid) {
      message.error(fileValidation.message);
      return false;
    }
    const dimensionValidation = await validateImageDimensions(file, FILE_VALIDATION.diagram.width, FILE_VALIDATION.diagram.height);
    if (!dimensionValidation.valid) {
      message.error(dimensionValidation.message || FILE_VALIDATION.diagram.dimensionMessage);
      return false;
    }
    const formData = new FormData();
    formData.append('file', file);
    const result = await uploadImage(UPLOAD_IMAGE_TYPE.diagram, formData);
    if (result?.code === '200' && result.data) {
      setDiagram({ fileId: result.data.fileId, url: result.data.url });
      message.success('示意图上传成功');
    } else {
      message.error(result.messageZh || '示意图上传失败');
    }
    return false;
  };

  const handleDeleteDiagram = () => {
    setDiagram(null);
  };

  const validateSpace = (value, message) => {
    if (value && (value.startsWith(" ") || value.endsWith(" "))) {
      return Promise.reject(message);
    } 
    return Promise.resolve();
  }

  return (
    <>
      <Card
        className="info-card"
        title={
          <span className="card-title-with-action">
            基本信息
            {!editing && (
              <Button type="link" icon={<EditOutlined />} onClick={() => setEditing(true)}>
                编辑
              </Button>
            )}
          </span>
        }
      >
        {editing ? (
          <Form form={editForm} layout="vertical" className="basic-info-edit-form">
            <div className="edit-form-row">
              <div className="edit-form-label"><span className="edit-form-required">*</span> 应用图标</div>
              <div className="edit-form-field">
                <IconPicker
                  value={iconId}
                  uploadedUrl={iconUrl}
                  onChange={handleIconChange}
                />
              </div>
            </div>

            <div className="edit-form-row">
              <div className="edit-form-label"><span className="edit-form-required">*</span> 中文名称</div>
              <div className="edit-form-field">
                <Form.Item 
                  name="nameCn" 
                  rules={[
                    { required: true, message: "应用中文名不能为空" },
                    { max: 255, message: "不超过255字符" },
                    { validator: (_, value) => validateSpace(value, '前后不能有空格')},
                  ]}
                  style={{ marginBottom: 0 }}
                >
                  <Input maxLength={255} showCount />
                </Form.Item>
              </div>
            </div>

            <div className="edit-form-row">
              <div className="edit-form-label"><span className="edit-form-required">*</span> 英文名称</div>
              <div className="edit-form-field">
                <Form.Item 
                  name="nameEn" rules={[
                  { required: true, message: "应用英文名不能为空" },
                  { max: 255, message: "不超过255字符" },
                  { validator: (_, value) => validateSpace(value, '前后不能有空格')},
                  ]}
                  style={{ marginBottom: 0 }}
                >
                  <Input maxLength={255} showCount />
                </Form.Item>
              </div>
            </div>

            <div className="edit-form-row">
              <div className="edit-form-label">中文描述</div>
              <div className="edit-form-field">
                <Form.Item name="descCn" rules={[{ max: 2000, message: '描述不超过2000字符' }]} style={{ marginBottom: 0 }}>
                  <Input.TextArea rows={3} maxLength={2000} showCount />
                </Form.Item>
              </div>
            </div>

            <div className="edit-form-row">
              <div className="edit-form-label">英文描述</div>
              <div className="edit-form-field">
                <Form.Item name="descEn" rules={[{ max: 2000, message: '描述不超过2000字符' }]} style={{ marginBottom: 0 }}>
                  <Input.TextArea rows={3} maxLength={2000} showCount />
                </Form.Item>
              </div>
            </div>

            <div className="edit-form-row">
              <div className="edit-form-label">功能示意图</div>
              <div className="edit-form-field">
                <div className="diagram-list">
                  {diagram ? (
                    <div className="diagram-item has-image">
                      <img src={diagram.url} alt="功能示意图" className="diagram-thumb" />
                      <div className="diagram-overlay">
                        <span className="diagram-action" onClick={() => setDiagramPreviewVisible(true)} title="预览">
                          <EyeOutlined />
                        </span>
                        <span className="diagram-action" onClick={handleDeleteDiagram} title="删除">
                          <CloseOutlined />
                        </span>
                      </div>
                    </div>
                  ) : (
                    <Upload
                      showUploadList={false}
                      beforeUpload={() => false}
                      accept="image/png,image/jpeg,image/jpg"
                      onChange={({ file }) => handleDiagramUpload(file.originFileObj || file)}
                    >
                      <div className="diagram-add">
                        <PlusOutlined />
                        <span>添加示意图</span>
                      </div>
                    </Upload>
                  )}
                </div>
                <div className="diagram-tip">自定义上传：图片尺寸360px*200px, 推荐使用2倍图720px*400px,PNG、JPG、JEPG格式,小于500KB,<a className="diagram-example-link" onClick={() => setExampleVisible(true)}>查看示例</a></div>
              </div>
            </div>

            <div className="edit-form-row card-actions-row">
              <div className="edit-form-label"></div>
              <div className="edit-form-field">
                <div className="card-actions">
                  <Button onClick={handleCancel}>取消</Button>
                  <Button type="primary" onClick={handleSave}>保存</Button>
                </div>
              </div>
            </div>
          </Form>
        ) : (
          <div className="basic-info-view">
            <div className="info-row">
              <span className="info-label">应用图标</span>
              <span className="info-value">
                {appData.icon?.url ? (
                  <div className="info-icon-wrap">
                    <img src={appData.icon.url} alt="icon" className="info-icon-preview" />
                    <div className="info-icon-overlay" onClick={() => setIconPreviewVisible(true)} title="预览">
                      <EyeOutlined />
                    </div>
                  </div>
                ) : '-'}
              </span>
            </div>
            <div className="info-row">
              <span className="info-label">中文名称</span>
              <span className="info-value">{appData.nameCn}</span>
            </div>
            <div className="info-row">
              <span className="info-label">英文名称</span>
              <span className="info-value">{appData.nameEn}</span>
            </div>
            <div className="info-row">
              <span className="info-label">中文描述</span>
              <span className="info-value">{appData.descCn || '-'}</span>
            </div>
            <div className="info-row">
              <span className="info-label">英文描述</span>
              <span className="info-value">{appData.descEn || '-'}</span>
            </div>
            <div className="info-row">
              <span className="info-label">功能示意图</span>
              <div className="info-value diagram-view-list">
                {appData.diagramIdList && appData.diagramIdList.length > 0 ? (
                  <div className="diagram-view-item">
                    <img src={appData.diagramIdList[0].url} alt="功能示意图" className="diagram-view-thumb" />
                    <div className="diagram-overlay">
                      <span className="diagram-action" onClick={() => setDiagramPreviewVisible(true)} title="预览">
                        <EyeOutlined />
                      </span>
                    </div>
                  </div>
                ) : (
                  '-'
                )}
              </div>
            </div>
          </div>
        )}
      </Card>

      <Modal
        title="功能示意图预览"
        open={diagramPreviewVisible}
        onCancel={() => setDiagramPreviewVisible(false)}
        footer={null}
        width={720}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          {(diagram || appData?.diagramIdList?.[0]) ? (
            <img src={(diagram || appData.diagramIdList[0]).url} alt="功能示意图" style={{ maxWidth: '100%', maxHeight: 500, objectFit: 'contain' }} />
          ) : (
            <span style={{ color: '#8f959e' }}>暂无示意图</span>
          )}
        </div>
      </Modal>

      <Modal
        title="功能示意图示例"
        open={exampleVisible}
        onCancel={() => setExampleVisible(false)}
        footer={null}
        width={520}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <div style={{ background: '#f7f8fa', border: '1px dashed #d0d0d0', borderRadius: 8, padding: '40px 20px', color: '#8f959e', fontSize: 14 }}>
            <div style={{ marginBottom: 8, fontSize: 16, color: '#1f2329' }}>功能示意图示例</div>
            <div>建议尺寸：360×200px（推荐2倍图 720×400px）</div>
            <div style={{ marginTop: 8 }}>支持 PNG/JPG/JPEG 格式，不超过 500KB</div>
            <div style={{ marginTop: 16, background: '#e8e8e8', height: 120, borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <span style={{ color: '#bbb' }}>360 × 200</span>
            </div>
          </div>
        </div>
      </Modal>

      <Modal
        title="应用图标预览"
        open={iconPreviewVisible}
        onCancel={() => setIconPreviewVisible(false)}
        footer={null}
        width={400}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          {appData?.icon?.url ? (
            <img src={appData.icon.url} alt="应用图标" style={{ maxWidth: '100%', maxHeight: 300, objectFit: 'contain' }} />
          ) : (
            <span style={{ color: '#8f959e' }}>暂无图标</span>
          )}
        </div>
      </Modal>
    </>
  );
}

export default BasicInfoCard;
