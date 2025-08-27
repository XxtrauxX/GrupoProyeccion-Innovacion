package com.grupoproyeccion.play.modules.bancolombia_creditcard;

import com.grupoproyeccion.play.model.CreditCardTransaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class BancolombiaCreditCardService {

    public List<CreditCardTransaction> parse(String text) {
        List<CreditCardTransaction> transactions = new ArrayList<>();
        
        // Limpieza inicial para normalizar tasas que usan comas y podrían confundir al parser
        String cleanText = text.replace("0,0000", "0.0000")
                               .replace("1,8594", "1.8594")
                               .replace("24,7421", "24.7421");
        
        String[] sections = cleanText.split("ESTADO DE CUENTA EN: PESOS");
        String usdSection = sections[0];
        String copSection = sections.length > 1 ? sections[1] : "";

        processSection(usdSection, "USD", transactions);
        processSection(copSection, "COP", transactions);

        return transactions;
    }

    private void processSection(String sectionText, String currency, List<CreditCardTransaction> transactions) {
        List<String> lines = Arrays.asList(sectionText.split("\\r?\\n"));
        // Patrón para encontrar una fecha DD/MM/YYYY, que es el ancla más fiable.
        Pattern startOfTransactionPattern = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
        StringBuilder currentBlock = new StringBuilder();
        boolean inTransactionSection = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!inTransactionSection && (trimmedLine.contains("Fecha de Transacción") || trimmedLine.contains("Descripción"))) {
                inTransactionSection = true;
                continue;
            }
            if (!inTransactionSection || trimmedLine.isEmpty() || trimmedLine.startsWith("DCF:defensor") || trimmedLine.startsWith("Pag.")) continue;

            Matcher startMatcher = startOfTransactionPattern.matcher(trimmedLine);
            if (startMatcher.find()) {
                if (currentBlock.length() > 0) {
                    processBlock(currentBlock.toString(), currency, transactions);
                }
                currentBlock.setLength(0);
                currentBlock.append(trimmedLine);
            } else if (currentBlock.length() > 0) {
                // Unimos solo si la línea no es basura del pie de página
                if (!trimmedLine.contains("CUPÓN DE PAGO") && !trimmedLine.contains("SEÑOR (A):")) {
                     currentBlock.append(" ").append(trimmedLine);
                }
            }
        }
        // Procesar el último bloque que quedó pendiente
        if (currentBlock.length() > 0) {
            processBlock(currentBlock.toString(), currency, transactions);
        }
    }

    private void processBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();
        String[] parts = cleanedBlock.split(" ");

        if (parts.length < 4) return; // No tiene suficientes componentes para ser una transacción

        try {
            String fecha = "";
            int descriptionStartIndex = -1;
            // Encontrar la fecha, que es nuestro punto de anclaje
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].matches("\\d{2}/\\d{2}/\\d{4}")) {
                    fecha = parts[i];
                    descriptionStartIndex = i + 1; // La descripción empieza justo después de la fecha
                    break;
                }
            }
            if (fecha.isEmpty()) return; // Si no hay fecha, no es una transacción

            String lastPart = parts[parts.length - 1];
            double saldoADiferir, cargosYAbonos, valorOriginal;
            int descriptionEndIndex;

            // Estrategia de componentes: analizamos desde el final para identificar el tipo de transacción
            if (lastPart.contains("/")) { // Caso 1: Es una compra con cuotas (formato largo)
                saldoADiferir = parseFlexibleDouble(parts[parts.length - 2]);
                cargosYAbonos = parseFlexibleDouble(parts[parts.length - 3]);
                valorOriginal = parseFlexibleDouble(parts[parts.length - 6]);
                descriptionEndIndex = parts.length - 6;
            } else { // Caso 2: Es un abono, pago u otro cargo sin cuotas (formato corto)
                saldoADiferir = parseFlexibleDouble(parts[parts.length - 1]);
                cargosYAbonos = parseFlexibleDouble(parts[parts.length - 2]);
                valorOriginal = parseFlexibleDouble(parts[parts.length - 3]);
                descriptionEndIndex = parts.length - 3;
            }
            
            String descripcion = Arrays.stream(parts, descriptionStartIndex, descriptionEndIndex)
                                       .collect(Collectors.joining(" "));

            transactions.add(new CreditCardTransaction(fecha, descripcion, valorOriginal, cargosYAbonos, saldoADiferir, currency));

        } catch (Exception e) {
            // System.err.println("Omitiendo bloque por error de formato: " + cleanedBlock);
        }
    }

    private double parseFlexibleDouble(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty() || numberStr.trim().equals("-")) {
            return 0.0;
        }
        String cleanStr = numberStr.trim();
        
        boolean isNegative = cleanStr.endsWith("-");
        if(isNegative) {
            cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
        }
        
        // Lógica unificada para ambos formatos: elimina comas de miles y confía en el punto como decimal.
        cleanStr = cleanStr.replace(",", "");
        
        double value = Double.parseDouble(cleanStr);
        return isNegative ? -value : value;
    }
}