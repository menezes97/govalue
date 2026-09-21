package br.com.govalue.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Clock injetavel: regras de vigencia ficam testaveis sem depender da data real. */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
