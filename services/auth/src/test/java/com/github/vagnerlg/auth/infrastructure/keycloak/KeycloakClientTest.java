package com.github.vagnerlg.auth.infrastructure.keycloak;

import com.github.vagnerlg.auth.domain.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakClientTest {

    static final String BASE_URL = "http://mock-keycloak";
    static final String REALM = "testrealm";

    MockRestServiceServer mockServer;
    KeycloakClient client;

    @BeforeEach
    void setUp() {
        var restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);
        var restClient = RestClient.builder()
                .requestFactory(restTemplate.getRequestFactory())
                .baseUrl(BASE_URL)
                .build();
        var props = new KeycloakProperties(BASE_URL, REALM, "clientid", "clientsecret", "admin", "adminpass");
        client = new KeycloakClient(restClient, props);
    }

    @Test
    void refreshToken_throwsInvalidCredentials_whenKeycloakReturns401() {
        mockServer.expect(requestTo(BASE_URL + "/realms/" + REALM + "/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.refreshToken("some-refresh-token"))
                .isInstanceOf(InvalidCredentialsException.class);

        mockServer.verify();
    }

    @Test
    void createUser_throwsIllegalState_whenLocationHeaderMissing() {
        mockServer.expect(requestTo(BASE_URL + "/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"admin-tok\",\"expires_in\":300}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> client.createUser("testuser", "Test User", "Password1!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("testuser");

        mockServer.verify();
    }
}
