package com.community.platform.vo.workorder;

import com.community.platform.vo.application.MaterialCompletenessVO;
import lombok.Data;
import com.community.platform.vo.application.ApplicationVO;

import java.time.LocalDateTime;

/**
 * Work order view object.
 */
@Data
public class WorkOrderVO {

    private Long orderId;

    private Long applicationId;

    private String itemName;

    private String residentName;

    private String status;

    private String statusLabel;

    private String auditOpinion;

    private String staffName;

    private MaterialCompletenessVO materialCompleteness;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private ApplicationVO applicationDetail;
}
