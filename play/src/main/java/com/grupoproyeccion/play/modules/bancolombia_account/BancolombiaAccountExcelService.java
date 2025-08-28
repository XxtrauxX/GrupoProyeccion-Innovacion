package com.grupoproyeccion.play.modules.bancolombia_account;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BancolombiaAccountExcelService {

    public ByteArrayInputStream transactionsToExcel(List<AccountBancolombia> transactions) throws IOException {
        String[] HEADERS = { "Fecha", "Descripción", "Valor" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // --- Lógica para crear las 6 hojas ---
            createSheet(workbook, "General", HEADERS, transactions);
            createSheet(workbook, "Ordenado", HEADERS, transactions.stream().sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));
            createSheet(workbook, "Ingresos", HEADERS, transactions.stream().filter(t -> t.getValue() > 0).sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));
            createSheet(workbook, "Salidas", HEADERS, transactions.stream().filter(t -> t.getValue() <= 0).sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));

            // Ingresos Agrupados
            Map<String, Double> incomeGrouped = transactions.stream()
                .filter(t -> t.getValue() > 0)
                .collect(Collectors.groupingBy(AccountBancolombia::getDescription, Collectors.summingDouble(AccountBancolombia::getValue)));
            List<AccountBancolombia> incomeList = incomeGrouped.entrySet().stream()
                .map(entry -> new AccountBancolombia("", entry.getKey(), entry.getValue()))
                .sorted((a,b) -> a.getDescription().compareTo(b.getDescription()))
                .collect(Collectors.toList());
            createSheet(workbook, "Ingresos Agrupados", HEADERS, incomeList);

            // Salidas Agrupadas
            Map<String, Double> expenseGrouped = transactions.stream()
                .filter(t -> t.getValue() <= 0)
                .collect(Collectors.groupingBy(AccountBancolombia::getDescription, Collectors.summingDouble(AccountBancolombia::getValue)));
            List<AccountBancolombia> expenseList = expenseGrouped.entrySet().stream()
                .map(entry -> new AccountBancolombia("", entry.getKey(), entry.getValue()))
                 .sorted((a,b) -> a.getDescription().compareTo(b.getDescription()))
                .collect(Collectors.toList());
            createSheet(workbook, "Salidas Agrupadas", HEADERS, expenseList);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSheet(Workbook workbook, String sheetName, String[] headers, List<AccountBancolombia> data) {
        Sheet sheet = workbook.createSheet(sheetName);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerCellStyle);
        }

        CellStyle currencyCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("$ #,##0.00"));

        int rowIdx = 1;
        for (AccountBancolombia transaction : data) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(transaction.getDate());
            row.createCell(1).setCellValue(transaction.getDescription());
            
            Cell valueCell = row.createCell(2);
            valueCell.setCellValue(transaction.getValue());
            valueCell.setCellStyle(currencyCellStyle);
        }
        
        for(int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}