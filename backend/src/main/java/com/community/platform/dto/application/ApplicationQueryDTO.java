package com.community.platform.dto.application;

import lombok.Data;

/**
 * 申请查询 DTO
 */
@Data
public class ApplicationQueryDTO {

    private String status;

    private Long itemId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long proxyFor;
}
