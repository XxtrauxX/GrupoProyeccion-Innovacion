package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import com.grupoproyeccion.play.model.CreditCardTransaction; // 1. Importar el nuevo modelo
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
public class ExcelService {

    // --- MÉTODO ORIGINAL PARA CUENTAS DE AHORROS/CORRIENTE (SIN CAMBIOS) ---
    public ByteArrayInputStream transactionsToExcel(List<AccountBancolombia> transactions) throws IOException {
        boolean includeBranchColumn = transactions.stream()
                .anyMatch(t -> t.getBranch() != null && !t.getBranch().isEmpty());

        String[] HEADERS;
        if (includeBranchColumn) {
            HEADERS = new String[]{ "Fecha", "Descripción", "Valor", "Oficina" };
        } else {
            HEADERS = new String[]{ "Fecha", "Descripción", "Valor" };
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
            
            if (headers.length > 3) {
                row.createCell(3).setCellValue(transaction.getBranch());
            }
        }
        
        for(int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // --- NUEVO MÉTODO PARA TARJETAS DE CRÉDITO ---
    public ByteArrayInputStream creditCardTransactionsToExcel(List<CreditCardTransaction> transactions) throws IOException {
        final String[] HEADERS = { "Fecha", "Descripción", "Valor Original", "Cargos y Abonos", "Saldo a Diferir" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 2. Filtrar transacciones por moneda para crear hojas separadas
            List<CreditCardTransaction> usdTransactions = transactions.stream()
                .filter(t -> "USD".equals(t.getMoneda()))
                .collect(Collectors.toList());
            if (!usdTransactions.isEmpty()) {
                createCreditCardSheet(workbook, "Dólares", HEADERS, usdTransactions);
            }

            List<CreditCardTransaction> copTransactions = transactions.stream()
                .filter(t -> "COP".equals(t.getMoneda()))
                .collect(Collectors.toList());
            if (!copTransactions.isEmpty()) {
                createCreditCardSheet(workbook, "Pesos", HEADERS, copTransactions);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // --- NUEVO MÉTODO AUXILIAR PARA LAS HOJAS DE TARJETA DE CRÉDITO ---
    private void createCreditCardSheet(Workbook workbook, String sheetName, String[] headers, List<CreditCardTransaction> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        
        // --- Creación de Encabezados (similar al método original) ---
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

        // --- Estilo de Celda para Moneda ---
        CellStyle currencyCellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("$ #,##0.00"));

        // --- Llenado de Filas con Datos ---
        int rowIdx = 1;
        for (CreditCardTransaction transaction : data) {
            Row row = sheet.createRow(rowIdx++);
            
            row.createCell(0).setCellValue(transaction.getFecha());
            row.createCell(1).setCellValue(transaction.getDescripcion());
            
            // Aplicar formato de moneda a las celdas numéricas
            Cell valorOriginalCell = row.createCell(2);
            valorOriginalCell.setCellValue(transaction.getValorOriginal());
            valorOriginalCell.setCellStyle(currencyCellStyle);

            Cell cargosAbonosCell = row.createCell(3);
            cargosAbonosCell.setCellValue(transaction.getCargosYAbonos());
            cargosAbonosCell.setCellStyle(currencyCellStyle);
            
            Cell saldoDiferirCell = row.createCell(4);
            saldoDiferirCell.setCellValue(transaction.getSaldoADiferir());
            saldoDiferirCell.setCellStyle(currencyCellStyle);
        }
        
        // --- Auto-ajuste de Columnas ---
        for(int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}