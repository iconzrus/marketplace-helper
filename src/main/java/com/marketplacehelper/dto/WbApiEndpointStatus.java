package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WbApiEndpointStatus {

    private String name;
    private String path;
    private String status; // UP | DOWN
    private Integer httpStatus;
    private String message;

    public WbApiEndpointStatus() {}

    public WbApiEndpointStatus(String name, String path, String status, Integer httpStatus, String message) {
        this.name = name;
        this.path = path;
        this.status = status;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getHttpStatus() { return httpStatus; }
    public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}


