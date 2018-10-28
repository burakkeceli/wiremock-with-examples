package com.wiremock.client.configuration;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (409 == response.status()) {
            return new RetryableException("getting conflict and retry", response.request().httpMethod(), null);
        } else {
            return super.decode(methodKey, response);
        }
    }
}
