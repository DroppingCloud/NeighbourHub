package com.community.platform.vo.workorder;

import com.community.platform.vo.application.MaterialCompletenessVO;
import com.community.platform.entity.ApplicationMaterial;
import lombok.Data;
import com.community.platform.vo.application.ApplicationVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Work order view object.
 */
@Data
public class WorkOrderVO {

    private Long orderId;

    private Long applicationId;

    private String itemName;

    private String category;

    private String residentName;

    private String applicationStatus;

    private String applicationStatusLabel;

    private String formData;

    private String remark;

    private LocalDateTime submitTime;

    private String status;

    private String statusLabel;

    private String auditOpinion;

    private String staffName;

    private MaterialCompletenessVO materialCompleteness;

    private List<ApplicationMaterial> materials;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private ApplicationVO applicationDetail;
}
