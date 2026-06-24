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
    role: null,
    roleLoading: false,
  },
  reducers: {
    clearRole(state) {
      state.role = null;
      state.roleLoading = false;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchRole.pending, (state) => {
        state.roleLoading = true;
      })
      .addCase(fetchRole.fulfilled, (state, action) => {
        state.role = action.payload;
        state.roleLoading = false;
      })
      .addCase(fetchRole.rejected, (state) => {
        state.role = null;
        state.roleLoading = false;
      });
  },
});

export const { clearRole } = roleSlice.actions;
export default roleSlice.reducer;
