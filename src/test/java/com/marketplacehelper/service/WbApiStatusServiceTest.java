package com.marketplacehelper.service;

import com.marketplacehelper.dto.WbApiStatusReportDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@RestClientTest(WbApiStatusService.class)
class WbApiStatusServiceTest {

    @Autowired
    private WbApiStatusService statusService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MockRestServiceServer server;

    @MockBean
    private WbApiService wbApiService;

    @Test
    void shouldReportUpWhenEndpointsNot503() {
        // ping
        server.expect(requestTo("/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON));
        // seller info
        server.expect(requestTo("https://common-api.wildberries.ru/api/v1/seller-info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)); // не 503 — считаем UP по методу исключения

        // goods — через wbApiService (мок)
        doNothing().when(wbApiService).syncProductsFromWbApi();
        // В статус‑чеке мы дергаем getGoodsWithPrices(); подменять ответ не требуется, просто не бросаем исключение

        WbApiStatusReportDto report = statusService.checkAll();
        assertThat(report.getEndpoints()).isNotEmpty();
        assertThat(report.getEndpoints()).allMatch(e -> "UP".equals(e.getStatus()));
    }

    @Test
    void shouldReportDownWhen503() {
        server.expect(requestTo("/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // Остальные пусть будут ок
        server.expect(requestTo("https://common-api.wildberries.ru/api/v1/seller-info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        WbApiStatusReportDto report = statusService.checkAll();
        assertThat(report.getEndpoints()).anyMatch(e -> 
                e.getPath().equals("/ping") && "DOWN".equals(e.getStatus()));
    }
}


