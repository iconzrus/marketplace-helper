package com.marketplacehelper.controller;

import com.marketplacehelper.auth.SimpleAuthFilter;
import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SimpleAuthFilter.class))
class AlertsSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @Test
    void streamProducesTextEventStream() throws Exception {
        when(alertService.buildAlerts()).thenReturn(List.of(new AlertDto()));
        mockMvc.perform(get("/api/alerts/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/event-stream"));
    }
}


