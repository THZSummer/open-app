import { useState, useCallback } from 'react';
import { message } from 'antd';
import {
  getCategoryTree,
  getCategoryDetail,
  createCategory,
  updateCategory,
  deleteCategory,
  getCategoryOwners,
  addCategoryOwner,
  removeCategoryOwner,
  Category,
  CategoryOwner,
  CreateCategoryParams,
  UpdateCategoryParams,
  AddOwnerParams,
} from '@/services/category.service';

/**
 * 分类管理 Hook
 */
export const useCategory = () => {
  const [loading, setLoading] = useState(false);
  const [categoryTree, setCategoryTree] = useState<Category[]>([]);
  const [currentCategory, setCurrentCategory] = useState<Category | null>(null);
  const [owners, setOwners] = useState<CategoryOwner[]>([]);

  /**
   * 获取分类树
   */
  const fetchCategoryTree = useCallback(async (categoryAlias?: string) => {
    setLoading(true);
    try {
      const response = await getCategoryTree(categoryAlias);
      setCategoryTree(response.data.data || []);
      return response.data.data;
    } catch (error) {
      console.error('获取分类树失败:', error);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取分类详情
   */
  const fetchCategoryDetail = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await getCategoryDetail(id);
      setCurrentCategory(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取分类详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 创建分类
   */
  const handleCreateCategory = useCallback(async (data: CreateCategoryParams) => {
    setLoading(true);
    try {
      const response = await createCategory(data);
      message.success('分类创建成功');
      return response.data.data;
    } catch (error) {
      console.error('创建分类失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 更新分类
   */
  const handleUpdateCategory = useCallback(async (id: string, data: UpdateCategoryParams) => {
    setLoading(true);
    try {
      const response = await updateCategory(id, data);
      message.success('分类更新成功');
      return response.data.data;
    } catch (error) {
      console.error('更新分类失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 删除分类
   */
  const handleDeleteCategory = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await deleteCategory(id);
      message.success('分类删除成功');
      return true;
    } catch (error) {
      console.error('删除分类失败:', error);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取分类责任人
   */
  const fetchOwners = useCallback(async (categoryId: string) => {
    setLoading(true);
    try {
      const response = await getCategoryOwners(categoryId);
      setOwners(response.data.data || []);
      return response.data.data;
    } catch (error) {
      console.error('获取责任人失败:', error);
      return [];
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 添加责任人
   */
  const handleAddOwner = useCallback(async (categoryId: string, data: AddOwnerParams) => {
    setLoading(true);
    try {
      const response = await addCategoryOwner(categoryId, data);
      message.success('责任人添加成功');
      return response.data.data;
    } catch (error) {
      console.error('添加责任人失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 移除责任人
   */
  const handleRemoveOwner = useCallback(async (categoryId: string, userId: string) => {
    setLoading(true);
    try {
      await removeCategoryOwner(categoryId, userId);
      message.success('责任人移除成功');
      return true;
    } catch (error) {
      console.error('移除责任人失败:', error);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    categoryTree,
    currentCategory,
    owners,
    fetchCategoryTree,
    fetchCategoryDetail,
    handleCreateCategory,
    handleUpdateCategory,
    handleDeleteCategory,
    fetchOwners,
    handleAddOwner,
    handleRemoveOwner,
  };
};
