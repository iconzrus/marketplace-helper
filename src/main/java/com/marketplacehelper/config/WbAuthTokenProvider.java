package com.marketplacehelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WbAuthTokenProvider {
    private volatile String token;
    private volatile boolean tokenSetViaApi = false;

    public WbAuthTokenProvider(@Value("${wb.api.token:}") String initial) {
            this.token = normalize(initial);
        // Don't mark as "set via API" if it's just from env
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
            this.token = normalize(token);
        this.tokenSetViaApi = true;
    }

    public boolean hasTokenSetViaApi() {
        return tokenSetViaApi;
    }

        private String normalize(String raw) {
            if (raw == null) return "";
            String t = raw.trim();
            if (t.toLowerCase().startsWith("bearer ")) {
                t = t.substring(7).trim();
            }
            return t;
        }
}


