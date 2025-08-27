package com.grupoproyeccion.play.modules.bancolombia_account;

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
@RequestMapping("/api/bancolombia-account")
public class BancolombiaAccountController {

    private final BancolombiaAccountPdfService pdfService;
    private final BancolombiaAccountService accountService;
    private final BancolombiaAccountExcelService excelService;

    public BancolombiaAccountController(BancolombiaAccountPdfService pdfService, BancolombiaAccountService accountService, BancolombiaAccountExcelService excelService) {
        this.pdfService = pdfService;
        this.accountService = accountService;
        this.excelService = excelService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processBancolombiaAccount(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }
        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);
            List<AccountBancolombia> transactions = accountService.processText(extractedText);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones válidas en el documento. Verifique el formato.");
            }

            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Conciliacion_Bancolombia.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Password required")) {
                 return ResponseEntity.badRequest().body("El PDF está protegido. Por favor, introduce la contraseña.");
            }
             if (e.getMessage() != null && e.getMessage().contains("incorrect password")) {
                 return ResponseEntity.badRequest().body("La contraseña del PDF es incorrecta.");
            }
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al leer el archivo PDF.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar el archivo: " + e.getMessage());
        }
    }
}