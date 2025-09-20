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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductImportService {

    private static final String[] NAME_HEADERS = {
            "name", "название", "товар"
    };
    private static final String[] ARTICLE_HEADERS = {
            "wb_article", "артикулwb", "артикул", "артикул поставщика", "vendor code",
            "vendorcode", "код номенклатуры", "nm_id", "nm"
    };
    private static final String[] BARCODE_HEADERS = {
            "wb_barcode", "штрихкод", "barcode", "баркод"
    };
    private static final String[] CATEGORY_HEADERS = {
            "category", "категория", "предмет"
    };
    private static final String[] BRAND_HEADERS = {
            "brand", "бренд"
    };
    private static final String[] STOCK_HEADERS = {
            "stock", "остаток", "stock_quantity", "количество", "кол-во", "колво"
    };
    private static final String[] PRICE_HEADERS = {
        "price", "продажная цена", "цена", "цена розничная",
        "цена розничная с учетом согласованной скидки",
        "вайлдберриз реализовал товар (пр)",
        "выручка", "revenue"
    };
    private static final String[] PURCHASE_HEADERS = {
        "purchase price", "закупка", "purchase_price", "закупочная цена"
    };
    private static final String[] LOGISTICS_HEADERS = {
        "logistics", "логистика", "logistics_cost", "услуги по доставке товара покупателю",
        "возмещение издержек по перевозке/по складским операциям с товаром"
    };
    private static final String[] MARKETING_HEADERS = {
        "marketing", "маркетинг", "marketing_cost", "реклама"
    };
    private static final String[] OTHER_EXPENSE_HEADERS = {
        "other", "прочие", "other_expenses", "прочие расходы", "штрафы",
        "общая сумма штрафов"
    };

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

                String name = getString(row, headerMap, formatter, NAME_HEADERS);
                String wbArticle = getString(row, headerMap, formatter, ARTICLE_HEADERS);
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
                product.setWbArticle(wbArticle);
                product.setWbBarcode(getString(row, headerMap, formatter, BARCODE_HEADERS));
                product.setCategory(getString(row, headerMap, formatter, CATEGORY_HEADERS));
                product.setBrand(getString(row, headerMap, formatter, BRAND_HEADERS));
                product.setStockQuantity(getInteger(row, headerMap, formatter, STOCK_HEADERS));

                if ((product.getName() == null || product.getName().isBlank()) && wbArticle != null) {
                    product.setName("Товар " + wbArticle);
                }

                boolean isNew = product.getId() == null;

                BigDecimal plannedPrice = getDecimal(row, headerMap, formatter, PRICE_HEADERS);
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

                BigDecimal purchase = getDecimal(row, headerMap, formatter, PURCHASE_HEADERS);
                if (purchase == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не заполнено поле «Закупка».", row.getRowNum() + 1));
                }
                product.setPurchasePrice(purchase);

                BigDecimal logistics = getDecimal(row, headerMap, formatter, LOGISTICS_HEADERS);
                if (logistics == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указаны логистические расходы.", row.getRowNum() + 1));
                }
                product.setLogisticsCost(logistics);

                BigDecimal marketing = getDecimal(row, headerMap, formatter, MARKETING_HEADERS);
                if (marketing == null) {
                    warnings.add(String.format(Locale.ROOT,
                            "Строка %d: не указаны маркетинговые расходы.", row.getRowNum() + 1));
                }
                product.setMarketingCost(marketing);

                BigDecimal other = getDecimal(row, headerMap, formatter, OTHER_EXPENSE_HEADERS);
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
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                String formatted = formatter.formatCellValue(cell);
                return formatted != null ? formatted.trim() : null;
            }
            double numericValue = cell.getNumericCellValue();
            long rounded = Math.round(numericValue);
            if (Double.compare(numericValue, rounded) == 0) {
                return String.valueOf(rounded);
            }
        } else if (cell.getCellType() == CellType.FORMULA) {
            return getStringFromFormula(cell, formatter);
        }
        String value = formatter.formatCellValue(cell);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        } else if (value.endsWith(",0")) {
            value = value.substring(0, value.length() - 2);
        }
        return value.isEmpty() ? null : value;
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
        } else if (cell.getCellType() == CellType.FORMULA) {
            return switch (cell.getCachedFormulaResultType()) {
                case NUMERIC -> (int) cell.getNumericCellValue();
                case STRING -> parseInteger(formatter.formatCellValue(cell));
                default -> null;
            };
        }
        String text = formatter.formatCellValue(cell);
        return parseInteger(text);
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
        } else if (cell.getCellType() == CellType.FORMULA) {
            return switch (cell.getCachedFormulaResultType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue()).setScale(2, RoundingMode.HALF_UP);
                case STRING -> parseDecimal(formatter.formatCellValue(cell));
                default -> null;
            };
        }
        String text = formatter.formatCellValue(cell);
        return parseDecimal(text);
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

    private String getStringFromFormula(Cell cell, DataFormatter formatter) {
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> {
                double numericValue = cell.getNumericCellValue();
                long rounded = Math.round(numericValue);
                if (Double.compare(numericValue, rounded) == 0) {
                    yield String.valueOf(rounded);
                }
                String formatted = formatter.formatCellValue(cell);
                yield formatted != null && !formatted.isBlank() ? formatted.trim() : null;
            }
            case STRING -> {
                String formatted = formatter.formatCellValue(cell);
                yield formatted != null && !formatted.isBlank() ? formatted.trim() : null;
            }
            default -> null;
        };
    }

    private Integer parseInteger(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace("\u00A0", "").replace(" ", "").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace("%", "")
                .replace(",", ".")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
