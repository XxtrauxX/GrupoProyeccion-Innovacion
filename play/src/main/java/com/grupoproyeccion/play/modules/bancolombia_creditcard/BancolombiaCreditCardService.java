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
        boolean isConsolidado = false; // Flag para el nuevo formato
        StringBuilder currentBlock = new StringBuilder();

        Pattern startOfTransactionPattern = Pattern.compile("^[A-Z]?\\d{5,6}|^000000");

        for (String line : lines) {
            String trimmedLine = line.trim();
            String upperCaseLine = trimmedLine.toUpperCase();

            // --- MEJORA 1: Detección del tipo de extracto ---
            if (upperCaseLine.contains("ESTADO DE CUENTA CONSOLIDADO")) {
                isConsolidado = true;
            }

            if (upperCaseLine.contains("ESTADO DE CUENTA EN: DOLARES") || upperCaseLine.contains("ESTADO DE CUENTA CONSOLIDADO EN: DOLARES")) {
                processAndClearBlock(currentBlock, currentCurrency, transactions, isConsolidado);
                currentCurrency = "USD";
                inTransactionSection = false;
                continue;
            }
            if (upperCaseLine.contains("ESTADO DE CUENTA EN: PESOS") || upperCaseLine.contains("ESTADO DE CUENTA CONSOLIDADO EN: PESOS")) {
                processAndClearBlock(currentBlock, currentCurrency, transactions, isConsolidado);
                currentCurrency = "COP";
                inTransactionSection = false;
                continue;
            }

            if (!inTransactionSection && (upperCaseLine.contains("FECHA DE TRANSACCIÓN") || upperCaseLine.contains("DESCRIPCIÓN"))) {
                inTransactionSection = true;
                continue;
            }
            if (!inTransactionSection || trimmedLine.isEmpty()) continue;

            // La lógica de agrupación de bloques ahora funciona para ambos formatos
            if (startOfTransactionPattern.matcher(trimmedLine).find() && !isConsolidado) {
                 processAndClearBlock(currentBlock, currentCurrency, transactions, isConsolidado);
                 currentBlock.append(trimmedLine);
            } else {
                 currentBlock.append(" ").append(trimmedLine);
            }
        }

        processAndClearBlock(currentBlock, currentCurrency, transactions, isConsolidado);
        return transactions;
    }

    private void processAndClearBlock(StringBuilder block, String currency, List<CreditCardTransaction> transactions, boolean isConsolidado) {
        if (block.length() > 0) {
            if (isConsolidado) {
                processConsolidadoBlock(block.toString(), currency, transactions);
            } else {
                processStandardBlock(block.toString(), currency, transactions);
            }
            block.setLength(0);
        }
    }

    // --- MEJORA 2: Nuevo método exclusivo para el formato "Consolidado" ---
    private void processConsolidadoBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();

        // Patrón diseñado para la estructura de la tabla consolidada
        Pattern transactionPattern = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s+" +      // Grupo 1: Fecha
            "(.+?)\\s+" +                       // Grupo 2: Descripción
            "([\\d,.-]+)\\s+" +                 // Grupo 3: Valor Total
            "([\\d,.-]+)"                       // Grupo 4: Cuota del Mes
        );

        Matcher matcher = transactionPattern.matcher(cleanedBlock);

        while (matcher.find()) {
            try {
                String fecha = matcher.group(1);
                String descripcion = matcher.group(2).trim();
                // Ignoramos las líneas de resumen que no son transacciones reales
                if (descripcion.equalsIgnoreCase("Saldo Anterior") || descripcion.equalsIgnoreCase("Saldo en Mora")) {
                    continue;
                }
                
                double valorTotal = parseFlexibleDouble(matcher.group(3));
                double cuotaDelMes = parseFlexibleDouble(matcher.group(4));

                // Mapeamos los nuevos campos a nuestro modelo existente
                transactions.add(new CreditCardTransaction(fecha, descripcion, valorTotal, cuotaDelMes, 0.0, currency));

            } catch (Exception e) {
                System.err.println("Error procesando línea en bloque consolidado: " + matcher.group(0));
            }
        }
    }

    // Método anterior, ahora renombrado para claridad
    private void processStandardBlock(String block, String currency, List<CreditCardTransaction> transactions) {
        String cleanedBlock = block.replaceAll("\\s+", " ").trim();
        // ... (El resto del método processBlock anterior se mantiene igual aquí)
        Pattern descFirstPattern = Pattern.compile(
            "^(?!.*\\d{2}/\\d{2}/\\d{4}.*\\d{2}/\\d{2}/\\d{4})" + "(.+?)\\s+" + "(\\d{2}/\\d{2}/\\d{4})\\s+" + "([\\d,.-]+)\\s+" + "([\\d,.-]+)\\s+" + "([\\d,.-]+)$"
        );
        Pattern dateFirstPattern = Pattern.compile(
            "(\\d{2}/\\d{2}/\\d{4})\\s+" + "(.+?)\\s+" + "([\\d,.-]+)?\\s*" + "(?:[\\d.,]+\\s+){0,2}" + "([\\d,.-]+)\\s+" + "([\\d,.-]+)" + "(?:\\s+\\d{1,2}/\\d{1,2})?"
        );
        Matcher matcher = descFirstPattern.matcher(cleanedBlock);
        if (matcher.matches()) {
            try {
                transactions.add(new CreditCardTransaction(matcher.group(2), matcher.group(1).trim(), parseFlexibleDouble(matcher.group(3)), parseFlexibleDouble(matcher.group(4)), parseFlexibleDouble(matcher.group(5)), currency));
                return;
            } catch (Exception e) {
                 System.err.println("Error procesando bloque con formato especial: " + cleanedBlock);
            }
        }
        matcher = dateFirstPattern.matcher(cleanedBlock);
        while (matcher.find()) {
            try {
                transactions.add(new CreditCardTransaction(matcher.group(1), matcher.group(2).trim(), parseFlexibleDouble(matcher.group(3)), parseFlexibleDouble(matcher.group(4)), parseFlexibleDouble(matcher.group(5)), currency));
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