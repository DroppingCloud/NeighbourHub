package com.community.platform.dto.guide;

import lombok.Data;

/**
 * 智能导办请求 DTO
 */
@Data
public class GuideRequestDTO {

    /** 居民类型: local/non_local */
    private String residentType;

    private Integer age;

    /** 需求类型: 证明/补贴/证件/服务 */
    private String needType;

    /** 自然语言描述的办事需求 */
    private String description;
}
