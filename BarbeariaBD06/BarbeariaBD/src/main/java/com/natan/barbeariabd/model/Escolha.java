package com.natan.barbeariabd.model;

public record Escolha<T>(T id, String nome) {
    @Override
    public String toString() {
        return nome;
    }
}
