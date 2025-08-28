package com.grupoproyeccion.play.modules.davivienda_account;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class DaviviendaAccountPdfService {
    public String extractTextFromPdf(MultipartFile file, String password) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream(), password)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setSortByPosition(true);
            return pdfStripper.getText(document);
        }
    }
}