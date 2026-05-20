import React from 'react';
import ResourceDrawer from '../../components/ResourceDrawer/ResourceDrawer';
import { fetchCallbackCategories, fetchCallbacks } from './thunk';
import './CallbackDrawer.m.less';

function CallbackDrawer({ open, onClose, onConfirm, selectedCallbacks = [], subscribeLoading = false, appId }) {
  return (
    <ResourceDrawer
      open={open}
      appId={appId}
      title="添加回调"
      className="callback-drawer"
      onClose={onClose}
      onConfirm={onConfirm}
      selectedItems={selectedCallbacks}
      subscribeLoading={subscribeLoading}
      fetchCategories={fetchCallbackCategories}
      fetchData={fetchCallbacks}
      columnType='callback'
    />
  );
}

export default CallbackDrawer;
