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

            // --- MEJORA: Lógica para crear 6 hojas ---

            // 1. Filtramos las transacciones por moneda (USD y COP)
            List<CreditCardTransaction> usdTransactions = transactions.stream()
                .filter(t -> "USD".equals(t.getMoneda()))
                .collect(Collectors.toList());

            List<CreditCardTransaction> copTransactions = transactions.stream()
                .filter(t -> "COP".equals(t.getMoneda()))
                .collect(Collectors.toList());

            // 2. Creamos las hojas para DÓLARES si hay transacciones
            if (!usdTransactions.isEmpty()) {
                // Filtramos ingresos (pagos/abonos, que son valores negativos) y salidas (compras/cargos)
                List<CreditCardTransaction> usdIngresos = usdTransactions.stream()
                    .filter(t -> t.getCargosYAbonos() < 0)
                    .collect(Collectors.toList());
                List<CreditCardTransaction> usdSalidas = usdTransactions.stream()
                    .filter(t -> t.getCargosYAbonos() >= 0)
                    .collect(Collectors.toList());

                // Creamos las 3 hojas correspondientes
                createSheet(workbook, "Dólares", HEADERS, usdTransactions);
                createSheet(workbook, "Ingresos Dólares", HEADERS, usdIngresos);
                createSheet(workbook, "Salidas Dólares", HEADERS, usdSalidas);
            }

            // 3. Creamos las hojas para PESOS si hay transacciones
            if (!copTransactions.isEmpty()) {
                // Filtramos ingresos y salidas
                List<CreditCardTransaction> copIngresos = copTransactions.stream()
                    .filter(t -> t.getCargosYAbonos() < 0)
                    .collect(Collectors.toList());
                List<CreditCardTransaction> copSalidas = copTransactions.stream()
                    .filter(t -> t.getCargosYAbonos() >= 0)
                    .collect(Collectors.toList());

                // Creamos las 3 hojas correspondientes
                createSheet(workbook, "Pesos", HEADERS, copTransactions);
                createSheet(workbook, "Ingresos Pesos", HEADERS, copIngresos);
                createSheet(workbook, "Salidas Pesos", HEADERS, copSalidas);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    // El método createSheet no necesita cambios, lo reutilizamos
    private void createSheet(Workbook workbook, String sheetName, String[] headers, List<CreditCardTransaction> data) {
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
        for (CreditCardTransaction transaction : data) {
            Row row = sheet.createRow(rowIdx++);
            
            row.createCell(0).setCellValue(transaction.getFecha());
            row.createCell(1).setCellValue(transaction.getDescripcion());
            
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
        
        for(int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}