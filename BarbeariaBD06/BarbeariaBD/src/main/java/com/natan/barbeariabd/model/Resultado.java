package com.natan.barbeariabd.model;

import java.util.List;

public record Resultado(List<String> colunas, List<Class<?>> tipos, List<Object[]> linhas) {}
