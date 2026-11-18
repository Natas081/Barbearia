package com.natan.barbeariabd.model;

import java.time.LocalDateTime;
import java.util.List;

public record Agendamento(Integer id, String clienteCpf, String cliente, String barbeiroCpf, String barbeiro,
                          LocalDateTime horario, String status, Integer atendimento) {
    public static final String CONCLUIDO = "Concluido";
    public static final List<String> STATUS = List.of("Agendado", "Confirmado", CONCLUIDO, "Cancelado");
}
