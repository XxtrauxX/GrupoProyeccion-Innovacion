package com.grupoproyeccion.play.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBancolombia {

    private String date;
    private String description;
    private double value;
    private String branch; 

    public AccountBancolombia(String date, String description, double value) {
        this(date, description, value, "");
    }
}