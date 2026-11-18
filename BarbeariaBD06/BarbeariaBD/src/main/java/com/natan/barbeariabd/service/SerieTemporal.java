package com.natan.barbeariabd.service;

import com.natan.barbeariabd.model.Dashboard.Filtro;
import com.natan.barbeariabd.model.Dashboard.Intervalo;
import com.natan.barbeariabd.model.Dashboard.Visita;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

public final class SerieTemporal {
    private SerieTemporal() {}

    public record Ponto(BigDecimal receita, long n) {
        public BigDecimal ticket() {
            return n == 0 ? null : receita.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
    }

    public record Serie(NavigableMap<LocalDate, Ponto> pontos, Intervalo intervalo) {}

    public static Serie calcular(List<Visita> visitas, Filtro filtro) {
        if (visitas.isEmpty()) {
            return new Serie(new TreeMap<>(), filtro.intervalo());
        }
        LocalDate inicio = filtro.periodo().inicio() != null ? filtro.periodo().inicio()
                : visitas.stream().map(Visita::dia).min(LocalDate::compareTo).orElseThrow();
        LocalDate fim = filtro.periodo().fim() != null ? filtro.periodo().fim()
                : visitas.stream().map(Visita::dia).max(LocalDate::compareTo).orElseThrow();

        long dias = ChronoUnit.DAYS.between(inicio, fim);
        Intervalo intervalo = filtro.intervalo();
        if (dias > 366 && intervalo == Intervalo.DIA) {
            intervalo = Intervalo.SEMANA;
        }
        if (dias > 366 * 4L) {
            intervalo = Intervalo.MES;
        }

        NavigableMap<LocalDate, Ponto> pontos = new TreeMap<>();
        for (Visita v : visitas) {
            pontos.merge(inicioDe(v.dia(), intervalo), new Ponto(v.total(), 1),
                    (a, b) -> new Ponto(a.receita.add(b.receita), a.n + b.n));
        }
        if (ChronoUnit.MONTHS.between(inicio, fim) <= 600) {
            for (LocalDate data = inicioDe(inicio, intervalo); !data.isAfter(fim); data = avancar(data, intervalo)) {
                pontos.putIfAbsent(data, new Ponto(BigDecimal.ZERO, 0));
            }
        }
        return new Serie(pontos, intervalo);
    }

    private static LocalDate inicioDe(LocalDate dia, Intervalo intervalo) {
        return switch (intervalo) {
            case DIA -> dia;
            case SEMANA -> dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> dia.withDayOfMonth(1);
        };
    }

    private static LocalDate avancar(LocalDate dia, Intervalo intervalo) {
        return switch (intervalo) {
            case DIA -> dia.plusDays(1);
            case SEMANA -> dia.plusWeeks(1);
            case MES -> dia.plusMonths(1);
        };
    }
}
