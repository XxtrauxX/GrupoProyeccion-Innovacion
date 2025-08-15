package com.grupoproyeccion.play.controller;

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

@CrossOrigin
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final PdfService pdfService;
    private final ExcelService excelService;
    private final ParserFactory parserFactory;

    public FileUploadController(PdfService pdfService, ExcelService excelService, ParserFactory parserFactory) {
        this.pdfService = pdfService;
        this.excelService = excelService;
        this.parserFactory = parserFactory;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileAndGenerateExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(name = "password", required = false) String password) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }

        try {
            String extractedText = pdfService.extractTextFromPdf(file, password);

            StatementParser parser = parserFactory.getParser(extractedText)
                    .orElseThrow(() -> new IllegalArgumentException("El formato del extracto PDF no es compatible."));

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
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar el archivo.");
        }
    }
}