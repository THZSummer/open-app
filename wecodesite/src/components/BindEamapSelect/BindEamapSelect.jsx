import React, { useState, useEffect } from 'react';
import { message, Select } from 'antd';
import { constant, debounce } from 'lodash';
import { fetchEamapOptions } from '../../pages/AppList/thunk';
import './BindEamapSelect.m.less';

const { Option } = Select;

/**
 * 绑定应用服务下拉选择组件
 *
 * @param {Object} props - 组件属性
 * @param {string} props.value - 当前选中值
 * @param {Function} props.onChange - 选择变化回调
 * @param {string} props.placeholder - 占位文字
 */
function BindEamapSelect(props) {
  const { value, onChange, placeholder } = props;
  const [pagination, setPagination] = useState({
    curPage: 1,
    pageSize: 100,
  });
  const [codelist, setCodeList] = useState([]);

  // 查询EAMAP的应用模块列表
  const getEamapList = async (curPage) => {
    const res = await fetchEamapOptions({curPage, pageSize: 100});
    if(res && res.code === '200') {
      const optionsList = (res.data || []).map(opt => ({
        value: opt.eamapAppCode,
        lable: opt.eamapAppName? `${opt.eamapAppName} ${opt.eamapAppCode}` : opt.eamapAppCode,
      }))
      setPagination(res.page);
      setCodeList(codelist.concat(optionsList));
    } else {
      message.error(res?.messageZh || '获取Eamap列表失败');
    }
  };
  
  const selectPopupScroll = debounce(event => {
    const { scrollTop, scrollHeight, clientHeight } = event.target;
    const { curPage, totalPages } = pagination;
    if (scrollTop + clientHeight === scrollHeight && curPage < totalPages) {
      getEamapList(curPage + 1);
    }
  }, 700);

  useEffect(() => {
    getEamapList(1);
  }, []);

  return (
    <Select
      value={value}
      onChange={onChange}
      placeholder={placeholder || '选择应用服务'}
      showSearch
      optionFilterProp='label'
      onPopupScroll={value => selectPopupScroll(value)}
      className='eamap-select-full'
    >
      {codelist?.map((item, index) => {
        return (
          <Option key={index} value={item?.value}>{item?.lable}</Option>
        )
      })}
    </Select>
  );
}

export default BindEamapSelect;
