package com.wiremock.client.configuration;

import feign.Logger;
import feign.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeignLogger extends Logger.JavaLogger {

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        log.debug("Request: Headers {}, Body {} and url {}",
                request.headers(),
                request.body(),
                request.url());
    }
}
