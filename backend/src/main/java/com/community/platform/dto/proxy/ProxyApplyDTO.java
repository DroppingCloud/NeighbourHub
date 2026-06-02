package com.community.platform.dto.proxy;

import lombok.Data;

@Data
public class ProxyApplyDTO {
    private String realName;      // 真实姓名
    private String idCard;        // 身份证号
    private String relation;      // 关系（子女、父母等）
    private String authorizedActions; // 授权范围，默认 "application,booking,notice"
}