package com.community.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.platform.entity.ServiceItem;
import com.community.platform.service.ServiceItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceItemControllerApiTest {

    private ServiceItemService serviceItemService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        serviceItemService = mock(ServiceItemService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ServiceItemController(serviceItemService)).build();
    }

    @Test
    void shouldReturnServiceItemListWithUnifiedResult() throws Exception {
        ServiceItem item = new ServiceItem();
        item.setItemId(1L);
        item.setItemName("居住证办理");
        item.setCategory("证件");
        item.setStatus("online");

        Page<ServiceItem> page = new Page<>(1, 10, 1);
        page.setRecords(List.of(item));

        when(serviceItemService.getList(eq("证件"), eq("online"), eq(1), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/service-item/list")
                        .param("category", "证件")
                        .param("status", "online")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("success")))
                .andExpect(jsonPath("$.data.records[0].itemName", is("居住证办理")));
    }
}
