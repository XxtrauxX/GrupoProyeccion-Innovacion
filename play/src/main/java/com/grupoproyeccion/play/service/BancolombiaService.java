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
        String normalizedText = text.toUpperCase().replaceAll("\\s+", " ");
        return normalizedText.contains("BANCOLOMBIA") && normalizedText.contains("CUENTA CORRIENTE");
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
                    double value = Double.parseDouble(valueString.replace(",", ""));
                    transactions.add(new AccountBancolombia(date, description, value));
                } catch (NumberFormatException e) {
                    // Ignorar
                }
            }
        }
        return transactions;
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