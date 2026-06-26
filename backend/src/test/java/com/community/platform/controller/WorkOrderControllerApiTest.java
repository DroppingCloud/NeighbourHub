package com.community.platform.controller;

import com.community.platform.dto.workorder.WorkOrderAuditDTO;
import com.community.platform.service.WorkOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkOrderControllerApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WorkOrderService workOrderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        workOrderService = mock(WorkOrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkOrderController(workOrderService)).build();
    }

    @Test
    void shouldAuditWorkOrderAndReturnUnifiedResult() throws Exception {
        WorkOrderAuditDTO dto = new WorkOrderAuditDTO();
        dto.setOrderId(11L);
        dto.setAction("approved");
        dto.setOpinion("材料齐全");

        mockMvc.perform(post("/api/workorder/audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("success")));

        verify(workOrderService).audit(isNull(), any(WorkOrderAuditDTO.class));
    }
}
