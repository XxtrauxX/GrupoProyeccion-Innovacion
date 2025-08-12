package com.grupoproyeccion.play.controller;

import com.grupoproyeccion.play.service.PdfService; // 1. Importar el servicio
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    
    private final PdfService pdfService;

    
    public FileUploadController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }

        try {
           
            String extractedText = pdfService.extractTextFromPdf(file);

           
            System.out.println("----- INICIO DEL TEXTO EXTRAÍDO DEL PDF -----");
            System.out.println(extractedText);
            System.out.println("----- FIN DEL TEXTO EXTRAÍDO DEL PDF -----");

            String message = "Archivo '" + file.getOriginalFilename() + "' procesado exitosamente.";
            return ResponseEntity.ok(message);

        } catch (Exception e) {
            String errorMessage = "Falló el procesamiento del PDF: " + e.getMessage();
            return ResponseEntity.internalServerError().body(errorMessage);
        }
    }
}