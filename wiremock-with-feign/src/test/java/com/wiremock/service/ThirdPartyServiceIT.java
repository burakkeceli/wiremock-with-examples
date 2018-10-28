package com.wiremock.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.client.api.ThirdPartyApi.ThirdPartyConfiguration;
import com.wiremock.client.configuration.FeignConfig;
import com.wiremock.client.configuration.FeignErrorDecoder;
import com.wiremock.model.ThirdPartyModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ThirdPartyService.class,
        ThirdPartyApi.class,
        ThirdPartyConfiguration.class,
        FeignConfig.class,
        FeignErrorDecoder.class,
        SerializationService.class})
@ExtendWith(SpringExtension.class)
public class ThirdPartyServiceIT {

    @Autowired
    private ThirdPartyService thirdPartyService;

    private static WireMockServer wireMockServer = new WireMockServer(options().port(8888));

    @BeforeAll
    static void setup() {
        wireMockServer.start();
        WireMock.configureFor("localhost", 8888);
    }

    @AfterAll
    static void clean() {
        wireMockServer.stop();
    }

    @DisplayName("This test shows the closest stub if stubbing is wrong. Request will not be matched")
    @Test
    public void showTheClosestStubWhenUrlIsNotMatched() {
        // given
        String notMatchedUsername = "notMatchedUsername";
        String notMatchedPassword = "notMatchedPassword";
        String stubbedModelId = randomUUID().toString();

        // and
        String searchedModelId = randomUUID().toString();
        String testUrl = "models?id=" + stubbedModelId;

        // and
        WireMock.stubFor(get(urlEqualTo(testUrl))
                .withHeader("Content-type", containing("application/xml"))
                .withHeader("Custom-header", containing("custom-header"))
                .withBasicAuth(notMatchedUsername, notMatchedPassword)
                .withQueryParam("id", equalTo(stubbedModelId))
                //.withCookie()
                //.withMetadata()
                .willReturn(aResponse()
                        .withStatus(200)));

        // when
        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(searchedModelId);

        // then
        assertThat(thirdPartyModelById).isNull();

        // verify by the count
        verify(0, getRequestedFor(urlEqualTo(testUrl)));
    }

    @DisplayName("Should return when")
    @ParameterizedTest(name = "{0}")
    @MethodSource("getResponseDefinitionBuilderBody")
    public void getTheCorrectReturnValueBasedOnBody(String testDefination, ResponseDefinitionBuilder responseInformation, String searchedModelId) {
        // given
        String testUrl = "/models?id=" + searchedModelId;

        // and
        WireMock.stubFor(get(urlEqualTo(testUrl))
                .withHeader("Content-type", containing("application/json"))
                .withHeader("Custom-header", containing("custom-header"))
                .willReturn(responseInformation));

        // when
        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(searchedModelId);

        // then
        assertThat(thirdPartyModelById.getId()).isEqualTo(searchedModelId);
        assertThat(thirdPartyModelById.getName()).isEqualTo("burak");

        // verify by the count
        verify(1, getRequestedFor(urlEqualTo(testUrl)));
    }

    private static Stream<Arguments> getResponseDefinitionBuilderBody() {
        ThirdPartyModel expectedThirdPartyModel = new ThirdPartyModel(randomUUID().toString(), "burak");

        return Stream.of(
                Arguments.of("json by default", okForJson(expectedThirdPartyModel), expectedThirdPartyModel.getId()),
                Arguments.of("read from file", aResponse().withStatus(200).withBodyFile("third-party-model.json"), "1"),
                Arguments.of("get with body", aResponse().withStatus(200).withBody("{\"id\":\"2\",\"name\":\"burak\"}"), "2")
        );
    }
}
