package com.wiremock.client.api;

import com.wiremock.model.ThirdPartyAll;
import com.wiremock.model.ThirdPartyModel;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Headers(value = {"Content-type: application/json", "Custom-header: custom-header"})
public interface ThirdPartyApi {

    @RequestLine("GET /models?id={id}")
    ThirdPartyModel getById(@Param(value = "id") String id);

    @RequestLine("GET /models?page={pageId}")
    ThirdPartyAll getAll(@Param(value = "pageId") Integer pageId);

    @RequestLine("GET /models")
    ThirdPartyAll getAllWithoutPagination();

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
