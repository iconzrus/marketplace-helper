package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbApiStatusReportDto {

    private Instant checkedAt;
    private List<WbApiEndpointStatus> endpoints;

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public List<WbApiEndpointStatus> getEndpoints() { return endpoints; }
    public void setEndpoints(List<WbApiEndpointStatus> endpoints) { this.endpoints = endpoints; }
}


