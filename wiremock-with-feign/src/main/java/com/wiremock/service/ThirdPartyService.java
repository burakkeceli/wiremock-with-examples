package com.wiremock.service;

import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.model.ThirdPartyModel;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyService {

    private final ThirdPartyApi thirdPartyApi;

    public ThirdPartyModel getThirdPartyModelById(Integer id) {
        try {
            return thirdPartyApi.getById(id);
        } catch (FeignException feignException) {
            log.warn("error");
            return null;
        }
    }
}
