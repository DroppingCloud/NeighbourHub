package com.community.platform.service.impl;

import com.community.platform.common.BusinessException;
import com.community.platform.dto.application.ApplicationSubmitDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.ServiceItem;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.mapper.*;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.ProxyPermissionService;
import com.community.platform.service.WorkOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplicationServiceCoreFlowTest {

    private ApplicationFormMapper applicationFormMapper;
    private ServiceItemMapper serviceItemMapper;
    private WorkOrderMapper workOrderMapper;
    private UserMapper userMapper;
    private NoticeService noticeService;
    private WorkOrderService workOrderService;
    private ProxyPermissionService proxyPermissionService;
    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationFormMapper = mock(ApplicationFormMapper.class);
        serviceItemMapper = mock(ServiceItemMapper.class);
        workOrderMapper = mock(WorkOrderMapper.class);
        userMapper = mock(UserMapper.class);
        noticeService = mock(NoticeService.class);
        workOrderService = mock(WorkOrderService.class);
        proxyPermissionService = mock(ProxyPermissionService.class);

        applicationService = new ApplicationServiceImpl(
                applicationFormMapper,
                mock(ApplicationMaterialMapper.class),
                serviceItemMapper,
                mock(ServiceMaterialTemplateMapper.class),
                workOrderMapper,
                mock(NoticeMapper.class),
                userMapper,
                mock(ResidentProfileMapper.class),
                noticeService,
                mock(ApplicationMaterialService.class),
                workOrderService,
                proxyPermissionService,
                new ObjectMapper());
    }

    @Test
    void submitShouldCreateApplicationWorkOrderAndAssignStaff() {
        ServiceItem item = serviceItem(2L, "online");
        User resident = new User();
        resident.setUserId(7L);
        resident.setCommunityId(1L);

        when(serviceItemMapper.selectById(2L)).thenReturn(item);
        when(proxyPermissionService.validateAndGetTargetUserId(7L, null, "apply")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(resident);
        when(userMapper.selectList(any())).thenReturn(List.of());
        when(applicationFormMapper.insert(any(ApplicationForm.class))).thenAnswer(invocation -> {
            ApplicationForm application = invocation.getArgument(0);
            application.setApplicationId(101L);
            return 1;
        });
        when(workOrderMapper.insert(any(WorkOrder.class))).thenAnswer(invocation -> {
            WorkOrder order = invocation.getArgument(0);
            order.setOrderId(201L);
            return 1;
        });

        ApplicationSubmitDTO dto = new ApplicationSubmitDTO();
        dto.setItemId(2L);
        dto.setFormData(Map.of("applicant", "张三"));

        Long applicationId = applicationService.submit(7L, dto);

        assertEquals(101L, applicationId);
        verify(applicationFormMapper).insert(any(ApplicationForm.class));
        verify(workOrderMapper).insert(any(WorkOrder.class));
        verify(workOrderService).assign(201L);
        verify(noticeService, atLeastOnce()).sendNotice(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitShouldRejectOfflineServiceItem() {
        when(serviceItemMapper.selectById(2L)).thenReturn(serviceItem(2L, "offline"));

        ApplicationSubmitDTO dto = new ApplicationSubmitDTO();
        dto.setItemId(2L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.submit(7L, dto));

        assertEquals(2002, exception.getCode());
        verify(applicationFormMapper, never()).insert(any(ApplicationForm.class));
    }

    private ServiceItem serviceItem(Long itemId, String status) {
        ServiceItem item = new ServiceItem();
        item.setItemId(itemId);
        item.setItemName("居住证办理");
        item.setStatus(status);
        return item;
    }
}
