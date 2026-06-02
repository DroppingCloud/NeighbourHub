package com.community.platform.service;

import com.community.platform.dto.proxy.ProxyBindDTO;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.dto.proxy.ProxyApplyDTO;

import java.util.List;

public interface ProxyRelationService {
    Long bind(Long proxyUserId, ProxyBindDTO dto);
    List<ProxyRelation> getList(Long proxyUserId);
    void revoke(Long proxyUserId, Long id);
    Long apply(Long proxyUserId, ProxyApplyDTO dto);
    List<ProxyRelation> getPendingRequests(Long targetUserId);
    void confirm(Long targetUserId, Long relationId);
    void reject(Long targetUserId, Long relationId);
}
