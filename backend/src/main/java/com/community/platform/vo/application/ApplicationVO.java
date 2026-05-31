package com.community.platform.vo.application;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Application view object.
 */
@Data
public class ApplicationVO {

    private Long applicationId;

    private Long itemId;

    private String itemName;

    private String category;

    private String status;

    private String statusLabel;

    private LocalDateTime submitTime;

    private LocalDateTime updateTime;

    private String remark;

    private Boolean isProxy;

    private Long orderId;

    private String workOrderStatus;

    private String workOrderStatusLabel;

    private String auditOpinion;

    private String staffName;

    private String formData;

    private List<Map<String, Object>> materials;

    private List<Map<String, Object>> requiredMaterials;

    private MaterialCompletenessVO materialCompleteness;
}
