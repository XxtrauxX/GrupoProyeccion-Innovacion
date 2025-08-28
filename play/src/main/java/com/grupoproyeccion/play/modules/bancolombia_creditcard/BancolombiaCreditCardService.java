package com.grupoproyeccion.play.modules.bancolombia_creditcard;

import com.grupoproyeccion.play.model.CreditCardTransaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BancolombiaCreditCardService {

    public List<CreditCardTransaction> parse(String text) {
        List<CreditCardTransaction> transactions = new ArrayList<>();
        String cleanText = text.replaceAll("(\\d),(\\d{4})", "$1.$2");

        List<String> lines = Arrays.asList(cleanText.split("\\r?\\n"));

        String currentCurrency = "";
        boolean inTransactionSection = false;
        StringBuilder currentBlock = new StringBuilder();

        Pattern startOfTransactionPattern = Pattern.compile("^[A-Z]?\\d{5,6}|^000000");

        for (String line : lines) {
            String trimmedLine = line.trim();
            String upperCaseLine = trimmedLine.toUpperCase();

            if (upperCaseLine.contains("ESTADO DE CUENTA EN: DOLARES")) {
                processAndClearBlock(currentBlock, currentCurrency, transactions);
                currentCurrency = "USD";
                inTransactionSection = false;
                continue;
            }
            if (upperCaseLine.contains("ESTADO DE CUENTA EN: PESOS")) {
                processAndClearBlock(currentBlock, currentCurrency, transactions);
                currentCurrency = "COP";
                inTransactionSection = false;
                continue;
            }

            if (!inTransactionSection && (upperCaseLine.contains("FECHA DE TRANSACCIÓN") || upperCaseLine.contains("DESCRIPCIÓN"))) {
                inTransactionSection = true;
                continue;
            }
            if (!inTransactionSection || trimmedLine.isEmpty()) continue;

           
            if (startOfTransactionPattern.matcher(trimmedLine).find()) {
                processAndClearBlock(currentBlock, currentCurrency, transactions);
                currentBlock.append(trimmedLine);
            } else if (currentBlock.length() > 0) {
                currentBlock.append(" ").append(trimmedLine);
            } else {
                
                currentBlock.append(trimmedLine);
            }
        }

        processAndClearBlock(currentBlock, currentCurrency, transactions);
        return transactions;
    }

    private void processAndClearBlock(StringBuilder block, String currency, List<CreditCardTransaction> transactions) {
        if (block.length() > 0) {
            processBlock(block.toString(), currency, transactions);
            block.setLength(0);
        }
    }

    private void processBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();

        

        
        Pattern descFirstPattern = Pattern.compile(
            "^(?!.*\\d{2}/\\d{2}/\\d{4}.*\\d{2}/\\d{2}/\\d{4})" + // Asegura que no haya dos fechas
            "(.+?)\\s+" +                           // Grupo 1: Descripción
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +          // Grupo 2: Fecha
            "([\\d,.-]+)\\s+" +                     // Grupo 3: Valor Original
            "([\\d,.-]+)\\s+" +                     // Grupo 4: Cargos y Abonos
            "([\\d,.-]+)$"                          // Grupo 5: Saldo a Diferir
        );

        
        Pattern dateFirstPattern = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +          // Grupo 1: Fecha
            "(.+?)\\s+" +                           // Grupo 2: Descripción
            "([\\d,.-]+)?\\s*" +                    // Grupo 3: Valor Original (opcional)
            "(?:[\\d.,]+\\s+){0,2}" +                // Tasas opcionales
            "([\\d,.-]+)\\s+" +                     // Grupo 4: Cargos y Abonos
            "([\\d,.-]+)" +                         // Grupo 5: Saldo a Diferir
            "(?:\\s+\\d{1,2}/\\d{1,2})?"            // Cuotas opcionales
        );

        
        Matcher matcher = descFirstPattern.matcher(cleanedBlock);
        if (matcher.matches()) {
            try {
                String descripcion = matcher.group(1).trim();
                String fecha = matcher.group(2);
                double valorOriginal = parseFlexibleDouble(matcher.group(3));
                double cargosYAbonos = parseFlexibleDouble(matcher.group(4));
                double saldoADiferir = parseFlexibleDouble(matcher.group(5));
                transactions.add(new CreditCardTransaction(fecha, descripcion, valorOriginal, cargosYAbonos, saldoADiferir, currency));
                return; // Procesado, salimos
            } catch (Exception e) {
                 System.err.println("Error procesando bloque con formato especial: " + cleanedBlock);
            }
        }

        
        matcher = dateFirstPattern.matcher(cleanedBlock);
        while (matcher.find()) {
            try {
                String fecha = matcher.group(1);
                String descripcion = matcher.group(2).trim();
                double valorOriginal = parseFlexibleDouble(matcher.group(3));
                double cargosYAbonos = parseFlexibleDouble(matcher.group(4));
                double saldoADiferir = parseFlexibleDouble(matcher.group(5));
                transactions.add(new CreditCardTransaction(fecha, descripcion, valorOriginal, cargosYAbonos, saldoADiferir, currency));
            } catch (Exception e) {
                System.err.println("Error procesando sub-bloque con formato estándar: " + matcher.group(0));
            }
        }
    }

    private double parseFlexibleDouble(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty() || numberStr.trim().equals("-")) {
            return 0.0;
        }
        String cleanStr = numberStr.trim();
        boolean isNegative = cleanStr.endsWith("-");
        if (isNegative) {
            cleanStr = cleanStr.substring(0, cleanStr.length() - 1);
        }
        cleanStr = cleanStr.replace(",", "");
        try {
            return isNegative ? -Double.parseDouble(cleanStr) : Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}