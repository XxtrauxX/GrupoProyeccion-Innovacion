package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DaviviendaService implements StatementParser {

    private static final List<String> KNOWN_BRANCHES;

    static {
        List<String> branches = Arrays.asList(
            "Compras y Pagos PSE",
            "PORTAL PYMES INTERBANCARI",
            "PORTAL PYMES",
            "BTA PROCESOS ESP.",
            "BTA PROCESOS ESP",
            "0000",
            "PROCESOS ACH"
        );
        KNOWN_BRANCHES = branches.stream()
                                 .sorted(Comparator.comparingInt(String::length).reversed())
                                 .collect(Collectors.toList());
    }

    @Override
    public boolean supports(String text) {
        String normalizedText = text.toUpperCase().replaceAll("\\s+", " ");
        return normalizedText.contains("DAVIVIENDA") && normalizedText.contains("CUENTA DE AHORROS");
    }

    @Override
    public List<AccountBancolombia> parse(String text) {
        List<AccountBancolombia> transactions = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        String year = findStatementYear(Arrays.asList(lines));
        Pattern startOfTransactionPattern = Pattern.compile("^\\s*\\d{2}\\s+\\d{2}\\s+\\$");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (startOfTransactionPattern.matcher(line).find()) {
                StringBuilder fullTransactionText = new StringBuilder(line);
                int nextIndex = i + 1;
                while (nextIndex < lines.length && !isStartOfNewTransaction(lines[nextIndex]) && !isFooterOrHeader(lines[nextIndex])) {
                    fullTransactionText.append(" ").append(lines[nextIndex].trim());
                    nextIndex++;
                }

                String fullLine = fullTransactionText.toString();

                try {
                    Pattern datePattern = Pattern.compile("^(\\d{2})\\s+(\\d{2})");
                    Matcher dateMatcher = datePattern.matcher(fullLine);
                    if (!dateMatcher.find()) continue;
                    String date = dateMatcher.group(1) + "/" + dateMatcher.group(2) + "/" + year;

                    Pattern valuePattern = Pattern.compile("\\$ ([\\d,.]+)([+-]?)");
                    Matcher valueMatcher = valuePattern.matcher(fullLine);
                    if (!valueMatcher.find()) continue;
                    double value = parseValue(valueMatcher.group(0));

                    String branch = "";
                    for (String knownBranch : KNOWN_BRANCHES) {
                        if (fullLine.contains(knownBranch)) {
                            branch = knownBranch;
                            break;
                        }
                    }

                    
                    String description = fullLine
                            .replace(dateMatcher.group(0), "")
                            .replace(valueMatcher.group(0), "")
                            .replace(branch, "")
                            
                            .replaceAll("\\s+", " ")
                            .trim();
                    
                    transactions.add(new AccountBancolombia(date, description, value, branch));

                } catch (Exception e) {
                    System.err.println("Ignorando línea Davivienda por error de formato: " + fullLine);
                }
                i = nextIndex - 1;
            }
        }
        return transactions;
    }
    
   
    private double parseValue(String valuePart) {
        Matcher valueMatcher = Pattern.compile("[\\d,.]+").matcher(valuePart);
        valueMatcher.find();
        double value = Double.parseDouble(valueMatcher.group(0).replace(",", ""));
        if (valuePart.endsWith("-")) {
            value = -value;
        }
        return value;
    }

    private boolean isFooterOrHeader(String line) {
        String upperCaseLine = line.toUpperCase();
        return upperCaseLine.contains("ESTE PRODUCTO CUENTA CON SEGURO") ||
               upperCaseLine.contains("DEFENSOR DEL CONSUMIDOR FINANCIERO") ||
               upperCaseLine.contains("BANCO DAVIVIENDA S.A NIT") ||
               upperCaseLine.contains("CLASE DE MOVIMIENTO");
    }

    private boolean isStartOfNewTransaction(String line) {
        return Pattern.compile("^\\s*\\d{2}\\s+\\d{2}\\s+\\$").matcher(line.trim()).find();
    }
    
    private String findStatementYear(List<String> lines) {
        for (String line : lines) {
            if (line.contains("INFORME DEL MES")) {
                Pattern pattern = Pattern.compile("(\\d{4})");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) return matcher.group(1);
            }
        }
        return String.valueOf(java.time.Year.now().getValue());
    }
}