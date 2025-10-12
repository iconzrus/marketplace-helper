package com.marketplacehelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WbAuthTokenProvider {
    private volatile String token;

    public WbAuthTokenProvider(@Value("${wb.api.token:}") String initial) {
        this.token = initial == null ? "" : initial;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token;
    }
}


