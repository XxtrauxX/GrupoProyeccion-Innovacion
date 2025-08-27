package com.grupoproyeccion.play.controller;

import com.grupoproyeccion.play.model.CreditCardTransaction;
import com.grupoproyeccion.play.service.BancolombiaCreditCardService;
import com.grupoproyeccion.play.service.ExcelService;
import com.grupoproyeccion.play.service.ParserFactory;
import com.grupoproyeccion.play.service.PdfService;
import com.grupoproyeccion.play.service.StatementParser;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final PdfService pdfService;
    private final ExcelService excelService;
    private final ParserFactory parserFactory;
    // 1. Inyectamos el nuevo servicio específico para tarjetas de crédito
    private final BancolombiaCreditCardService creditCardService;

    public FileUploadController(PdfService pdfService, ExcelService excelService, ParserFactory parserFactory, BancolombiaCreditCardService creditCardService) {
        this.pdfService = pdfService;
        this.excelService = excelService;
        this.parserFactory = parserFactory;
        this.creditCardService = creditCardService; // 2. Lo añadimos al constructor
    }

    // --- CARRIL 1: FUNCIONALIDAD ORIGINAL PARA CUENTAS DE AHORROS/CORRIENTE ---
    @PostMapping("/upload-account")
    public ResponseEntity<?> uploadAccountStatement(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }

        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);

            // La ParserFactory sigue manejando la lógica para Davivienda y Bancolombia (Cuentas)
            StatementParser parser = parserFactory.getParser(extractedText)
                    .orElseThrow(() -> new IllegalArgumentException("El formato del extracto de cuenta no es compatible."));

            var transactions = parser.parse(extractedText);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones válidas en el documento.");
            }

            var excelFile = excelService.transactionsToExcel(transactions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=ConciliacionBancaria.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar el extracto de cuenta.");
        }
    }

    // --- CARRIL 2: NUEVA FUNCIONALIDAD PARA TARJETAS DE CRÉDITO ---
    @PostMapping("/upload-credit-card")
    public ResponseEntity<?> uploadCreditCardStatement(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo.");
        }
        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);

            // Llamamos directamente al nuevo servicio, aislando la lógica
            List<CreditCardTransaction> transactions = creditCardService.parse(extractedText);

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones de tarjeta de crédito válidas.");
            }

            // Llamamos al nuevo método en ExcelService
            var excelFile = excelService.creditCardTransactionsToExcel(transactions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=ReporteTarjetaCredito.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al procesar el extracto de tarjeta de crédito.");
        }
    }
}