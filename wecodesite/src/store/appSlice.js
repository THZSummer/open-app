import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { fetchAppById } from '../pages/AppList/thunk';

export const fetchAppDetail = createAsyncThunk(
  'app/fetchAppDetail',
  async (appId) => {
    const result = await fetchAppById(appId);
    if (result?.code === '200' && result.data) {
      return result.data;
    }
    return null;
  }
);

const appSlice = createSlice({
  name: 'app',
  initialState: {
    appBaseInfo: null,
    appBaseInfoLoading: false,
  },
  reducers: {
    clearAppDetail(state) {
      state.appBaseInfo = null;
      state.appBaseInfoLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAppDetail.pending, (state) => {
        state.appBaseInfoLoading = true;
      })
      .addCase(fetchAppDetail.fulfilled, (state, action) => {
        state.appBaseInfo = action.payload;
        state.appBaseInfoLoading = false;
      })
      .addCase(fetchAppDetail.rejected, (state) => {
        state.appBaseInfo = null;
        state.appBaseInfoLoading = false;
      });
  },
});

export const { clearAppDetail } = appSlice.actions;
export default appSlice.reducer;
