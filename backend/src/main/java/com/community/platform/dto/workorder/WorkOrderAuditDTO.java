package com.community.platform.dto.workorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 工单审核 DTO
 */
@Data
public class WorkOrderAuditDTO {

    @NotNull(message = "工单ID不能为空")
    private Long orderId;

    /**
     * 审核动作:
     * approved - 审核通过
     * rejected - 驳回
     * supplement_required - 退回补件
     * completed - 办结
     */
    @NotBlank(message = "审核动作不能为空")
    private String action;

    private String opinion;
}
