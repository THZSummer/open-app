package com.xxx.it.works.wecode.v2.modules.chatbotbindtab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 通讯录 API 请求 DTO
 *
 * @author SDDU Build Agent
 * @version 2.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeContactRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待查询的账号列表 */
    private List<String> users;
}
