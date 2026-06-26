import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { API_CONFIG, fetchApi } from '../configs/web.config';

export const fetchRole = createAsyncThunk(
  'role/fetchRole',
  async (appId) => {
    try {
      const result = await fetchApi(API_CONFIG.APP.CURRENT_ROLE, { params: { appId } });
      if (result?.code === '200' && result.data?.role != null) {
        return result.data.role;
      }
    } catch (err) {
      // ignore
    }
    return null;
  }
);

const roleSlice = createSlice({
  name: 'role',
  initialState: {
    roleType: null,
    roleTypeLoading: true,
  },
  reducers: {
    clearRole(state) {
      state.roleType = null;
      state.roleTypeLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRole.pending, (state) => {
        state.roleTypeLoading = true;
      })
      .addCase(fetchRole.fulfilled, (state, action) => {
        state.roleType = action.payload;
        state.roleTypeLoading = false;
      })
      .addCase(fetchRole.rejected, (state) => {
        state.roleType = null;
        state.roleTypeLoading = false;
      });
  },
});

export const { clearRole } = roleSlice.actions;
export default roleSlice.reducer;
