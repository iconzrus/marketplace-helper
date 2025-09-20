package com.marketplacehelper.service;

import com.marketplacehelper.dto.AutoFillRequestDto;
import com.marketplacehelper.dto.AutoFillResultDto;
import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DemoDataService {

    private final ProductRepository productRepository;

    public DemoDataService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public AutoFillResultDto autoFillMissingCosts(AutoFillRequestDto request) {
        List<Product> products = productRepository.findAll();
        int affected = 0;
        AutoFillResultDto result = new AutoFillResultDto();
        int limit = request.getLimit() != null && request.getLimit() > 0 ? request.getLimit() : Integer.MAX_VALUE;

        for (Product product : products) {
            List<String> updatedFields = new ArrayList<>();

            BigDecimal basePrice = product.getPrice();
            if (basePrice == null) {
                continue;
            }

            // purchase
            if ((!request.isOnlyIfMissing() || product.getPurchasePrice() == null) && request.getPurchasePercentOfPrice() != null) {
                BigDecimal purchase = percentageOf(basePrice, request.getPurchasePercentOfPrice());
                product.setPurchasePrice(purchase);
                updatedFields.add("purchasePrice");
            }

            // logistics
            if ((!request.isOnlyIfMissing() || product.getLogisticsCost() == null) && request.getLogisticsFixed() != null) {
                product.setLogisticsCost(request.getLogisticsFixed());
                updatedFields.add("logisticsCost");
            }

            // marketing
            if ((!request.isOnlyIfMissing() || product.getMarketingCost() == null) && request.getMarketingPercentOfPrice() != null) {
                BigDecimal marketing = percentageOf(basePrice, request.getMarketingPercentOfPrice());
                product.setMarketingCost(marketing);
                updatedFields.add("marketingCost");
            }

            // other
            if ((!request.isOnlyIfMissing() || product.getOtherExpenses() == null) && request.getOtherFixed() != null) {
                product.setOtherExpenses(request.getOtherFixed());
                updatedFields.add("otherExpenses");
            }

            if (!updatedFields.isEmpty()) {
                productRepository.save(product);
                affected++;
                AutoFillResultDto.AffectedItem item = new AutoFillResultDto.AffectedItem();
                item.setProductId(product.getId());
                item.setName(product.getName());
                item.setWbArticle(product.getWbArticle());
                item.setUpdatedFields(updatedFields);
                result.addItem(item);
                if (affected >= limit) break;
            }
        }

        result.setAffectedCount(affected);
        return result;
    }

    private BigDecimal percentageOf(BigDecimal base, BigDecimal percent) {
        return base.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}


