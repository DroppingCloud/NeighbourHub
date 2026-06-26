package com.community.platform.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkOrderStatusTest {

    @Test
    void shouldResolveStatusByCode() {
        assertEquals(WorkOrderStatus.PENDING, WorkOrderStatus.fromCode("pending"));
        assertEquals(WorkOrderStatus.PROCESSING, WorkOrderStatus.fromCode("processing"));
        assertNull(WorkOrderStatus.fromCode("unknown"));
    }

    @Test
    void shouldAllowDesignedStateTransitions() {
        assertTrue(WorkOrderStatus.isValidTransition("pending", "processing"));
        assertTrue(WorkOrderStatus.isValidTransition("processing", "approved"));
        assertTrue(WorkOrderStatus.isValidTransition("processing", "supplement_required"));
        assertTrue(WorkOrderStatus.isValidTransition("approved", "completed"));
    }

    @Test
    void shouldRejectIllegalStateTransitions() {
        assertFalse(WorkOrderStatus.isValidTransition("pending", "completed"));
        assertFalse(WorkOrderStatus.isValidTransition("completed", "processing"));
        assertFalse(WorkOrderStatus.isValidTransition("rejected", "approved"));
        assertFalse(WorkOrderStatus.isValidTransition("unknown", "pending"));
    }
}
