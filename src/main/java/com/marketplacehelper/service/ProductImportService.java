package com.marketplacehelper.service;

import com.marketplacehelper.dto.ProductImportResultDto;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductImportService {

    private final ProductRepository productRepository;

    public ProductImportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductImportResultDto importFromExcel(MultipartFile file) {
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
                ProductImportResultDto emptyResult = new ProductImportResultDto();
                emptyResult.setCreated(0);
                emptyResult.setUpdated(0);
                emptyResult.setSkipped(0);
                return emptyResult;
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            DataFormatter formatter = new DataFormatter();

            int created = 0;
            int updated = 0;
            int skipped = 0;
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row == null) {
                    continue;
                }

                String name = getString(row, headerMap, formatter,
                        "name", "название", "товар", "наименование");

                String wbArticle = getString(row, headerMap, formatter,
                        "wb_article", "артикулwb", "артикул");
                if (wbArticle == null || wbArticle.isBlank()) {
                    wbArticle = getString(row, headerMap, formatter,
                            "кодноменклатуры", "nm_id", "nmid", "кодтовара", "номенклатура");
                }
                if (wbArticle == null || wbArticle.isBlank()) {
                    wbArticle = getString(row, headerMap, formatter,
                            "артикулпоставщика", "supplierarticle", "поставщикаартикул");
                }
                if (wbArticle == null || wbArticle.isBlank()) {
                    wbArticle = getString(row, headerMap, formatter,
                            "srid", "корзина", "idкорзинызаказа");
                }
                if ((name == null || name.isBlank()) && (wbArticle == null || wbArticle.isBlank())) {
                    // пустая строка
                    skipped++;
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
                if (wbArticle != null) {
                    product.setWbArticle(wbArticle.trim());
                }
                product.setWbBarcode(getString(row, headerMap, formatter,
                        "wb_barcode", "штрихкод", "barcode", "баркод", "шк"));
                product.setCategory(getString(row, headerMap, formatter,
                        "category", "категория", "предмет", "категориятовара"));
                product.setBrand(getString(row, headerMap, formatter, "brand", "бренд"));
                product.setStockQuantity(getInteger(row, headerMap, formatter,
                        "stock", "остаток", "stock_quantity", "колво", "количество"));

                if ((product.getName() == null || product.getName().isBlank()) && wbArticle != null) {
                    product.setName("Товар " + wbArticle);
                }

                boolean isNew = product.getId() == null;

                BigDecimal plannedPrice = getDecimal(row, headerMap, formatter,
                        "price", "продажнаяцена", "цена", "ценарозничная", "розничнаяцена");
                if (plannedPrice == null) {
                    plannedPrice = getDecimal(row, headerMap, formatter,
                            "вайлдберризреализовалтоварпр", "реализация", "продажапоакции");
                }
                if (plannedPrice != null) {
                    if (plannedPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        errors.add(String.format(Locale.ROOT,
                                "Строка %d: цена должна быть больше нуля", row.getRowNum() + 1));
                        skipped++;
                        continue;
                    }
                    product.setPrice(plannedPrice);
                } else if (product.getPrice() == null) {
                    product.setPrice(BigDecimal.ONE);
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указана цена, установлено значение по умолчанию 1", row.getRowNum() + 1));
                }

                BigDecimal purchase = getDecimal(row, headerMap, formatter,
                        "purchaseprice", "закупка", "purchase_price", "закупочнаяцена");
                if (purchase == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не заполнено поле «Закупка».", row.getRowNum() + 1));
                }
                product.setPurchasePrice(purchase);

                BigDecimal logistics = getDecimal(row, headerMap, formatter,
                        "logistics", "логистика", "logistics_cost", "возмещениеиздержек", "доставка");
                if (logistics == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указаны логистические расходы.", row.getRowNum() + 1));
                }
                product.setLogisticsCost(logistics);

                BigDecimal marketing = getDecimal(row, headerMap, formatter,
                        "marketing", "маркетинг", "marketing_cost", "промокод", "реклама");
                if (marketing == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указаны маркетинговые расходы.", row.getRowNum() + 1));
                }
                product.setMarketingCost(marketing);

                BigDecimal other = getDecimal(row, headerMap, formatter,
                        "other", "прочие", "other_expenses", "прочиерасходы", "штрафы");
                if (other == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указаны прочие расходы.", row.getRowNum() + 1));
                }
                product.setOtherExpenses(other);

                product.setUpdatedAt(java.time.LocalDateTime.now());
                productRepository.save(product);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }

            ProductImportResultDto result = new ProductImportResultDto();
            result.setCreated(created);
            result.setUpdated(updated);
            result.setSkipped(skipped);
            if (!warnings.isEmpty()) {
                result.setWarnings(List.copyOf(warnings));
            }
            if (!errors.isEmpty()) {
                result.setErrors(List.copyOf(errors));
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
