package com.wiremock.client.configuration;

import feign.Feign;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

import static feign.Logger.Level.FULL;

@Configuration
public class FeignConfig {

    @Bean
    public Feign.Builder feignBuilder(FeignErrorDecoder errorDecoder) {
        return Feign.builder()
                .logger(new FeignLogger())
                .decoder(new JacksonDecoder())
                .encoder(new JacksonEncoder())
                .errorDecoder(errorDecoder)
                .retryer(new Retryer.Default(100L, TimeUnit.SECONDS.toMillis(1L), 3))
                .logLevel(FULL);
    }
}
