package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BancolombiaService implements StatementParser {

    @Override
    public boolean supports(String text) {
       
        List<String> headerLines = Arrays.asList(text.split("\\r?\\n")).subList(0, Math.min(15, text.split("\\r?\\n").length));
        String headerText = String.join(" ", headerLines).toUpperCase().replaceAll("\\s+", " ");

        
        if (headerText.contains("BANCOLOMBIA")) {
            
            return headerText.contains("CUENTA CORRIENTE") || headerText.contains("CUENTA DE AHORROS");
        }
        
        
        return false;
    }

    @Override
    public List<AccountBancolombia> parse(String text) {
       
        List<AccountBancolombia> transactions = new ArrayList<>();
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));
        String year = findStatementYear(lines);
        boolean isTransactionSection = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.toUpperCase().contains("FECHA") && trimmedLine.toUpperCase().contains("DESCRIPCIÓN")) {
                isTransactionSection = true;
                continue;
            }
            if (trimmedLine.toUpperCase().contains("FIN ESTADO DE CUENTA")) {
                break;
            }
            if (!isTransactionSection || trimmedLine.isEmpty()) {
                continue;
            }
            Pattern pattern = Pattern.compile("^(\\d{1,2}/\\d{1,2})\\s+(.*?)\\s+(-?[\\d,.]+)\\s+(-?[\\d,.]+)$");
            Matcher matcher = pattern.matcher(trimmedLine);
            if (matcher.find()) {
                try {
                    String date = matcher.group(1) + "/" + year;
                    String description = matcher.group(2).trim();
                    String valueString = matcher.group(3);
                    double value = parseFlexibleDouble(valueString); 
                    transactions.add(new AccountBancolombia(date, description, value));
                } catch (Exception e) {
                    // Ignorar
                }
            }
        }
        return transactions;
    }

    private double parseFlexibleDouble(String numberStr) {
        if (numberStr == null || numberStr.trim().isEmpty()) {
            return 0.0;
        }
        String cleanStr = numberStr.trim();
        if (cleanStr.contains(",") && cleanStr.contains(".")) {
            int lastComma = cleanStr.lastIndexOf(',');
            int lastDot = cleanStr.lastIndexOf('.');
            if (lastDot > lastComma) {
                return Double.parseDouble(cleanStr.replace(",", ""));
            } else {
                return Double.parseDouble(cleanStr.replace(".", "").replace(",", "."));
            }
        } else if (cleanStr.contains(",")) {
            return Double.parseDouble(cleanStr.replace(",", "."));
        }
        return Double.parseDouble(cleanStr);
    }

    private String findStatementYear(List<String> lines) {
        for (String line : lines) {
            if (line.contains("HASTA:")) {
                Pattern pattern = Pattern.compile("(\\d{4})");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) return matcher.group(1);
            }
        }
        return String.valueOf(java.time.Year.now().getValue());
    }
}