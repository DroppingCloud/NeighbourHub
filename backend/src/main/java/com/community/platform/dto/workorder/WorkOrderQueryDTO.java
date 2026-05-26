package com.community.platform.dto.workorder;

import lombok.Data;

/**
 * 工单查询 DTO
 */
@Data
public class WorkOrderQueryDTO {

    private String status;

    private Long staffUserId;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
