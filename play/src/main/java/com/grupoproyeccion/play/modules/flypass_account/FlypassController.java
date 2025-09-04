package com.grupoproyeccion.play.modules.flypass_account;

import com.grupoproyeccion.play.model.FlypassTransaction;
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
@RequestMapping("/api/flypass")
public class FlypassController {

    private final FlypassPdfService pdfService;
    private final FlypassService flypassService;
    private final FlypassExcelService excelService;

    public FlypassController(FlypassPdfService pdfService, FlypassService flypassService, FlypassExcelService excelService) {
        this.pdfService = pdfService;
        this.flypassService = flypassService;
        this.excelService = excelService;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processFlypassStatement(@RequestParam("file") MultipartFile file,
                                                     @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo.");
        }
        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);
            List<FlypassTransaction> transactions = flypassService.parse(extractedText);

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones válidas en el extracto de Flypass.");
            }

            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Reporte_Flypass.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al procesar el extracto de Flypass.");
        }
    }
}