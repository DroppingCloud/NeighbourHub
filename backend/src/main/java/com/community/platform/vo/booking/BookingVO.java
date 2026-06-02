package com.community.platform.vo.booking;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务预约 VO
 */
@Data
public class BookingVO {

    private Long bookingId;

    private String serviceType;

    private String serviceTypeLabel;

    private LocalDateTime expectTime;

    private String status;

    private String statusLabel;

    private String address;

    private String remark;

    private LocalDateTime createTime;

    private String staffName; 
    private String staffPhone; 

    private String feedback;
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}
