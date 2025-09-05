package com.grupoproyeccion.play.modules.flypass_account;

import com.grupoproyeccion.play.model.FlypassTransaction;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class FlypassExcelService {

    public ByteArrayInputStream transactionsToExcel(List<FlypassTransaction> transactions) throws IOException {
        // Se añade la columna "Servicio"
        final String[] HEADERS = { "Fecha", "Descripción", "Placa", "Servicio", "Valor" };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Movimientos Flypass");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerCellStyle);
            }

            CellStyle currencyCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("$ #,##0"));

            int rowIdx = 1;
            for (FlypassTransaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getFecha());
                row.createCell(1).setCellValue(tx.getDescripcion());
                row.createCell(2).setCellValue(tx.getPlaca());
                row.createCell(3).setCellValue(tx.getServicio()); // Se añade el valor del servicio
                Cell valueCell = row.createCell(4); // La columna de valor ahora es la 4
                valueCell.setCellValue(tx.getValor());
                valueCell.setCellStyle(currencyCellStyle);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}