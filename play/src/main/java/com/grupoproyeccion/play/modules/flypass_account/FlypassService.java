 package com.grupoproyeccion.play.modules.flypass_account;

import com.grupoproyeccion.play.model.FlypassTransaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class FlypassService {

    public List<FlypassTransaction> parse(String text) {
        List<FlypassTransaction> transactions = new ArrayList<>();
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));

        boolean inTransactionSection = false;
        Pattern datePattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})");
        StringBuilder currentBlock = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.contains("Fecha aplicación") && trimmedLine.contains("Tipo movimiento")) {
                inTransactionSection = true;
                continue;
            }
            if (trimmedLine.startsWith("Contáctenos") || trimmedLine.contains("Total recargas")) {
                break;
            }
            if (!inTransactionSection || trimmedLine.isEmpty()) {
                continue;
            }

            // Si la línea empieza con una fecha, es el inicio de una nueva transacción.
            // Procesamos el bloque anterior y empezamos uno nuevo.
            if (datePattern.matcher(trimmedLine).find() && currentBlock.length() > 0) {
                processTransactionBlock(currentBlock.toString(), transactions);
                currentBlock.setLength(0);
            }
            currentBlock.append(" ").append(trimmedLine);
        }

        // Procesamos el último bloque que quedó pendiente.
        if (currentBlock.length() > 0) {
            processTransactionBlock(currentBlock.toString(), transactions);
        }

        transactions.sort(Comparator.comparing(FlypassTransaction::getFecha));
        return transactions;
    }

    private void processTransactionBlock(String block, List<FlypassTransaction> transactions) {
        String cleanBlock = block.replaceAll("\\s+", " ").trim();

        Pattern fullDatePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})");
        Matcher dateMatcher = fullDatePattern.matcher(cleanBlock);
        if (!dateMatcher.find()) return; // Si no hay fecha completa, no es una transacción válida.
        String fecha = dateMatcher.group(1);

        // Extraemos todos los valores monetarios del bloque.
        List<Double> values = new ArrayList<>();
        Pattern valuePattern = Pattern.compile("(-?\\$\\s?[\\d.,]+)");
        Matcher valueMatcher = valuePattern.matcher(cleanBlock);
        while (valueMatcher.find()) {
            values.add(parseValue(valueMatcher.group(1)));
        }

        if (cleanBlock.contains("PEAJES")) {
            String placa = "";
            Pattern placaPattern = Pattern.compile("([A-Z]{3}[\\d]{2,3}[A-Z]?)");
            Matcher placaMatcher = placaPattern.matcher(cleanBlock);
            if (placaMatcher.find()) {
                placa = placaMatcher.group(1);
            }

            String descripcion = "Peaje";
            Pattern descPattern = Pattern.compile("Cobro (.+?) " + (placa.isEmpty() ? "" : Pattern.quote(placa)));
            Matcher descMatcher = descPattern.matcher(cleanBlock);
            if (descMatcher.find()) {
                descripcion += ": " + descMatcher.group(1).trim();
            }

            // En peajes, el valor es el único negativo de la lista.
            double valor = values.stream().filter(v -> v < 0).findFirst().orElse(0.0);
            if (valor != 0) {
                transactions.add(new FlypassTransaction(fecha, descripcion, placa, "N/A", valor));
            }

        } else if (cleanBlock.contains("RECARGAS")) {
            String descripcion = "Recarga";
            Pattern descPattern = Pattern.compile("canal (.+?) RECARGAS");
            Matcher descMatcher = descPattern.matcher(cleanBlock);
            if (descMatcher.find()) {
                descripcion += ": " + descMatcher.group(1).trim().split("Empresas")[0].trim();
            }

            // En recargas, hay dos valores positivos. El primero es el servicio, el segundo el valor.
            List<Double> positiveValues = values.stream().filter(v -> v >= 0).collect(Collectors.toList());
            if (positiveValues.size() >= 2) {
                String servicio = String.format("$%,.0f", positiveValues.get(0));
                double valor = positiveValues.get(1);
                transactions.add(new FlypassTransaction(fecha, descripcion, "", servicio, valor));
            }
        }
    }

    private double parseValue(String valueStr) {
        // Lógica corregida para manejar correctamente los valores.
        String cleanValue = valueStr.replaceAll("[$.]", "").replaceAll("\\s", "");
        return Double.parseDouble(cleanValue);
    }
}