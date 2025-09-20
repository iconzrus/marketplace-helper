package com.marketplacehelper.service;

import com.marketplacehelper.dto.WbApiStatusReportDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

@RestClientTest(WbApiStatusService.class)
@TestPropertySource(properties = {
        "wb.api.base-url=https://marketplace-api.wildberries.ru"
})
@Import(WbApiStatusServiceTest.TestConfig.class)
class WbApiStatusServiceTest {

    @Autowired
    private WbApiStatusService statusService;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Test
    void shouldReportUpWhenEndpointsNot503() {
        // marketplace ping
        server.expect(requestTo("https://marketplace-api.wildberries.ru/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON));

        // goods filter
        server.expect(requestTo("https://marketplace-api.wildberries.ru/api/v2/list/goods/filter"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)); // не 503 — считаем UP

        // seller info
        server.expect(requestTo("https://common-api.wildberries.ru/api/v1/seller-info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // roots
        server.expect(requestTo("https://content-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://statistics-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://advert-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo("https://finance-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        WbApiStatusReportDto report = statusService.checkAll();
        assertThat(report.getEndpoints()).isNotEmpty();
        assertThat(report.getEndpoints()).allMatch(e -> "UP".equals(e.getStatus()));
    }

    @Test
    void shouldReportDownWhen503() {
        server.expect(requestTo("https://marketplace-api.wildberries.ru/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        // остальные ok
        server.expect(requestTo("https://marketplace-api.wildberries.ru/api/v2/list/goods/filter"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo("https://common-api.wildberries.ru/api/v1/seller-info"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo("https://content-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo("https://statistics-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo("https://advert-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));
        server.expect(requestTo("https://finance-api.wildberries.ru"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        WbApiStatusReportDto report = statusService.checkAll();
        assertThat(report.getEndpoints()).anyMatch(e ->
                e.getPath().equals("https://marketplace-api.wildberries.ru/ping") && "DOWN".equals(e.getStatus()));
    }
}


