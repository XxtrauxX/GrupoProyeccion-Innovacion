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
        String cleanText = text.replace("0,0000", "0.0000")
                               .replace("1,8594", "1.8594")
                               .replace("24,7421", "24.7421");
        
        List<String> lines = Arrays.asList(cleanText.split("\\r?\\n"));
        
        String currentCurrency = "";
        boolean inTransactionSection = false;
        StringBuilder currentBlock = new StringBuilder();
        
        Pattern startOfTransactionPattern = Pattern.compile("^(?:[A-Z]?\\d{5,6})?\\s*(\\d{2}/\\d{2}/\\d{4})");

        for (String line : lines) {
            String trimmedLine = line.trim();
            String upperCaseLine = trimmedLine.toUpperCase();

            if (upperCaseLine.contains("ESTADO DE CUENTA EN: DOLARES")) {
                if (currentBlock.length() > 0) {
                    processBlock(currentBlock.toString(), currentCurrency, transactions);
                }
                currentBlock.setLength(0);
                currentCurrency = "USD";
                inTransactionSection = false;
                continue;
            }
            if (upperCaseLine.contains("ESTADO DE CUENTA EN: PESOS")) {
                if (currentBlock.length() > 0) {
                    processBlock(currentBlock.toString(), currentCurrency, transactions);
                }
                currentBlock.setLength(0);
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
                if (currentBlock.length() > 0) {
                    processBlock(currentBlock.toString(), currentCurrency, transactions);
                }
                currentBlock.setLength(0);
                currentBlock.append(trimmedLine);
            } else if (currentBlock.length() > 0) {
                 currentBlock.append(" ").append(trimmedLine);
            }
        }
        
        if (currentBlock.length() > 0) {
            processBlock(currentBlock.toString(), currentCurrency, transactions);
        }

        return transactions;
    }

    private void processBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();

        // --- Estrategia C: Truncamiento Inteligente ---
        Pattern endOfTransactionPattern = Pattern.compile(".*(\\d{1,2}/\\d{1,2}|-?[\\d,.]+\\d-?)$");
        Matcher endMatcher = endOfTransactionPattern.matcher(cleanedBlock);
        if (endMatcher.find()) {
            // Cortamos el string justo después del último número o cuota encontrada.
            cleanedBlock = cleanedBlock.substring(0, endMatcher.end());
        }
        
        List<Pattern> patterns = Arrays.asList(
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([\\d,.-]+)\\s+[\\d.]+\\s+[\\d.]+\\s+([\\d,.-]+)\\s+([\\d,.-]+)\\s+\\d{1,2}/\\d{1,2}"),
            Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\s+(.+?)\\s+([\\d,.-]+)\\s+([\\d,.-]+)\\s+([\\d,.-]+)$")
        );

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(cleanedBlock);
            if (matcher.find()) {
                try {
                    String fecha = matcher.group(1);
                    String descripcion = matcher.group(2).trim();
                    double valorOriginal = parseFlexibleDouble(matcher.group(3));
                    double cargosYAbonos = parseFlexibleDouble(matcher.group(4));
                    double saldoADiferir = parseFlexibleDouble(matcher.group(5));

                    transactions.add(new CreditCardTransaction(fecha, descripcion, valorOriginal, cargosYAbonos, saldoADiferir, currency));
                    return;
                } catch (Exception e) {
                    // Intencionalmente vacío
                }
            }
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
        
        cleanStr = cleanStr.replace(",", "");
        
        double value = Double.parseDouble(cleanStr);
        return isNegative ? -value : value;
    }
}