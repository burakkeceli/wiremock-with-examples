package com.wiremock.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.wiremock.client.api.ThirdPartyApi;
import com.wiremock.client.api.ThirdPartyApi.ThirdPartyConfiguration;
import com.wiremock.client.configuration.FeignConfig;
import com.wiremock.client.configuration.FeignErrorDecoder;
import com.wiremock.model.ThirdPartyModel;
import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ThirdPartyService.class,
        ThirdPartyApi.class,
        ThirdPartyConfiguration.class,
        FeignConfig.class,
        FeignErrorDecoder.class})
@ExtendWith(SpringExtension.class)
public class ThirdPartyServiceIT {

    @Autowired
    private ThirdPartyService thirdPartyService;

    private static WireMockServer wireMockServer = new WireMockServer(options().port(8888));

    @BeforeAll
    public static void setup() {
        wireMockServer.start();
        WireMock.configureFor("localhost", 8888);
    }

    @AfterAll
    public static void clean() {
        wireMockServer.stop();
    }

    @DisplayName("This test shows the closest stub if stubbing is wrong. Request will not be matched")
    @Test
    public void showTheClosestStub() {
        WireMock.stubFor(get(urlEqualTo("?id=1"))
                .willReturn(aResponse()
                        .withStatus(200)));

        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(1);

        assertThat(thirdPartyModelById).isNull();
    }

    @Test
    public void exampleTest() {
        WireMock.stubFor(get(urlEqualTo("/?id=1"))
                .willReturn(aResponse()
                        .withStatus(200)));

        ThirdPartyModel thirdPartyModelById = thirdPartyService.getThirdPartyModelById(1);

        assertThat(thirdPartyModelById).isNull();

        verify(getRequestedFor(urlEqualTo("?id=3")));
    }

    @DisplayName("Fail now")
    @Test
    void shouldThrowException() {
        thirdPartyService.getThirdPartyModelById(1);

        Throwable exception = Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            throw new UnsupportedOperationException("Not supported");
        });
        Assert.assertEquals(exception.getMessage(), "Not supported");
    }
}
