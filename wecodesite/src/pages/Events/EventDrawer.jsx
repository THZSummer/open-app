import React from 'react';
import ResourceDrawer from '../../components/ResourceDrawer';
import { fetchEventCategories, fetchEvents } from './thunk';
import { getEventDrawerColumns } from './constants';
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
      placeholder="事件名称/Topic"
      fetchCategories={fetchEventCategories}
      fetchData={fetchEvents}
      getColumns={getEventDrawerColumns}
    />
  );
}

export default EventDrawer;
