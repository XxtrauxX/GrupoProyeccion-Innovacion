package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    public ByteArrayInputStream transactionsToExcel(List<AccountBancolombia> transactions) throws IOException {
        
        // --- LÓGICA INTELIGENTE PARA DETERMINAR LAS COLUMNAS ---
        // 1. Verificamos si alguna de las transacciones tiene datos en el campo 'branch'.
        boolean includeBranchColumn = transactions.stream()
                .anyMatch(t -> t.getBranch() != null && !t.getBranch().isEmpty());

        // 2. Definimos los encabezados dinámicamente.
        String[] HEADERS;
        if (includeBranchColumn) {
            HEADERS = new String[]{ "Fecha", "Descripción", "Valor", "Oficina" };
        } else {
            HEADERS = new String[]{ "Fecha", "Descripción", "Valor" };
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // La lógica de creación de hojas ahora usará los encabezados dinámicos
            createSheet(workbook, "General", HEADERS, transactions);
            createSheet(workbook, "Ordenado", HEADERS, transactions.stream().sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));
            createSheet(workbook, "Ingresos", HEADERS, transactions.stream().filter(t -> t.getValue() > 0).sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));
            createSheet(workbook, "Salidas", HEADERS, transactions.stream().filter(t -> t.getValue() <= 0).sorted((a, b) -> a.getDescription().compareTo(b.getDescription())).collect(Collectors.toList()));
            
            Map<String, Double> incomeGrouped = transactions.stream()
                .filter(t -> t.getValue() > 0)
                .collect(Collectors.groupingBy(AccountBancolombia::getDescription, Collectors.summingDouble(AccountBancolombia::getValue)));
            List<AccountBancolombia> incomeList = incomeGrouped.entrySet().stream()
                .map(entry -> new AccountBancolombia(null, entry.getKey(), entry.getValue(), null))
                .sorted((a,b) -> a.getDescription().compareTo(b.getDescription()))
                .collect(Collectors.toList());
            createSheet(workbook, "Ingresos Agrupados", HEADERS, incomeList);

            Map<String, Double> expenseGrouped = transactions.stream()
                .filter(t -> t.getValue() <= 0)
                .collect(Collectors.groupingBy(AccountBancolombia::getDescription, Collectors.summingDouble(AccountBancolombia::getValue)));
            List<AccountBancolombia> expenseList = expenseGrouped.entrySet().stream()
                .map(entry -> new AccountBancolombia(null, entry.getKey(), entry.getValue(), null))
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
            
            // --- LÓGICA INTELIGENTE PARA ESCRIBIR LA CELDA ---
            // 3. Solo intentamos escribir la columna "Oficina" si el encabezado existe.
            if (headers.length > 3) {
                row.createCell(3).setCellValue(transaction.getBranch());
            }
        }
        
        for(int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}