package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductIssueDto {

    private String field;
    private String reason;
    private String suggestion;
    private boolean blocking;

    public ProductIssueDto() {
    }

    public ProductIssueDto(String field, String reason, String suggestion, boolean blocking) {
        this.field = field;
        this.reason = reason;
        this.suggestion = suggestion;
        this.blocking = blocking;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }
}


