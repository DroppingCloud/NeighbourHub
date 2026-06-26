package com.community.platform.service.impl;

import com.community.platform.common.BusinessException;
import com.community.platform.common.utils.JwtUtil;
import com.community.platform.dto.auth.LoginDTO;
import com.community.platform.mapper.*;
import com.community.platform.vo.auth.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuthServiceImplTest {

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);

        authService = new AuthServiceImpl(
                mock(UserMapper.class),
                mock(UserRoleMapper.class),
                mock(ResidentProfileMapper.class),
                mock(PasswordEncoder.class),
                jwtUtil,
                mock(ProxyRelationMapper.class),
                mock(ApplicationFormMapper.class),
                mock(ApplicationMaterialMapper.class),
                mock(ServiceBookingMapper.class),
                mock(NoticeMapper.class),
                mock(WorkOrderMapper.class),
                mock(WorkOrderLogMapper.class));
    }

    @Test
    void builtInAdminShouldLoginSuccessfully() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("123456");

        LoginVO result = authService.login(dto);

        assertEquals(1L, result.getUserId());
        assertEquals("admin", result.getUsername());
        assertTrue(result.getRoles().contains("ROLE_ADMIN"));
        assertNotNull(result.getToken());
    }

    @Test
    void builtInAdminShouldRejectWrongPassword() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("admin");
        dto.setPassword("wrong-password");

        BusinessException exception = assertThrows(BusinessException.class, () -> authService.login(dto));

        assertEquals(1001, exception.getCode());
    }
}
