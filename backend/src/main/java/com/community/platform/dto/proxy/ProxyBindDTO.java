package com.community.platform.dto.proxy;

import lombok.Data;

@Data
public class ProxyBindDTO {

    private Long targetUserId;

    private Long targetProfileId;

    private String relation;

    private String authorizedActions;
}
