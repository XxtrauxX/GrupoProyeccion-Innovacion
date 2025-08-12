package com.grupoproyeccion.play.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfService {

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            
            PDFTextStripper pdfStripper = new PDFTextStripper();
            
            
            pdfStripper.setSortByPosition(true);
            
            String text = pdfStripper.getText(document);
            return text;
        }
    }
}