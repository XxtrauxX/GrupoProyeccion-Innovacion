package com.grupoproyeccion.play.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlypassTransaction {
    private String fecha;
    private String descripcion;
    private String placa;
    private String servicio;
    private double valor;
}