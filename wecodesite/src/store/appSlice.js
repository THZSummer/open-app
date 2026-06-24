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
    appDetail: null,
    appDetailLoading: false,
  },
  reducers: {
    clearAppDetail(state) {
      state.appDetail = null;
      state.appDetailLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchAppDetail.pending, (state) => {
        state.appDetailLoading = true;
      })
      .addCase(fetchAppDetail.fulfilled, (state, action) => {
        state.appDetail = action.payload;
        state.appDetailLoading = false;
      })
      .addCase(fetchAppDetail.rejected, (state) => {
        state.appDetail = null;
        state.appDetailLoading = false;
      });
  },
});

export const { clearAppDetail } = appSlice.actions;
export default appSlice.reducer;
