package com.marketplacehelper.controller;

import com.marketplacehelper.model.DailySnapshot;
import com.marketplacehelper.service.SnapshotService;
import com.marketplacehelper.auth.SimpleAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SnapshotController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SimpleAuthFilter.class))
class SnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SnapshotService snapshotService;

    // exclude SimpleAuthFilter via annotation above

    @Test
    void shouldTakeSnapshot() throws Exception {
        doNothing().when(snapshotService).takeDailySnapshot(any());
        mockMvc.perform(post("/api/snapshots/take")).andExpect(status().isOk());
    }

    @Test
    void shouldReturnSnapshots() throws Exception {
        DailySnapshot s = new DailySnapshot();
        s.setWbArticle("100");
        when(snapshotService.getSnapshots(eq(LocalDate.parse("2025-01-01")), eq(LocalDate.parse("2025-01-31"))))
                .thenReturn(List.of(s));

        mockMvc.perform(get("/api/snapshots")
                        .param("from", "2025-01-01")
                        .param("to", "2025-01-31"))
                .andExpect(status().isOk());
    }
}


