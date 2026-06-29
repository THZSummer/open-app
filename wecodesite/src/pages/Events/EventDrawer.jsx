import React from 'react';
import ResourceDrawer from '../../components/ResourceDrawer/ResourceDrawer';
import { fetchEventCategories, fetchEvents } from './thunk';
import './EventDrawer.m.less';

function EventDrawer({ open, onClose, onConfirm, selectedEvents = [], subscribeLoading = false, appId }) {
  return (
    <ResourceDrawer
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      selectedItems={selectedEvents}
      subscribeLoading={subscribeLoading}
      appId={appId}
      title="添加事件"
      className="event-drawer"
      fetchCategories={fetchEventCategories}
      fetchData={fetchEvents}
      columnType='event'
    />
  );
}

export default EventDrawer;
