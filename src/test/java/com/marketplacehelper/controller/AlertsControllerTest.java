package com.marketplacehelper.controller;

import com.marketplacehelper.dto.AlertDto;
import com.marketplacehelper.service.AlertService;
import com.marketplacehelper.auth.SimpleAuthFilter;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = AlertsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SimpleAuthFilter.class))
class AlertsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    // exclude SimpleAuthFilter via annotation above

    @Test
    void shouldReturnAlerts() throws Exception {
        AlertDto a = new AlertDto();
        a.setType(AlertDto.AlertType.LOW_STOCK);
        a.setWbArticle("123");
        a.setName("Test");
        when(alertService.buildAlerts()).thenReturn(List.of(a));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].wbArticle").value("123"))
                .andExpect(jsonPath("$[0].type").value("LOW_STOCK"));
    }
}


