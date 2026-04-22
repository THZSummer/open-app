import { useState, useCallback } from 'react';
import { message } from 'antd';
import {
  getEventList,
  getEventDetail,
  createEvent,
  updateEvent,
  deleteEvent,
  withdrawEvent,
  Event,
  EventListParams,
  CreateEventParams,
  UpdateEventParams,
} from '@/services/event.service';

/**
 * 事件管理 Hook
 */
export const useEvent = () => {
  const [loading, setLoading] = useState(false);
  const [eventList, setEventList] = useState<Event[]>([]);
  const [currentEvent, setCurrentEvent] = useState<Event | null>(null);
  const [total, setTotal] = useState(0);

  /**
   * 获取事件列表
   */
  const fetchEventList = useCallback(async (params: EventListParams) => {
    setLoading(true);
    try {
      const response = await getEventList(params);
      setEventList(response.data.data || []);
      setTotal(response.data.page?.total || 0);
      return {
        list: response.data.data || [],
        total: response.data.page?.total || 0,
      };
    } catch (error) {
      console.error('获取事件列表失败:', error);
      return { list: [], total: 0 };
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 获取事件详情
   */
  const fetchEventDetail = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await getEventDetail(id);
      setCurrentEvent(response.data.data);
      return response.data.data;
    } catch (error) {
      console.error('获取事件详情失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 注册事件
   */
  const handleCreateEvent = useCallback(async (data: CreateEventParams) => {
    setLoading(true);
    try {
      const response = await createEvent(data);
      message.success('事件注册成功，等待审批');
      return response.data.data;
    } catch (error) {
      console.error('注册事件失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 更新事件
   */
  const handleUpdateEvent = useCallback(async (id: string, data: UpdateEventParams) => {
    setLoading(true);
    try {
      const response = await updateEvent(id, data);
      message.success('事件更新成功');
      return response.data.data;
    } catch (error) {
      console.error('更新事件失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 删除事件
   */
  const handleDeleteEvent = useCallback(async (id: string) => {
    setLoading(true);
    try {
      await deleteEvent(id);
      message.success('事件删除成功');
      return true;
    } catch (error) {
      console.error('删除事件失败:', error);
      return false;
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * 撤回事件
   */
  const handleWithdrawEvent = useCallback(async (id: string) => {
    setLoading(true);
    try {
      const response = await withdrawEvent(id);
      message.success('事件已撤回');
      return response.data.data;
    } catch (error) {
      console.error('撤回事件失败:', error);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    eventList,
    currentEvent,
    total,
    fetchEventList,
    fetchEventDetail,
    handleCreateEvent,
    handleUpdateEvent,
    handleDeleteEvent,
    handleWithdrawEvent,
  };
};
