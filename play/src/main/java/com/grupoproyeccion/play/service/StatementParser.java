package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import java.util.List;

public interface StatementParser {

    /**
     * Verifica si este parser es compatible con el texto del extracto.
     */
    boolean supports(String text);

    /**
     * Procesa el texto y lo convierte en una lista de transacciones.
     */
    List<AccountBancolombia> parse(String text);
}