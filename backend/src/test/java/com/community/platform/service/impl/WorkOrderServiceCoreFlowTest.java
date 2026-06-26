package com.community.platform.service.impl;

import com.community.platform.common.BusinessException;
import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.entity.ApplicationForm;
import com.community.platform.entity.User;
import com.community.platform.entity.WorkOrder;
import com.community.platform.entity.WorkOrderLog;
import com.community.platform.mapper.*;
import com.community.platform.service.ApplicationMaterialService;
import com.community.platform.service.ApplicationService;
import com.community.platform.service.NoticeService;
import com.community.platform.service.StaffDispatchService;
import com.community.platform.vo.application.MaterialCompletenessVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WorkOrderServiceCoreFlowTest {

    private WorkOrderMapper workOrderMapper;
    private WorkOrderLogMapper workOrderLogMapper;
    private ApplicationFormMapper applicationFormMapper;
    private ApplicationMaterialMapper applicationMaterialMapper;
    private ServiceItemMapper serviceItemMapper;
    private ResidentProfileMapper residentProfileMapper;
    private UserMapper userMapper;
    private ApplicationMaterialService applicationMaterialService;
    private NoticeService noticeService;
    private StaffDispatchService staffDispatchService;
    private ApplicationService applicationService;
    private WorkOrderServiceImpl workOrderService;

    @BeforeEach
    void setUp() {
        workOrderMapper = mock(WorkOrderMapper.class);
        workOrderLogMapper = mock(WorkOrderLogMapper.class);
        applicationFormMapper = mock(ApplicationFormMapper.class);
        applicationMaterialMapper = mock(ApplicationMaterialMapper.class);
        serviceItemMapper = mock(ServiceItemMapper.class);
        residentProfileMapper = mock(ResidentProfileMapper.class);
        userMapper = mock(UserMapper.class);
        applicationMaterialService = mock(ApplicationMaterialService.class);
        noticeService = mock(NoticeService.class);
        staffDispatchService = mock(StaffDispatchService.class);
        applicationService = mock(ApplicationService.class);

        workOrderService = new WorkOrderServiceImpl(
                workOrderMapper,
                workOrderLogMapper,
                applicationFormMapper,
                applicationMaterialMapper,
                serviceItemMapper,
                residentProfileMapper,
                userMapper,
                applicationMaterialService,
                noticeService,
                staffDispatchService,
                applicationService);
    }

    @Test
    void bookingStaffShouldNotAuditApplicationWorkOrder() {
        WorkOrder order = workOrder(10L, 100L, 2L, "pending", 1L);
        User bookingStaff = staff(2L, "booking", 1L);
        WorkOrderAuditDTO dto = auditDto(10L, "approved");

        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(userMapper.selectById(2L)).thenReturn(bookingStaff);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> workOrderService.audit(2L, dto));

        assertEquals(403, exception.getCode());
    }

    @Test
    void approvingWorkOrderShouldRejectIncompleteRequiredMaterials() {
        WorkOrder order = workOrder(10L, 100L, 2L, "pending", 1L);
        User applicationStaff = staff(2L, "application", 1L);
        ApplicationForm application = new ApplicationForm();
        application.setApplicationId(100L);
        application.setUserId(8L);
        application.setStatus("pending");

        MaterialCompletenessVO completeness = new MaterialCompletenessVO();
        completeness.setApplicationId(100L);
        completeness.setComplete(false);
        completeness.setMissingMaterialNames(List.of("户口簿"));

        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(userMapper.selectById(2L)).thenReturn(applicationStaff);
        when(applicationFormMapper.selectById(100L)).thenReturn(application);
        when(applicationMaterialService.checkCompletenessForSystem(100L)).thenReturn(completeness);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> workOrderService.audit(2L, auditDto(10L, "approved")));

        assertEquals(400, exception.getCode());
        verify(noticeService, never()).sendNotice(any(), any(), any(), any(), any(), any());
    }

    @Test
    void approvingWorkOrderShouldRejectFailedMaterialPrecheck() {
        WorkOrder order = workOrder(10L, 100L, 2L, "pending", 1L);
        User applicationStaff = staff(2L, "application", 1L);
        ApplicationForm application = new ApplicationForm();
        application.setApplicationId(100L);
        application.setUserId(8L);
        application.setStatus("pending");

        MaterialCompletenessVO completeness = new MaterialCompletenessVO();
        completeness.setApplicationId(100L);
        completeness.setComplete(true);

        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(userMapper.selectById(2L)).thenReturn(applicationStaff);
        when(applicationFormMapper.selectById(100L)).thenReturn(application);
        when(applicationMaterialService.checkCompletenessForSystem(100L)).thenReturn(completeness);
        when(applicationMaterialService.hasFailedPrecheckForSystem(100L)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> workOrderService.audit(2L, auditDto(10L, "approved")));

        assertEquals(400, exception.getCode());
        verify(applicationFormMapper, never()).updateById(any(ApplicationForm.class));
    }

    @Test
    void staffCannotAuditOtherCommunityWorkOrder() {
        WorkOrder order = workOrder(10L, 100L, 2L, "pending", 2L);
        User applicationStaff = staff(2L, "application", 1L);
        WorkOrderAuditDTO dto = auditDto(10L, "approved");

        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(userMapper.selectById(2L)).thenReturn(applicationStaff);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> workOrderService.audit(2L, dto));

        assertEquals(403, exception.getCode());
    }

    @Test
    void assignShouldUseApplicationStaffDispatch() {
        WorkOrder order = workOrder(10L, 100L, null, "pending", 1L);
        User assignee = staff(2L, "application", 1L);

        when(workOrderMapper.selectById(10L)).thenReturn(order);
        when(staffDispatchService.selectBestStaff(1L, null, false, "application")).thenReturn(assignee);

        workOrderService.assign(10L);

        assertEquals(2L, order.getStaffUserId());
        verify(workOrderMapper).updateById(order);
        verify(workOrderLogMapper).insert(any(WorkOrderLog.class));
    }

    private WorkOrder workOrder(Long orderId, Long applicationId, Long staffUserId, String status, Long communityId) {
        WorkOrder order = new WorkOrder();
        order.setOrderId(orderId);
        order.setApplicationId(applicationId);
        order.setStaffUserId(staffUserId);
        order.setStatus(status);
        order.setCommunityId(communityId);
        return order;
    }

    private User staff(Long userId, String staffType, Long communityId) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(staffType + "Staff");
        user.setRole("staff");
        user.setStaffType(staffType);
        user.setCommunityId(communityId);
        user.setStatus("active");
        return user;
    }

    private WorkOrderAuditDTO auditDto(Long orderId, String action) {
        WorkOrderAuditDTO dto = new WorkOrderAuditDTO();
        dto.setOrderId(orderId);
        dto.setAction(action);
        dto.setOpinion("测试审核");
        return dto;
    }
}
