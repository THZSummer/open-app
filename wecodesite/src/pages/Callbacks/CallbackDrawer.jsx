import React from 'react';
import ResourceDrawer from '../../components/ResourceDrawer';
import { fetchCallbackCategories, fetchCallbacks } from './thunk';
import { getCallbackDrawerColumns } from './constants';
import './CallbackDrawer.m.less';

function CallbackDrawer({ open, onClose, onConfirm, selectedCallbacks = [], subscribeLoading = false, appId }) {
  return (
    <ResourceDrawer
      open={open}
      onClose={onClose}
      onConfirm={onConfirm}
      selectedItems={selectedCallbacks}
      subscribeLoading={subscribeLoading}
      appId={appId}
      title="添加回调"
      className="callback-drawer"
      placeholder="回调名称/Scope"
      fetchCategories={fetchCallbackCategories}
      fetchData={fetchCallbacks}
      getColumns={getCallbackDrawerColumns}
    />
  );
}

export default CallbackDrawer;
