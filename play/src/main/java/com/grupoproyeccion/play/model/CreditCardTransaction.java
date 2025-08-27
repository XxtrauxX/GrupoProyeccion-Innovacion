package com.grupoproyeccion.play.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardTransaction {
    private String fecha;
    private String descripcion;
    private double valorOriginal;
    private double cargosYAbonos;
    private double saldoADiferir;
    private String moneda; // "USD" o "COP"
}