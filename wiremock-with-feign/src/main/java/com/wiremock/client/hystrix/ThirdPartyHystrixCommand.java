package com.wiremock.client.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.model.ThirdPartyAll;
import org.springframework.stereotype.Component;

import static com.wiremock.client.hystrix.HystrixConfig.config;

@Component
public class ThirdPartyHystrixCommand extends HystrixCommand<ThirdPartyAll> {

    private final ThirdPartyApi thirdPartyApi;

    ThirdPartyHystrixCommand(ThirdPartyApi thirdPartyApi) {
        super(config());
        this.thirdPartyApi = thirdPartyApi;
    }

    @Override
    protected ThirdPartyAll run() throws Exception {
        return thirdPartyApi.getAllWithoutPagination();
    }
}
