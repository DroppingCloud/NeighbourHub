package com.community.platform.service;

import com.community.platform.dto.proxy.ProxyBindDTO;
import com.community.platform.entity.ProxyRelation;

import java.util.List;

public interface ProxyRelationService {
    Long bind(Long proxyUserId, ProxyBindDTO dto);
    List<ProxyRelation> getList(Long proxyUserId);
    void revoke(Long proxyUserId, Long id);
}
