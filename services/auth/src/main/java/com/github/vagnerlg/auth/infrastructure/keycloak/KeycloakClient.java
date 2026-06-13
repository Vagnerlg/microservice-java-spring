package com.github.vagnerlg.auth.infrastructure.keycloak;

import com.github.vagnerlg.auth.domain.AuthToken;
import com.github.vagnerlg.auth.domain.exception.InvalidCredentialsException;
import com.github.vagnerlg.auth.domain.exception.UserAlreadyExistsException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final KeycloakProperties properties;

    KeycloakClient(RestClient keycloakRestClient, KeycloakProperties properties) {
        this.restClient = keycloakRestClient;
        this.properties = properties;
    }

    public String createUser(String username, String name, String password) {
        String adminToken = getAdminToken();

        String email = username + "@auth.local";
        var userBody = new KeycloakUserRepresentation(username, name, email, true, true, List.of());

        URI location = restClient.post()
                .uri("/admin/realms/{realm}/users", properties.realm())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(userBody)
                .retrieve()
                .onStatus(status -> status.value() == 409,
                        (req, res) -> { throw new UserAlreadyExistsException(username); })
                .toBodilessEntity()
                .getHeaders()
                .getLocation();

        if (location == null) {
            throw new IllegalStateException("Keycloak did not return a Location header for user: " + username);
        }

        String path = location.getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        var credentials = new KeycloakCredentialRepresentation("password", password, false);
        restClient.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", properties.realm(), userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credentials)
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> updateBody = new LinkedHashMap<>();
        updateBody.put("username", username);
        updateBody.put("firstName", name);
        updateBody.put("email", email);
        updateBody.put("emailVerified", true);
        updateBody.put("enabled", true);
        updateBody.put("requiredActions", List.of());
        restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateBody)
                .retrieve()
                .toBodilessEntity();

        return userId;
    }

    public AuthToken getToken(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("username", username);
        form.add("password", password);

        Map<String, Object> body = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(),
                        (req, res) -> { throw new InvalidCredentialsException(); })
                .body(MAP_TYPE);

        return toAuthToken(body);
    }

    public AuthToken refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);

        Map<String, Object> body = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.value() == 400 || status.value() == 401,
                        (req, res) -> { throw new InvalidCredentialsException(); })
                .body(MAP_TYPE);

        return toAuthToken(body);
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);

        restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/logout", properties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (req, res) -> {})
                .toBodilessEntity();
    }

    private String getAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", properties.adminUsername());
        form.add("password", properties.adminPassword());

        Map<String, Object> body = restClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MAP_TYPE);

        return (String) body.get("access_token");
    }

    private AuthToken toAuthToken(Map<String, Object> body) {
        String accessToken = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token");
        long expiresIn = ((Number) body.get("expires_in")).longValue();
        return new AuthToken(accessToken, refreshToken, expiresIn);
    }
}
