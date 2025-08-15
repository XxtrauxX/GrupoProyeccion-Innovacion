package com.grupoproyeccion.play.service;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;

@Component
public class ParserFactory {

    private final List<StatementParser> parsers;

    public ParserFactory(List<StatementParser> parsers) {
        this.parsers = parsers;
    }

    public Optional<StatementParser> getParser(String text) {
        return parsers.stream()
                .filter(parser -> parser.supports(text))
                .findFirst();
    }
}