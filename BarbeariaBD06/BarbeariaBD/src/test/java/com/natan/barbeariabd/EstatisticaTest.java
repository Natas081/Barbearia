package com.natan.barbeariabd;

import com.natan.barbeariabd.model.Dashboard.*;
import com.natan.barbeariabd.model.Periodo;
import com.natan.barbeariabd.service.Estatistica;
import com.natan.barbeariabd.service.SerieTemporal;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EstatisticaTest {
    private static final LocalDate PRIMEIRO = LocalDate.of(2026, 9, 1);

    @Test
    void varianciaPopulacionalEModasEmpatadas() {
        var m = Estatistica.calcular(List.of(dec("1.00"), dec("1"), dec("3"), dec("3.0")));
        assertEquals(4, m.n());
        assertEquals(0, dec("2").compareTo(m.media()));
        assertEquals(0, dec("2").compareTo(m.mediana()));
        assertEquals(List.of(dec("1"), dec("3")), m.modas());
        assertEquals(0, BigDecimal.ONE.compareTo(m.variancia()));
        assertEquals(0, BigDecimal.ONE.compareTo(m.desvio()));
    }

    @Test
    void dispersaoEEstadosIndefinidos() {
        var v1 = visita(0, 60, "20");
        var v2 = visita(1, 120, "40");
        var v3 = visita(2, 180, "60");
        assertEquals(1, Estatistica.pearson(List.of(v1, v2, v3)).orElseThrow(), 1e-12);
        assertTrue(Estatistica.pearson(List.of(v1, v2)).isEmpty());
        assertTrue(Estatistica.pearson(List.of(v1, v1, v1)).isEmpty());
        assertTrue(Estatistica.calcular(List.of(v1.total(), v2.total())).modas().isEmpty());
        assertNull(Estatistica.calcular(List.of()).media());
        assertThrows(IllegalArgumentException.class, () -> new Periodo(PRIMEIRO.plusDays(1), PRIMEIRO));
    }

    @Test
    void diasSemMovimentoReceitaZeroETicketIndefinido() {
        var filtro = new Filtro(new Periodo(PRIMEIRO, PRIMEIRO.plusDays(3)), null, null, null, Intervalo.DIA);
        var s = SerieTemporal.calcular(List.of(visita(0, 60, "20"), visita(2, 180, "40")), filtro);
        assertEquals(4, s.pontos().size());
        assertEquals(dec("20.00"), s.pontos().get(PRIMEIRO).ticket());
        assertEquals(BigDecimal.ZERO, s.pontos().get(PRIMEIRO.plusDays(1)).receita());
        assertNull(s.pontos().get(PRIMEIRO.plusDays(1)).ticket());
        var mensal = new Filtro(filtro.periodo(), null, null, null, Intervalo.MES);
        assertEquals(dec("30.00"), SerieTemporal.calcular(
                List.of(visita(0, 60, "20"), visita(2, 180, "40")), mensal).pontos().get(PRIMEIRO).ticket());
    }

    private Visita visita(int acrescimo, long segundos, String total) {
        return new Visita(null, null, PRIMEIRO.plusDays(acrescimo), dec(total), 1, segundos, "Basico");
    }

    private BigDecimal dec(String number) {
        return new BigDecimal(number);
    }
}
