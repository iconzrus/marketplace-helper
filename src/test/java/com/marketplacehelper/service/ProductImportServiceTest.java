package com.marketplacehelper.service;

import com.marketplacehelper.model.Product;
import com.marketplacehelper.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ProductImportService.class)
class ProductImportServiceTest {

    @Autowired
    private ProductImportService productImportService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        entityManager.flush();
    }

    @Test
    void shouldImportProductsUsingWildberriesHeaders() throws Exception {
        MockMultipartFile file = createWildberriesExcel();

        var result = productImportService.importFromExcel(file);

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getUpdated()).isEqualTo(0);
        assertThat(result.getSkipped()).isEqualTo(0);

        Product product = productRepository.findByWbArticle("186961443").orElseThrow();
        assertThat(product.getName()).isEqualTo("Чехол для рыболовного подсака 80*85 см чёрный");
        assertThat(product.getBrand()).isEqualTo("FISHING BAND");
        assertThat(product.getCategory()).isEqualTo("Другие принадлежности для рыбалки");
        assertThat(product.getWbBarcode()).isEqualTo("4673756010669");
        assertThat(product.getStockQuantity()).isEqualTo(1);
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("2059.55"));
    }

    @Test
    void shouldUpdateExistingProductWhenDuplicateArticleFound() throws Exception {
        MockMultipartFile file = createWildberriesExcel();
        productImportService.importFromExcel(file);

        MockMultipartFile secondFile = createWildberriesExcelWithUpdatedPrice();
        var result = productImportService.importFromExcel(secondFile);

        assertThat(result.getCreated()).isEqualTo(0);
        assertThat(result.getUpdated()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(0);

        Product product = productRepository.findByWbArticle("186961443").orElseThrow();
        assertThat(product.getPrice()).isEqualByComparingTo(new BigDecimal("2159.55"));
        assertThat(product.getStockQuantity()).isEqualTo(2);
    }

    private MockMultipartFile createWildberriesExcel() throws Exception {
        try (var workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Отчёт");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("№");
            header.createCell(1).setCellValue("Код номенклатуры");
            header.createCell(2).setCellValue("Артикул поставщика");
            header.createCell(3).setCellValue("Название");
            header.createCell(4).setCellValue("Предмет");
            header.createCell(5).setCellValue("Бренд");
            header.createCell(6).setCellValue("Баркод");
            header.createCell(7).setCellValue("Кол-во");
            header.createCell(8).setCellValue("Цена розничная");
            header.createCell(9).setCellValue("Вайлдберриз реализовал Товар (Пр)");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(1);
            row.createCell(1).setCellValue("186961443");
            row.createCell(2).setCellValue("ch5_bl");
            row.createCell(3).setCellValue("Чехол для рыболовного подсака 80*85 см чёрный");
            row.createCell(4).setCellValue("Другие принадлежности для рыбалки");
            row.createCell(5).setCellValue("FISHING BAND");
            row.createCell(6).setCellValue("4673756010669");
            row.createCell(7).setCellValue(1);
            row.createCell(8).setCellValue(2059.55);
            row.createCell(9).setCellValue(1692.26);

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "wildberries.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }

    private MockMultipartFile createWildberriesExcelWithUpdatedPrice() throws Exception {
        try (var workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Отчёт");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Код номенклатуры");
            header.createCell(1).setCellValue("Название");
            header.createCell(2).setCellValue("Предмет");
            header.createCell(3).setCellValue("Бренд");
            header.createCell(4).setCellValue("Баркод");
            header.createCell(5).setCellValue("Кол-во");
            header.createCell(6).setCellValue("Цена розничная");
            header.createCell(7).setCellValue("Закупка");
            header.createCell(8).setCellValue("Логистика");
            header.createCell(9).setCellValue("Маркетинг");
            header.createCell(10).setCellValue("Прочие расходы");

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("186961443");
            row.createCell(1).setCellValue("Чехол для рыболовного подсака 80*85 см чёрный");
            row.createCell(2).setCellValue("Другие принадлежности для рыбалки");
            row.createCell(3).setCellValue("FISHING BAND");
            row.createCell(4).setCellValue("4673756010669");
            row.createCell(5).setCellValue(2);
            row.createCell(6).setCellValue(2159.55);
            row.createCell(7).setCellValue(950.0);
            row.createCell(8).setCellValue(210.0);
            row.createCell(9).setCellValue(130.0);
            row.createCell(10).setCellValue(80.0);

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "wildberries-update.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
