package com.grupoproyeccion.play.modules.bancolombia_creditcard;

import com.grupoproyeccion.play.model.CreditCardTransaction;
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
@RequestMapping("/api/bancolombia-credit-card") // Endpoint único y dedicado
public class BancolombiaCreditCardController {

    // 1. Inyectamos únicamente los servicios de este módulo
    private final BancolombiaCreditCardPdfService pdfService;
    private final BancolombiaCreditCardService creditCardService;
    private final BancolombiaCreditCardExcelService excelService;

    public BancolombiaCreditCardController(
            BancolombiaCreditCardPdfService pdfService,
            BancolombiaCreditCardService creditCardService,
            BancolombiaCreditCardExcelService excelService) {
        this.pdfService = pdfService;
        this.creditCardService = creditCardService;
        this.excelService = excelService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processCreditCardStatement(@RequestParam("file") MultipartFile file,
                                                        @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo.");
        }
        try {
            // 2. Orquestación del flujo de trabajo aislado
            String extractedText = pdfService.extractTextFromPdf(file, password);
            List<CreditCardTransaction> transactions = creditCardService.parse(extractedText);

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones de tarjeta de crédito válidas.");
            }

            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Reporte_TC_Bancolombia.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al procesar el extracto de tarjeta de crédito.");
        }
    }
}