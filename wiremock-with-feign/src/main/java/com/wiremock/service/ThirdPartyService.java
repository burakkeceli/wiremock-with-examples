package com.wiremock.service;

import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.client.hystrix.ThirdPartyHystrixCommand;
import com.wiremock.model.ThirdPartyAll;
import com.wiremock.model.ThirdPartyModel;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyService {

    private final ThirdPartyApi thirdPartyApi;
    private final ThirdPartyHystrixCommand thirdPartyHystrixCommand;

    public ThirdPartyModel getThirdPartyModelById(String id) {
        try {
            return thirdPartyApi.getById(id);
        } catch (FeignException feignException) {
            log.warn("error");
            return null;
        }
    }

    public List<ThirdPartyModel> getAll() {
        List<ThirdPartyModel> thirdPartyModelList = new ArrayList<>();

        ThirdPartyAll initialThirdParty = thirdPartyApi.getAll(0);
        thirdPartyModelList.addAll(initialThirdParty.getAll());

        Integer nextPageId = initialThirdParty.getNextPageId();
        while(nextPageId != null) {
            ThirdPartyAll all1 = thirdPartyApi.getAll(nextPageId);
            thirdPartyModelList.addAll(all1.getAll());
            nextPageId = all1.getNextPageId();
        }

        return thirdPartyModelList;
    }

    public ThirdPartyAll getAllWithoutPagination() {
        return thirdPartyHystrixCommand.execute();
    }
}
