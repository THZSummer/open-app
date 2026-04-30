import React from 'react';
import ResourceRegister from '../../../components/ResourceRegister';
import { createApi, updateApi, fetchApiDetail } from './thunk';
import { API_PROPERTY_PRESETS } from './constants';

const transformPropertiesWithPresets = (properties, presets) => {
  return properties?.map(prop => ({
    propertyName: presets.find(p => p.value === prop.propertyName)
      ? prop.propertyName
      : '__custom__',
    propertyValue: prop.propertyValue,
    customPropertyName: presets.find(p => p.value === prop.propertyName)
      ? undefined
      : prop.propertyName,
  })) || [];
};

function ApiRegister({ visible, api, mode = 'create', onSuccess, onCancel }) {
  return (
    <ResourceRegister
      visible={visible}
      resource={api}
      resourceType="api"
      thunk={{
        fetchDetail: fetchApiDetail,
        create: createApi,
        update: updateApi,
      }}
      propertyPresets={API_PROPERTY_PRESETS}
      transformProperties={transformPropertiesWithPresets}
      mode={mode}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
}

export default ApiRegister;
