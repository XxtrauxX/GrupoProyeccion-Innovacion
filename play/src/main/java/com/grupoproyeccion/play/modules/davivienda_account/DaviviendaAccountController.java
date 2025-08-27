package com.grupoproyeccion.play.modules.davivienda_account;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/davivienda-account") // Endpoint único para este módulo
public class DaviviendaAccountController {

    private final DaviviendaAccountPdfService pdfService;
    private final DaviviendaAccountService accountService;
    private final DaviviendaAccountExcelService excelService;

    public DaviviendaAccountController(DaviviendaAccountPdfService pdfService, DaviviendaAccountService accountService, DaviviendaAccountExcelService excelService) {
        this.pdfService = pdfService;
        this.accountService = accountService;
        this.excelService = excelService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processDaviviendaAccount(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo.");
        }
        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);
            
            // Verificamos si el parser soporta este texto
            if (!accountService.supports(extractedText)) {
                return ResponseEntity.badRequest().body("El formato del extracto PDF no parece ser de Davivienda.");
            }

            List<AccountBancolombia> transactions = accountService.parse(extractedText);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones válidas.");
            }

            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Conciliacion_Davivienda.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.sheet"))
                    .body(new InputStreamResource(excelFile));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error inesperado procesando el extracto.");
        }
    }
}