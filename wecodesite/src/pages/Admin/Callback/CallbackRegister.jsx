import React from 'react';
import ResourceRegister from '../../../components/ResourceRegister';
import { createCallback, updateCallback, fetchCallbackDetail } from './thunk';
import { CALLBACK_PROPERTY_PRESETS } from './constants';

function CallbackRegister({ visible, callback, mode = 'create', onSuccess, onCancel }) {
  return (
    <ResourceRegister
      visible={visible}
      resource={callback}
      resourceType="callback"
      thunk={{
        fetchDetail: fetchCallbackDetail,
        create: createCallback,
        update: updateCallback,
      }}
      propertyPresets={CALLBACK_PROPERTY_PRESETS}
      mode={mode}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
}

export default CallbackRegister;
