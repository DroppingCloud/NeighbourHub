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

    // 是否为代办（有 proxyUserId 表示代办）
    private Boolean isProxy;
    private String proxyUserName;

    public Boolean getIsProxy() { return isProxy; }
    public void setIsProxy(Boolean isProxy) { this.isProxy = isProxy; }
    public String getProxyUserName() { return proxyUserName; }
    public void setProxyUserName(String proxyUserName) { this.proxyUserName = proxyUserName; }
}
