package com.grupoproyeccion.play.controller;

import com.grupoproyeccion.play.service.BancolombiaService;
import com.grupoproyeccion.play.service.ExcelService;
import com.grupoproyeccion.play.service.PdfService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Controlador que maneja la API para la subida y procesamiento de archivos.
 */
@CrossOrigin // Habilita CORS para permitir peticiones desde el frontend (Live Server)
@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    // Inyección de los servicios necesarios
    private final PdfService pdfService;
    private final BancolombiaService bancolombiaService;
    private final ExcelService excelService;

    /**
     * Constructor para la inyección de dependencias.
     * Spring se encarga de crear y pasar las instancias de los servicios.
     */
    public FileUploadController(PdfService pdfService, BancolombiaService bancolombiaService, ExcelService excelService) {
        this.pdfService = pdfService;
        this.bancolombiaService = bancolombiaService;
        this.excelService = excelService;
    }

    /**
     * Endpoint para subir un archivo PDF, procesarlo y generar un archivo Excel.
     *
     * @param file El archivo PDF enviado en la petición.
     * @param password La contraseña del PDF (parámetro opcional).
     * @return Una respuesta HTTP que contiene el archivo Excel para descargar o un mensaje de error.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileAndGenerateExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "password", required = false) String password) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Por favor, selecciona un archivo para subir.");
        }

        try {
            // --- Flujo del Proceso ---

            // 1. Extraer texto del PDF, pasando el archivo y la contraseña.
            String extractedText = pdfService.extractTextFromPdf(file, password);

            // 2. Procesar el texto para obtener una lista de transacciones.
            var transactions = bancolombiaService.processText(extractedText);
            
            if (transactions.isEmpty()) {
                return ResponseEntity.badRequest().body("No se encontraron transacciones válidas en el documento. Verifique el formato.");
            }

            // 3. Generar el archivo Excel en memoria a partir de las transacciones.
            ByteArrayInputStream excelFile = excelService.transactionsToExcel(transactions);

            // 4. Preparar la respuesta HTTP para que el navegador inicie la descarga.
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=ConciliacionBancolombia.xlsx");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(excelFile));

        } catch (IOException e) {
            // Manejo específico para errores comunes de PDF, como contraseña incorrecta.
            if (e.getMessage() != null && e.getMessage().contains("Password required")) {
                 return ResponseEntity.badRequest().body("El PDF está protegido. Por favor, introduce la contraseña.");
            }
             if (e.getMessage() != null && e.getMessage().contains("incorrect password")) {
                return ResponseEntity.badRequest().body("La contraseña del PDF es incorrecta.");
            }
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error al leer el archivo PDF.");
        } catch (Exception e) {
            // Captura de cualquier otro error inesperado durante el proceso.
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Ocurrió un error inesperado al procesar el archivo: " + e.getMessage());
        }
    }
}