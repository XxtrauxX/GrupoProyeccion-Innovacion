package com.grupoproyeccion.play.modules.bancolombia_creditcard;

import com.grupoproyeccion.play.model.CreditCardTransaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BancolombiaCreditCardExcelService {

    public ByteArrayInputStream transactionsToExcel(List<CreditCardTransaction> transactions) throws IOException {
        final String[] HEADERS = { "Fecha", "Descripción", "Valor Original", "Cargos y Abonos", "Saldo a Diferir" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // 1. Filtrar transacciones por moneda para crear hojas separadas
            List<CreditCardTransaction> usdTransactions = transactions.stream()
                .filter(t -> "USD".equals(t.getMoneda()))
                .collect(Collectors.toList());
            if (!usdTransactions.isEmpty()) {
                createSheet(workbook, "Dólares", HEADERS, usdTransactions);
            }

            List<CreditCardTransaction> copTransactions = transactions.stream()
                .filter(t -> "COP".equals(t.getMoneda()))
                .collect(Collectors.toList());
            if (!copTransactions.isEmpty()) {
                createSheet(workbook, "Pesos", HEADERS, copTransactions);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void createSheet(Workbook workbook, String sheetName, String[] headers, List<CreditCardTransaction> data) {
        Sheet sheet = workbook.createSheet(sheetName);

        // --- Creación de Encabezados ---
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