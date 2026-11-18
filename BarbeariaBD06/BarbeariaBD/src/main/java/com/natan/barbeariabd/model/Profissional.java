package com.natan.barbeariabd.model;

import java.math.BigDecimal;

public record Profissional(String cpf, String nome, String rua, String numero, Barbeiro barbeiro, BigDecimal bonificacao) {
    public record Barbeiro(String especialidade, String horario, String supervisor) {}

    public String funcao() {
        if (barbeiro != null) {
            return bonificacao == null ? "Barbeiro" : "Barbeiro e gerente";
        }
        return bonificacao == null ? "Sem especialização" : "Gerente";
    }
}
