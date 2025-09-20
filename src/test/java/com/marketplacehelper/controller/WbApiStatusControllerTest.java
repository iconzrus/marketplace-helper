package com.marketplacehelper.controller;

import com.marketplacehelper.dto.WbApiEndpointStatus;
import com.marketplacehelper.dto.WbApiStatusReportDto;
import com.marketplacehelper.service.WbApiStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WbApiStatusController.class)
class WbApiStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WbApiStatusService statusService;

    @Test
    void returnsStatusReport() throws Exception {
        WbApiStatusReportDto report = new WbApiStatusReportDto();
        report.setCheckedAt(Instant.now());
        report.setEndpoints(List.of(new WbApiEndpointStatus("Ping", "/ping", "UP", 200, null)));
        when(statusService.checkAll()).thenReturn(report);

        mockMvc.perform(get("/api/wb-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoints[0].name").value("Ping"))
                .andExpect(jsonPath("$.endpoints[0].status").value("UP"));
    }
}


