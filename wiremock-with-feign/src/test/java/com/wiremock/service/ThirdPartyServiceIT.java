package com.wiremock.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.client.api.ThirdPartyApi.ThirdPartyConfiguration;
import com.wiremock.client.configuration.FeignConfig;
import com.wiremock.client.configuration.FeignErrorDecoder;
import com.wiremock.client.hystrix.HystrixConfig;
import com.wiremock.client.hystrix.ThirdPartyHystrixCommand;
import com.wiremock.model.ThirdPartyAll;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.okForJson;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static com.google.common.collect.ImmutableList.of;
import static com.wiremock.client.hystrix.HystrixConfig.TIMEOUT_MILLISEC;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = {ThirdPartyService.class,
        ThirdPartyApi.class,
        ThirdPartyConfiguration.class,
        FeignConfig.class,
        FeignErrorDecoder.class,
        ThirdPartyHystrixCommand.class,
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
        stubFor(get(urlEqualTo(testUrl))
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
        stubFor(get(urlEqualTo(testUrl))
                .withHeader("Content-type", containing("application/json"))
                .withHeader("Custom-header", containing("custom-header"))
                .willReturn(responseInformation));

        // when
        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(searchedModelId);

        // then
        assertThat(thirdPartyModelById.getId()).isEqualTo(searchedModelId);
        assertThat(thirdPartyModelById.getName()).isEqualTo("name");

        // verify by the count
        verify(1, getRequestedFor(urlEqualTo(testUrl)));
    }

    @DisplayName("Should show scenario with verify feature")
    @Test
    public void getPagination() {
        // given
        ThirdPartyModel thirdPartyModel1 = new ThirdPartyModel(UUID.randomUUID().toString(), "name1");
        ThirdPartyModel thirdPartyModel2 = new ThirdPartyModel(UUID.randomUUID().toString(), "name2");

        ThirdPartyAll initialThirdPartAll = new ThirdPartyAll(of(thirdPartyModel1, thirdPartyModel2), 1);
        String initialUrl = "/models?page=0";

        // and
        ThirdPartyModel secondCallThirdPartyModel1 = new ThirdPartyModel(UUID.randomUUID().toString(), "name1");
        ThirdPartyModel secondCallThirdPartyModel2 = new ThirdPartyModel(UUID.randomUUID().toString(), "name2");

        ThirdPartyAll secondThirdPartAll = new ThirdPartyAll(of(secondCallThirdPartyModel1, secondCallThirdPartyModel2), null);
        String secondUrl = "/models?page=1";

        // and
        stubFor(get(urlEqualTo(initialUrl))
                .inScenario("initial call")
                .whenScenarioStateIs(STARTED)
                .withHeader("Content-type", containing("application/json"))
                .withHeader("Custom-header", containing("custom-header"))
                .willReturn(okForJson(initialThirdPartAll)).willSetStateTo("second call"));

        // and
        stubFor(get(urlEqualTo(secondUrl))
                .inScenario("initial call")
                .whenScenarioStateIs("second call")
                .withHeader("Content-type", containing("application/json"))
                .withHeader("Custom-header", containing("custom-header"))
                .willReturn(okForJson(secondThirdPartAll)));

        // when
        List<ThirdPartyModel> thirdPartyModelList = thirdPartyService.getAll();

        // then
        assertThat(thirdPartyModelList).containsExactly(thirdPartyModel1, thirdPartyModel2, secondCallThirdPartyModel1, secondCallThirdPartyModel2);

        // and
        verify(1, getRequestedFor(new UrlPathPattern(containing("/models"), false))
                .withQueryParam("page", equalTo("0"))
                .withQueryParam("page", equalTo("1")));
    }

    @DisplayName("Should do retry")
    @Test
    public void shouldDoRetry() {
        // given
        String modelId = UUID.randomUUID().toString();
        String testUrl = "/models?id=" + modelId;

        // and
        ThirdPartyModel expectedThirdPartyModel = new ThirdPartyModel(modelId, "name");

        // and
        stubFor(get(urlEqualTo(testUrl))
                .inScenario("retry scenario")
                .whenScenarioStateIs(STARTED)
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Custom-header", equalTo("custom-header"))
                .willReturn(aResponse().withStatus(409)).willSetStateTo("second call"));

        // and
        stubFor(get(urlEqualTo(testUrl))
                .inScenario("retry scenario")
                .whenScenarioStateIs("second call")
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Custom-header", equalTo("custom-header"))
                .willReturn(aResponse().withStatus(409)).willSetStateTo("third call"))
                .setScenarioName("third call");

        // and
        stubFor(get(urlEqualTo(testUrl))
                .inScenario("retry scenario")
                .whenScenarioStateIs("third call")
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Custom-header", equalTo("custom-header"))
                .willReturn(okForJson(expectedThirdPartyModel)));

        // when
        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(modelId);

        // then
        assertThat(thirdPartyModelById.getId()).isEqualTo(modelId);
        assertThat(thirdPartyModelById.getName()).isEqualTo(expectedThirdPartyModel.getName());

        // verify by the count
        verify(3, getRequestedFor(urlEqualTo(testUrl)));
    }

    @DisplayName("Should throw hystrix run time exception when response takes more than timeout config")
    @Test
    public void shouldThrowException() {
        assertThatThrownBy(() -> {
            // given
            String testUrl = "/models";

            // and
            stubFor(get(urlEqualTo(testUrl))
                    .withHeader("Content-type", equalTo("application/json"))
                    .withHeader("Custom-header", equalTo("custom-header"))
                    .willReturn(aResponse().withFixedDelay(TIMEOUT_MILLISEC + 1_000).withStatus(200)));

            // when
            thirdPartyService.getAllWithoutPagination();
        }).isInstanceOf(HystrixRuntimeException.class);
    }

    @DisplayName("Should not hystrix run time exception when response takes less than timeout config")
    @Test
    public void shouldNotThrowException() {
        // given
        String testUrl = "/models";

        // and
        stubFor(get(urlEqualTo(testUrl))
                .withHeader("Content-type", equalTo("application/json"))
                .withHeader("Custom-header", equalTo("custom-header"))
                .willReturn(aResponse()
                        .withFixedDelay(TIMEOUT_MILLISEC - 1_000)
                        .withStatus(200)));

        // when
        thirdPartyService.getAllWithoutPagination();
    }

    private static Stream<Arguments> getResponseDefinitionBuilderBody() {
        ThirdPartyModel expectedThirdPartyModel = new ThirdPartyModel(randomUUID().toString(), "name");

        return Stream.of(
                Arguments.of("json by default", okForJson(expectedThirdPartyModel), expectedThirdPartyModel.getId()),
                Arguments.of("read from file", aResponse().withStatus(200).withBodyFile("third-party-model.json"), "1"),
                Arguments.of("get with body", aResponse().withStatus(200).withBody("{\"id\":\"2\",\"name\":\"name\"}"), "2")
        );
    }
}
