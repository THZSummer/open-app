import { configureStore } from '@reduxjs/toolkit';
import appReducer from './appSlice';
import roleReducer from './roleSlice';

const store = configureStore({
  reducer: {
    app: appReducer,
    role: roleReducer,
  },
});

export default store;
