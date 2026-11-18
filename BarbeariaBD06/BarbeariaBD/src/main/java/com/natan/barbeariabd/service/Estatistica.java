package com.natan.barbeariabd.service;

import com.natan.barbeariabd.model.Dashboard.Visita;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public final class Estatistica {
    private static final MathContext PRECISAO = MathContext.DECIMAL128;

    private Estatistica() {}

    public record Medidas(int n, BigDecimal soma, BigDecimal media, BigDecimal mediana,
                          List<BigDecimal> modas, BigDecimal variancia, BigDecimal desvio,
                          BigDecimal minimo, BigDecimal maximo) {}

    public static Medidas calcular(List<BigDecimal> valores) {
        var ordenados = valores.stream().map(BigDecimal::stripTrailingZeros).sorted().toList();
        int n = ordenados.size();
        var soma = ordenados.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (n == 0) {
            return new Medidas(0, soma, null, null, List.of(), null, null, null, null);
        }

        var media = soma.divide(BigDecimal.valueOf(n), PRECISAO);
        var mediana = ordenados.get((n - 1) / 2).add(ordenados.get(n / 2)).divide(BigDecimal.valueOf(2));
        var frequencias = ordenados.stream().collect(Collectors.groupingBy(
                v -> v, LinkedHashMap::new, Collectors.counting()));
        long frequenciaMaxima = frequencias.values().stream().mapToLong(Long::longValue).max().orElse(0);
        var modas = frequencias.entrySet().stream()
                .filter(e -> frequenciaMaxima > 1 && e.getValue() == frequenciaMaxima)
                .map(Map.Entry::getKey).toList();
        var variancia = ordenados.stream().map(v -> v.subtract(media).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(n), PRECISAO);
        return new Medidas(n, soma, media, mediana, modas, variancia, variancia.sqrt(PRECISAO),
                ordenados.getFirst(), ordenados.getLast());
    }

    public static OptionalDouble pearson(List<Visita> visitas) {
        if (visitas.size() < 3) {
            return OptionalDouble.empty();
        }
        double mediaX = visitas.stream().mapToDouble(Visita::segundos).average().orElseThrow();
        double mediaY = visitas.stream().mapToDouble(v -> v.total().doubleValue()).average().orElseThrow();
        double quadradosX = 0;
        double quadradosY = 0;
        double produtos = 0;
        for (Visita v : visitas) {
            double x = v.segundos() - mediaX;
            double y = v.total().doubleValue() - mediaY;
            quadradosX += x * x;
            quadradosY += y * y;
            produtos += x * y;
        }
        if (quadradosX == 0 || quadradosY == 0) {
            return OptionalDouble.empty();
        }
        double r = produtos / Math.sqrt(quadradosX * quadradosY);
        return OptionalDouble.of(Math.clamp(r, -1.0, 1.0));
    }
}
