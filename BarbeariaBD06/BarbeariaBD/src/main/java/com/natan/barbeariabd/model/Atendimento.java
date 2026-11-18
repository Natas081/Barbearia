package com.natan.barbeariabd.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Atendimento(Integer id, Integer agendamento, String cliente, String barbeiro, String statusAgenda,
                          LocalDateTime inicio, LocalDateTime fim, BigDecimal total, String classificacao) {
    public record Item(int servico, String nome, BigDecimal preco) {}
}
