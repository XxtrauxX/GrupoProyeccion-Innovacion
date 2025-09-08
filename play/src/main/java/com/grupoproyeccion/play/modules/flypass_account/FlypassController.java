package com.grupoproyeccion.play.modules.flypass_account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final N8nIntegrationService n8nIntegrationService;
    private final FlypassExcelService excelService;
    private final ObjectMapper objectMapper; // Para convertir JSON a Objetos

    public FlypassController(N8nIntegrationService n8nIntegrationService,
                             FlypassExcelService excelService,
                             ObjectMapper objectMapper) {
        this.n8nIntegrationService = n8nIntegrationService;
        this.excelService = excelService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/process")
    public ResponseEntity<?> processFlypassStatement(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo.");
        }
        try {
            // 1. Enviar el PDF a n8n y recibir el JSON como respuesta.
            String jsonResponse = n8nIntegrationService.processPdfViaN8n(file);

            // 2. Convertir el String JSON a una lista de objetos FlypassTransaction.
            List<FlypassTransaction> transactions = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("La IA no encontró transacciones válidas en el documento.");
            }

            // 3. Generar el archivo Excel con los datos limpios.
            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=Reporte_Flypass_IA.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error en la integración con n8n: " + e.getMessage());
        }
    }
}