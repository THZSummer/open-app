import React from 'react';
import ResourceRegister from '../../../components/ResourceRegister/ResourceRegister';
import { createEvent, updateEvent, fetchEventDetail } from './thunk';
import { PROPERTY_PRESETS } from '../../../utils/constants';

function EventRegister({ visible, event, mode = 'create', onSuccess, onCancel }) {
  return (
    <ResourceRegister
      visible={visible}
      resource={event}
      resourceType="event"
      thunk={{
        fetchDetail: fetchEventDetail,
        create: createEvent,
        update: updateEvent,
      }}
      propertyPresets={PROPERTY_PRESETS}
      mode={mode}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
}

export default EventRegister;
