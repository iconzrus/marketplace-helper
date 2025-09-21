package com.marketplacehelper.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SimpleAuthFilterQueryTokenTest.DummyController.class)
@Import({SimpleAuthFilter.class})
class SimpleAuthFilterQueryTokenTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SimpleAuthService authService;

    @BeforeEach
    void setup() {
        when(authService.isTokenValid("abc"))
                .thenReturn(true);
    }

    @RestController
    @RequestMapping("/api/protected")
    static class DummyController {
        @GetMapping
        public String ok() { return "OK"; }
    }

    @Test
    void allowsTokenAsQueryParam() throws Exception {
        mockMvc.perform(get("/api/protected?token=abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }
}


