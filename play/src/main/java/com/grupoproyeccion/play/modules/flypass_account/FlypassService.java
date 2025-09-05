package com.grupoproyeccion.play.modules.flypass_account;

import com.grupoproyeccion.play.model.FlypassTransaction;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FlypassService {

    public List<FlypassTransaction> parse(String text) {
        List<FlypassTransaction> transactions = new ArrayList<>();
        List<String> lines = Arrays.asList(text.split("\\r?\\n"));

        boolean inTransactionSection = false;
        Pattern datePattern = Pattern.compile("^\\$?\\s*-?\\$?\\s*(\\d{2}/\\d{2}/\\d{4})");
        StringBuilder currentBlock = new StringBuilder();
        String previousLine = "";

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmedLine = line.trim();

            // Detectar inicio de la sección de transacciones
            if (trimmedLine.contains("Fecha") && trimmedLine.contains("aplicación")) {
                inTransactionSection = true;
                continue;
            }
            
            // Detectar fin de la sección
            if (trimmedLine.startsWith("Total consumos") || trimmedLine.startsWith("Contáctenos")) {
                // Procesar el último bloque si existe
                if (currentBlock.length() > 0) {
                    processTransactionBlock(currentBlock.toString(), transactions);
                }
                break;
            }
            
            if (!inTransactionSection) {
                continue;
            }

            // Verificar si la línea actual o la anterior comienza con un valor monetario seguido de fecha
            boolean startsWithAmount = trimmedLine.matches("^\\$\\s*[\\d.,]+\\s+\\$.*") || 
                                      trimmedLine.matches("^-\\$\\s*[\\d.,]+\\s+\\$.*");
            boolean hasDateAtStart = datePattern.matcher(trimmedLine).find();
            
            // Si encontramos una nueva transacción (línea con monto inicial o fecha)
            if ((startsWithAmount || hasDateAtStart) && currentBlock.length() > 0) {
                // Procesar el bloque anterior
                processTransactionBlock(currentBlock.toString(), transactions);
                currentBlock.setLength(0);
            }
            
            // Agregar la línea al bloque actual
            if (!trimmedLine.isEmpty()) {
                currentBlock.append(" ").append(trimmedLine);
            }
            
            previousLine = trimmedLine;
        }

        // Procesar el último bloque si quedó algo
        if (currentBlock.length() > 0) {
            processTransactionBlock(currentBlock.toString(), transactions);
        }

        // Ordenar por fecha
        transactions.sort(Comparator.comparing(FlypassTransaction::getFecha));
        return transactions;
    }

    private void processTransactionBlock(String block, List<FlypassTransaction> transactions) {
        String cleanBlock = block.replaceAll("\\s+", " ").trim();
        
        // Buscar la fecha completa con hora
        Pattern fullDatePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})");
        Matcher dateMatcher = fullDatePattern.matcher(cleanBlock);
        if (!dateMatcher.find()) return;
        String fecha = dateMatcher.group(1);

        // Determinar el tipo de transacción
        if (cleanBlock.contains("PEAJES")) {
            processPeajeTransaction(cleanBlock, fecha, transactions);
        } else if (cleanBlock.contains("RECARGAS")) {
            processRecargaTransaction(cleanBlock, fecha, transactions);
        }
    }

    private void processPeajeTransaction(String block, String fecha, List<FlypassTransaction> transactions) {
        // Buscar placa
        String placa = "";
        Pattern placaPattern = Pattern.compile("([A-Z]{3}\\d{2,3}[A-Z]?)");
        Matcher placaMatcher = placaPattern.matcher(block);
        if (placaMatcher.find()) {
            placa = placaMatcher.group(1);
        }

        // Buscar descripción del peaje
        String descripcion = "Peaje";
        Pattern descPattern = Pattern.compile("Cobro\\s+([^\\s]+(?:\\s+[^\\s]+)*?)\\s+(?:[A-Z]{3}\\d|\\d{5})");
        Matcher descMatcher = descPattern.matcher(block);
        if (descMatcher.find()) {
            descripcion = "Peaje: " + descMatcher.group(1).trim();
        }

        // Buscar valor (negativo)
        Pattern valuePattern = Pattern.compile("-\\$\\s*([\\d.,]+)");
        Matcher valueMatcher = valuePattern.matcher(block);
        if (valueMatcher.find()) {
            double valor = -parseValue(valueMatcher.group(1));
            transactions.add(new FlypassTransaction(fecha, descripcion, placa, "N/A", valor));
        }
    }

    private void processRecargaTransaction(String block, String fecha, List<FlypassTransaction> transactions) {
        // Buscar descripción del canal
        String descripcion = "Recarga";
        Pattern descPattern = Pattern.compile("canal\\s+([^\\d]+?)(?:\\s+\\d|\\s+RECARGAS|\\s*$)");
        Matcher descMatcher = descPattern.matcher(block);
        if (descMatcher.find()) {
            String canal = descMatcher.group(1).trim();
            // Limpiar el canal de texto adicional
            canal = canal.replace("RECARGAS", "").trim();
            if (canal.contains("Servicio")) {
                canal = canal.substring(canal.indexOf("Servicio")).trim();
            }
            descripcion = "Recarga: " + canal;
        }

        // Buscar número de transacción (servicio)
        String servicio = "";
        Pattern servicioPattern = Pattern.compile("(\\d{8})");
        Matcher servicioMatcher = servicioPattern.matcher(block);
        if (servicioMatcher.find()) {
            servicio = servicioMatcher.group(1);
        }

        // Buscar valores monetarios positivos
        Pattern valuePattern = Pattern.compile("\\$\\s*([\\d.,]+)");
        Matcher valueMatcher = valuePattern.matcher(block);
        List<Double> valores = new ArrayList<>();
        
        while (valueMatcher.find()) {
            String valueStr = valueMatcher.group(1);
            // Evitar parsear números de transacción como valores
            if (!valueStr.matches("\\d{8}")) {
                valores.add(parseValue(valueStr));
            }
        }

        // El valor de la recarga es típicamente el primer valor grande encontrado
        // Los valores de 700, 1600, etc. son costos de servicio
        Double valorRecarga = null;
        Double costoServicio = null;
        
        for (Double val : valores) {
            if (val == 700 || val == 1600) {
                costoServicio = val;
            } else if (val > 1000 && valorRecarga == null) {
                valorRecarga = val;
            }
        }

        // Si no encontramos un valor grande, tomar el primer valor que no sea 700/1600
        if (valorRecarga == null && !valores.isEmpty()) {
            for (Double val : valores) {
                if (val != 700 && val != 1600) {
                    valorRecarga = val;
                    break;
                }
            }
        }

        // Si encontramos valor, agregar la transacción
        if (valorRecarga != null) {
            String servicioStr = costoServicio != null ? String.format("$%,.0f", costoServicio) : servicio;
            transactions.add(new FlypassTransaction(fecha, descripcion, "", servicioStr, valorRecarga));
        }
    }

    private double parseValue(String valueStr) {
        // Eliminar puntos de miles y comas decimales
        String cleanValue = valueStr.replaceAll("\\.", "").replaceAll(",", ".");
        try {
            return Double.parseDouble(cleanValue);
        } catch (NumberFormatException e) {
            // Si falla, intentar sin decimales
            cleanValue = valueStr.replaceAll("[.,]", "");
            return Double.parseDouble(cleanValue);
        }
    }
}