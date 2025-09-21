package com.marketplacehelper.controller;

import com.marketplacehelper.auth.SimpleAuthFilter;
import com.marketplacehelper.dto.ProductImportResultDto;
import com.marketplacehelper.service.ProductImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductImportController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SimpleAuthFilter.class))
class ProductImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductImportService productImportService;

    @Test
    void dryRunReturnsOkAndDoesNotCreate() throws Exception {
        ProductImportResultDto result = new ProductImportResultDto();
        result.setCreated(1);
        result.setUpdated(0);
        result.setSkipped(0);
        when(productImportService.importFromExcel(any(), eq(true))).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1,2,3});

        mockMvc.perform(multipart("/api/products/import/excel").file(file).param("dryRun", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(0));
    }

    @Test
    void regularImportReturnsCreated() throws Exception {
        ProductImportResultDto result = new ProductImportResultDto();
        result.setCreated(1);
        when(productImportService.importFromExcel(any(), eq(false))).thenReturn(result);

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1});

        mockMvc.perform(multipart("/api/products/import/excel").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.created").value(1));
    }
}


