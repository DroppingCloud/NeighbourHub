package com.community.platform.service.impl;

import com.community.platform.common.BusinessException;
import com.community.platform.entity.ProxyRelation;
import com.community.platform.entity.ResidentProfile;
import com.community.platform.mapper.ProxyRelationMapper;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProxyPermissionServiceImplTest {

    private ProxyRelationMapper proxyRelationMapper;
    private ResidentProfileMapper residentProfileMapper;
    private ProxyPermissionServiceImpl proxyPermissionService;

    @BeforeEach
    void setUp() {
        proxyRelationMapper = mock(ProxyRelationMapper.class);
        residentProfileMapper = mock(ResidentProfileMapper.class);
        proxyPermissionService = new ProxyPermissionServiceImpl(
                proxyRelationMapper,
                residentProfileMapper,
                mock(UserMapper.class));
    }

    @Test
    void shouldReturnCurrentUserWhenNoProxyTarget() {
        Long targetUserId = proxyPermissionService.validateAndGetTargetUserId(5L, null, "apply");

        assertEquals(5L, targetUserId);
    }

    @Test
    void activeFamilyRelationShouldAllowAuthorizedAction() {
        ResidentProfile profile = new ResidentProfile();
        profile.setProfileId(100L);
        profile.setUserId(8L);
        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(5L);
        relation.setTargetUserId(8L);
        relation.setTargetProfileId(100L);
        relation.setStatus("active");
        relation.setAuthorizedActions("apply,booking,query");

        when(residentProfileMapper.selectById(100L)).thenReturn(profile);
        when(proxyRelationMapper.selectOne(any())).thenReturn(relation);

        Long targetUserId = proxyPermissionService.validateAndGetTargetUserId(5L, 100L, "booking");

        assertEquals(8L, targetUserId);
    }

    @Test
    void missingFamilyRelationShouldBeForbidden() {
        ResidentProfile profile = new ResidentProfile();
        profile.setProfileId(100L);
        profile.setUserId(8L);

        when(residentProfileMapper.selectById(100L)).thenReturn(profile);
        when(proxyRelationMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> proxyPermissionService.validateAndGetTargetUserId(5L, 100L, "apply"));

        assertEquals(403, exception.getCode());
    }

    @Test
    void unauthorizedProxyActionShouldBeForbidden() {
        ResidentProfile profile = new ResidentProfile();
        profile.setProfileId(100L);
        profile.setUserId(8L);
        ProxyRelation relation = new ProxyRelation();
        relation.setProxyUserId(5L);
        relation.setTargetUserId(8L);
        relation.setTargetProfileId(100L);
        relation.setStatus("active");
        relation.setAuthorizedActions("booking");

        when(residentProfileMapper.selectById(100L)).thenReturn(profile);
        when(proxyRelationMapper.selectOne(any())).thenReturn(relation);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> proxyPermissionService.validateAndGetTargetUserId(5L, 100L, "apply"));

        assertEquals(403, exception.getCode());
    }
}
