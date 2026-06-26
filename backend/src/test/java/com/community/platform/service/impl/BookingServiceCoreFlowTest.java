package com.community.platform.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.community.platform.common.BusinessException;
import com.community.platform.dto.booking.BookingDTO;
import com.community.platform.entity.ServiceBooking;
import com.community.platform.entity.User;
import com.community.platform.mapper.ResidentProfileMapper;
import com.community.platform.mapper.ServiceBookingMapper;
import com.community.platform.mapper.UserMapper;
import com.community.platform.mapper.UserRoleMapper;
import com.community.platform.service.NoticeService;
import com.community.platform.service.ProxyPermissionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceCoreFlowTest {

    private ServiceBookingMapper serviceBookingMapper;
    private UserMapper userMapper;
    private UserRoleMapper userRoleMapper;
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                ServiceBooking.class);
        serviceBookingMapper = mock(ServiceBookingMapper.class);
        userMapper = mock(UserMapper.class);
        userRoleMapper = mock(UserRoleMapper.class);
        bookingService = new BookingServiceImpl(
                serviceBookingMapper,
                mock(NoticeService.class),
                userMapper,
                userRoleMapper,
                mock(ResidentProfileMapper.class),
                mock(ProxyPermissionService.class));
    }

    @Test
    void createShouldRejectUnsupportedServiceType() {
        BookingDTO dto = new BookingDTO();
        dto.setServiceType("unknown");
        dto.setExpectTime(LocalDateTime.now().plusDays(1));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookingService.create(7L, dto));

        assertEquals(2301, exception.getCode());
        verify(serviceBookingMapper, never()).insert(any(ServiceBooking.class));
    }

    @Test
    void bookingStaffCanClaimPendingBooking() {
        User bookingStaff = staff(9L, "booking", "dining");
        ServiceBooking booking = booking(33L, "dining", "pending", null);

        when(userMapper.selectById(9L)).thenReturn(bookingStaff);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(serviceBookingMapper.selectById(33L)).thenReturn(booking);
        when(serviceBookingMapper.update(isNull(), any())).thenReturn(1);
        when(userMapper.selectList(any())).thenReturn(List.of());

        bookingService.claim(9L, 33L);

        verify(serviceBookingMapper).update(isNull(), any());
    }

    @Test
    void claimShouldFailWhenBookingAlreadyClaimedConcurrently() {
        User bookingStaff = staff(9L, "booking", "dining");
        ServiceBooking booking = booking(33L, "dining", "pending", null);

        when(userMapper.selectById(9L)).thenReturn(bookingStaff);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());
        when(serviceBookingMapper.selectById(33L)).thenReturn(booking);
        when(serviceBookingMapper.update(isNull(), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookingService.claim(9L, 33L));

        assertEquals(2304, exception.getCode());
    }

    @Test
    void applicationStaffCannotClaimBooking() {
        User applicationStaff = staff(9L, "application", null);
        when(userMapper.selectById(9L)).thenReturn(applicationStaff);
        when(userRoleMapper.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> bookingService.claim(9L, 33L));

        assertEquals(403, exception.getCode());
        verify(serviceBookingMapper, never()).update(isNull(), any());
    }

    private User staff(Long userId, String staffType, String bookingServiceType) {
        User user = new User();
        user.setUserId(userId);
        user.setRole("staff");
        user.setStatus("active");
        user.setStaffType(staffType);
        user.setBookingServiceType(bookingServiceType);
        user.setCommunityId(1L);
        return user;
    }

    private ServiceBooking booking(Long bookingId, String serviceType, String status, Long staffUserId) {
        ServiceBooking booking = new ServiceBooking();
        booking.setBookingId(bookingId);
        booking.setServiceType(serviceType);
        booking.setStatus(status);
        booking.setStaffUserId(staffUserId);
        booking.setUserId(7L);
        booking.setCommunityId(1L);
        return booking;
    }
}
