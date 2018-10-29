package com.wiremock.client.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

public class HystrixConfig {

    public static final int TIMEOUT_MILLISEC = 3_000;

    public static HystrixCommand.Setter config() {

        HystrixCommand.Setter config = HystrixCommand
                .Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("ThirdPartyApi"));

        HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter();
        commandProperties.withExecutionTimeoutInMilliseconds(TIMEOUT_MILLISEC);
        config.andCommandPropertiesDefaults(commandProperties);

        return config;
    }
}
