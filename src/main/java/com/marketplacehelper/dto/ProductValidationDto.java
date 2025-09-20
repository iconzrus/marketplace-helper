package com.marketplacehelper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductValidationDto {

    private Long productId;
    private String name;
    private String wbArticle;
    private boolean requiresCorrection;
    private List<ProductIssueDto> issues;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWbArticle() {
        return wbArticle;
    }

    public void setWbArticle(String wbArticle) {
        this.wbArticle = wbArticle;
    }

    public boolean isRequiresCorrection() {
        return requiresCorrection;
    }

    public void setRequiresCorrection(boolean requiresCorrection) {
        this.requiresCorrection = requiresCorrection;
    }

    public List<ProductIssueDto> getIssues() {
        if (issues == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(issues);
    }

    public void setIssues(List<ProductIssueDto> issues) {
        this.issues = issues;
    }

    public void addIssue(ProductIssueDto issue) {
        if (issue == null) {
            return;
        }
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);
    }
}


