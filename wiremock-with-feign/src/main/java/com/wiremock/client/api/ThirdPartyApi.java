package com.wiremock.client.api;

import com.wiremock.model.ThirdPartyModel;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Headers(value = {"Content-type: application/json", "Custom-header: custom-header"})
public interface ThirdPartyApi {

    @RequestLine("GET /models?id={id}")
    ThirdPartyModel getById(@Param(value = "id") String id);

    @Configuration
    class ThirdPartyConfiguration {

        @Value("${base.url}")
        private String baseUrl;

        @Bean
        ThirdPartyApi thirdPartyApi(Feign.Builder feignBuilder) {
            return feignBuilder.target(ThirdPartyApi.class, baseUrl);
        }
    }
}
