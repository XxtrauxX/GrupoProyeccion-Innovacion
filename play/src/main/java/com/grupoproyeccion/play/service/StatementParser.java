package com.grupoproyeccion.play.service;

import com.grupoproyeccion.play.model.AccountBancolombia;
import java.util.List;

public interface StatementParser {

    
    boolean supports(String text);


    List<AccountBancolombia> parse(String text);
}