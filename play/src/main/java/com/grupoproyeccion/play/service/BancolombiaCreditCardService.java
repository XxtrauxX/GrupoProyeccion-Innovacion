package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.CreditCardTransaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BancolombiaCreditCardService {

    // Este parser no necesita el método supports() porque será llamado directamente.

    public List<CreditCardTransaction> parse(String text) {
        List<CreditCardTransaction> transactions = new ArrayList<>();

        // 1. Dividir el texto en secciones de DÓLARES y PESOS
        String[] sectionsByPesos = text.split("ESTADO DE CUENTA EN: PESOS");
        String usdSection = sectionsByPesos[0];
        String copSection = sectionsByPesos.length > 1 ? sectionsByPesos[1] : "";

        // 2. Procesar cada sección por separado
        processSection(usdSection, "USD", transactions);
        processSection(copSection, "COP", transactions);

        return transactions;
    }

    private void processSection(String sectionText, String currency, List<CreditCardTransaction> transactions) {
        List<String> lines = Arrays.asList(sectionText.split("\\r?\\n"));
        
        // Patrón para identificar el inicio de una línea de transacción válida (DD/MM/YYYY)
        Pattern startOfTransactionPattern = Pattern.compile("^\\d{2}/\\d{2}/\\d{4}");
        
        StringBuilder currentBlock = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (startOfTransactionPattern.matcher(trimmedLine).find()) {
                if (currentBlock.length() > 0) {
                    processBlock(currentBlock.toString(), currency, transactions);
                }
                currentBlock.setLength(0);
                currentBlock.append(trimmedLine);
            } else if (currentBlock.length() > 0) {
                // Si no es un nuevo inicio, es parte del bloque actual (descripción multilínea).
                currentBlock.append(" ").append(trimmedLine);
            }
        }
        // Procesar el último bloque pendiente.
        if (currentBlock.length() > 0) {
            processBlock(currentBlock.toString(), currency, transactions);
        }
    }

    private void processBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();
        
        // Regex para capturar los datos de una transacción de tarjeta de crédito
        Pattern pattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([\\d,.-]+)\\s+[\\d,.]+\\s+[\\d,.]+\\s+([\\d,.-]+)\\s+([\\d,.-]+)");
        if (currency.equals("COP")) { // El formato de pesos tiene menos columnas de tasas
             pattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([\\d,.-]+)\\s+[\\d,.]+\\s+[\\d,.]+\\s+([\\d,.-]+)\\s+([\\d,.-]+)");
        }
        
        Matcher matcher = pattern.matcher(cleanedBlock);

        if (matcher.find()) {
            try {
                String fecha = matcher.group(1);
                String descripcion = matcher.group(2).trim();
                double valorOriginal = parseFlexibleDouble(matcher.group(3));
                double cargosYAbonos = parseFlexibleDouble(matcher.group(4));
                double saldoADiferir = parseFlexibleDouble(matcher.group(5));

                transactions.add(new CreditCardTransaction(fecha, descripcion, valorOriginal, cargosYAbonos, saldoADiferir, currency));
            } catch (Exception e) {
                 System.err.println("Omitiendo bloque de TC por error de formato: " + cleanedBlock);
            }
        }
    }

    private double parseFlexibleDouble(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty() || numberStr.trim().equals("-")) {
            return 0.0;
        }
        String cleanStr = numberStr.trim().replace(".", "").replace(",", ".");
        if (cleanStr.endsWith("-")) {
            return -Double.parseDouble(cleanStr.substring(0, cleanStr.length() - 1));
        }
        return Double.parseDouble(cleanStr);
    }
}