package com.grupoproyeccion.play.controller;

import com.grupoproyeccion.play.service.BancolombiaService;
import com.grupoproyeccion.play.service.ExcelService;
import com.grupoproyeccion.play.service.PdfService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final PdfService pdfService;
    private final BancolombiaService bancolombiaService;
    private final ExcelService excelService;

    public FileUploadController(PdfService pdfService, BancolombiaService bancolombiaService, ExcelService excelService) {
        this.pdfService = pdfService;
        this.bancolombiaService = bancolombiaService;
        this.excelService = excelService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileAndGenerateExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }

        try {
            // 1. Extraer texto del PDF
            String extractedText = pdfService.extractTextFromPdf(file);

            // 2. Procesar el texto para obtener transacciones
            var transactions = bancolombiaService.processText(extractedText);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.ok("No se encontraron transacciones en el documento.");
            }

            // 3. Generar el archivo Excel en memoria
            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);

            // 4. Preparar la respuesta para descargar el archivo
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=ConciliacionBancolombia.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al procesar el archivo: " + e.getMessage());
        }
    }
}