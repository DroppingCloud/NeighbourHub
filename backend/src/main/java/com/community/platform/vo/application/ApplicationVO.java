package com.community.platform.vo.application;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 申请单 VO
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
}
