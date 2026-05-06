import React from 'react';
import ResourceRegister from '../../../components/ResourceRegister';
import { createCallback, updateCallback, fetchCallbackDetail } from './thunk';
import { PROPERTY_PRESETS } from '../../../utils/constants';

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
      propertyPresets={PROPERTY_PRESETS}
      mode={mode}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
}

export default CallbackRegister;
