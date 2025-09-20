package com.marketplacehelper.service;

import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ProductImportService {

    private final ProductRepository productRepository;

    public ProductImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public List<Product> importFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл Excel не загружен");
        }

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Не удалось прочитать первый лист Excel");
            }

            Iterator<Row> rowIterator = sheet.rowIterator();
            if (!rowIterator.hasNext()) {
                return Collections.emptyList();
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            DataFormatter formatter = new DataFormatter();

            List<Product> result = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row == null) {
                    continue;
                }

                String name = getString(row, headerMap, formatter, "name", "название", "товар");
                String wbArticle = getString(row, headerMap, formatter, "wb_article", "артикулwb", "артикул");
                if ((name == null || name.isBlank()) && (wbArticle == null || wbArticle.isBlank())) {
                    // пустая строка
                    continue;
                }

                Product product = Optional.ofNullable(wbArticle)
                        .filter(value -> !value.isBlank())
                        .flatMap(productRepository::findByWbArticle)
                        .orElseGet(Product::new);

                if (product.getId() == null) {
                    product.setCreatedAt(java.time.LocalDateTime.now());
                }

                product.setName(Optional.ofNullable(name).orElse(product.getName()));
                product.setWbArticle(wbArticle);
                product.setWbBarcode(getString(row, headerMap, formatter, "wb_barcode", "штрихкод", "barcode"));
                product.setCategory(getString(row, headerMap, formatter, "category", "категория"));
                product.setBrand(getString(row, headerMap, formatter, "brand", "бренд"));
                product.setStockQuantity(getInteger(row, headerMap, formatter, "stock", "остаток", "stock_quantity"));

                if ((product.getName() == null || product.getName().isBlank()) && wbArticle != null) {
                    product.setName("Товар " + wbArticle);
                }

                BigDecimal plannedPrice = getDecimal(row, headerMap, formatter, "price", "продажнаяцена", "цена");
                if (plannedPrice != null) {
                    product.setPrice(plannedPrice);
                } else if (product.getPrice() == null) {
                    product.setPrice(BigDecimal.ONE);
                }

                product.setPurchasePrice(getDecimal(row, headerMap, formatter, "purchaseprice", "закупка", "purchase_price"));
                product.setLogisticsCost(getDecimal(row, headerMap, formatter, "logistics", "логистика", "logistics_cost"));
                product.setMarketingCost(getDecimal(row, headerMap, formatter, "marketing", "маркетинг", "marketing_cost"));
                product.setOtherExpenses(getDecimal(row, headerMap, formatter, "other", "прочие", "other_expenses"));

                product.setUpdatedAt(java.time.LocalDateTime.now());
                result.add(productRepository.save(product));
            }

            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения Excel файла: " + e.getMessage(), e);
        }
    }

    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) {
            String value = formatter.formatCellValue(cell);
            if (value != null && !value.isBlank()) {
                headerMap.put(normalize(value), cell.getColumnIndex());
            }
        }
        return headerMap;
    }

    private String getString(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String... keys) {
        Integer index = getColumnIndex(headerMap, keys);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        String value = formatter.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    private Integer getInteger(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String... keys) {
        Integer index = getColumnIndex(headerMap, keys);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        }
        String text = formatter.formatCellValue(cell);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text.replace(" ", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal getDecimal(Row row, Map<String, Integer> headerMap, DataFormatter formatter, String... keys) {
        Integer index = getColumnIndex(headerMap, keys);
        if (index == null) {
            return null;
        }
        Cell cell = row.getCell(index);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
        }
        String text = formatter.formatCellValue(cell);
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.replace(" ", "").replace("%", "").replace(",", ".");
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer getColumnIndex(Map<String, Integer> headerMap, String... keys) {
        for (String key : keys) {
            Integer idx = headerMap.get(normalize(key));
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9а-яё]", "");
    }
}
