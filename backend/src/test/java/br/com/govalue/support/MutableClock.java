package br.com.govalue.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Relogio controlavel pelos testes, para exercitar regras de vigencia sem depender da data real. */
public class MutableClock extends Clock {

    private final ZoneId zone;
    private volatile Instant instant;

    public MutableClock(ZoneId zone, LocalDate inicio) {
        this.zone = zone;
        definir(inicio);
    }

    public void definir(LocalDate data) {
        this.instant = data.atTime(12, 0).atZone(zone).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId novaZona) {
        return new MutableClock(novaZona, LocalDate.ofInstant(instant, zone));
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
