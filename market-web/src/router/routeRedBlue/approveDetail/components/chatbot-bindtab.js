import React, { useState, useEffect, useRef } from 'react';
import { message, Modal, Empty } from 'antd';
import { ExclamationCircleOutlined, PlusOutlined, CloseOutlined } from '@ant-design/icons';
import { fetchBoundAccounts, bindAccount, unbindAccount } from '../thunk';
import less from '../index.module.less';

const { confirm } = Modal;

/**
 * 机器人绑定 Tab 主组件
 *
 * <p>标签流式布局，支持按字符宽度自动换行。
 * 「添加账号」按钮作为最后一个 tag，超出行限制自动换行。
 * 输入状态下点击页面其他区域自动触发保存。</p>
 *
 * @param {Object} props
 * @param {string} props.appId - 当前应用的业务 ID，由父页面传入
 */
const AppChatbotBindTab = ({ appId }) => {
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(false);
  const [addMode, setAddMode] = useState(false);
  const [addValue, setAddValue] = useState('');
  const [addHint, setAddHint] = useState('输入机器人账号 ID');
  const [addHintError, setAddHintError] = useState(false);
  const addInputRef = useRef(null);

  /** 加载已绑定列表 */
  const fetchData = async () => {
    if (!appId) return;
    setLoading(true);
    try {
      const result = await fetchBoundAccounts(appId);
      if (result && result.code === '200') {
        setAccounts(result.data || []);
      } else {
        message.error(result?.messageZh || '获取数据失败');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [appId]);

  /** 点击外部自动保存（输入状态下） */
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (!addMode) return;
      const inlineTag = document.getElementById('addInlineTag');
      const addBtn = document.getElementById('addBtnTag');
      if (inlineTag && !inlineTag.contains(e.target) &&
          addBtn && !addBtn.contains(e.target)) {
        submitAdd();
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [addMode, addValue]);

  /** 解绑确认 */
  const handleUnbind = (accountId) => {
    confirm({
      title: '确认删除该机器人账号吗？',
      icon: <ExclamationCircleOutlined />,
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        const result = await unbindAccount(appId, accountId);
        if (result && result.code === '200') {
          message.success('解绑成功');
          fetchData();
        } else {
          message.error(result?.messageZh || '解绑失败');
        }
      },
    });
  };

  /** 显示内联输入框 */
  const showAddInput = () => {
    setAddMode(true);
    setAddValue('');
    setAddHint('输入机器人账号 ID');
    setAddHintError(false);
    setTimeout(() => {
      addInputRef.current?.focus();
    }, 50);
  };

  /** 隐藏内联输入框 */
  const hideAddInput = () => {
    setAddMode(false);
    setAddValue('');
  };

  /** 提交绑定（内联输入） */
  const submitAdd = async () => {
    const val = addValue.trim();
    if (!val) {
      hideAddInput();
      return;
    }
    const result = await bindAccount(appId, val);
    if (result && result.code === '200') {
      message.success('绑定成功');
      hideAddInput();
      fetchData();
    } else {
      message.error(result?.messageZh || '绑定失败');
      hideAddInput();
    }
  };

  /** 输入框按键处理 */
  const handleInputKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      submitAdd();
    } else if (e.key === 'Escape') {
      hideAddInput();
    }
  };

  return (
    <div className={less.container}>
      {/* 标签流式容器 */}
      <div className={less.tagContainer}>
        {/* 账号标签 */}
        {accounts.map((account) => (
          <span key={account.id} className={less.tag}>
            {account.accountId}
            <button
              className={less.tagDelete}
              title="删除"
              onClick={() => handleUnbind(account.accountId)}
            >
              <CloseOutlined />
            </button>
          </span>
        ))}

        {/* 添加账号按钮 / 内联输入框 */}
        {addMode ? (
          <div id="addInlineTag" className={less.inlineInputTag}>
            <input
              ref={addInputRef}
              className={less.inlineInput}
              placeholder="输入机器人账号"
              value={addValue}
              onChange={(e) => setAddValue(e.target.value)}
              onKeyDown={handleInputKeyDown}
              onBlur={() => {
                // 延迟处理，让 clickOutside 先触发
                setTimeout(() => {
                  if (addMode) submitAdd();
                }, 100);
              }}
              autoComplete="off"
            />
            <span className={`${less.inlineHint} ${addHintError ? less.errorText : ''}`}>
              {addHint}
            </span>
          </div>
        ) : (
          <button
            id="addBtnTag"
            className={`${less.tag} ${less.tagAdd}`}
            onClick={showAddInput}
          >
            <PlusOutlined /> 添加账号
          </button>
        )}
      </div>
    </div>
  );
};

export default AppChatbotBindTab;
