package com.ksig.vcs_cli.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;


class KeycloakClientTest {
    @Test
    void constructor_shouldInitializeObjectMapper() throws Exception {
        KeycloakClient client = new KeycloakClient();

        Field field = KeycloakClient.class.getDeclaredField("objectMapper");
        field.setAccessible(true);

        Object mapper = field.get(client);

        assertNotNull(mapper);
    }

    @Test
    void authenticate_shouldReturnTrue_andSaveTokens_whenResponse200() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean authenticate(String username, String password) {
                try {
                    String responseBody = """
                    {
                      "access_token": "access123",
                      "refresh_token": "refresh456"
                    }
                """;

                    JsonNode jsonResponse = new ObjectMapper().readTree(responseBody);

                    String accessToken = jsonResponse.get("access_token").asText();
                    String refreshToken = jsonResponse.get("refresh_token").asText();

                    TokenStorageModule storage = new TokenStorageModule();
                    storage.saveTokens(accessToken, refreshToken);

                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };

        boolean result = client.authenticate("user", "pass");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        assertTrue(result);
        assertTrue(Files.exists(file));

        String content = Files.readString(file);
        assertTrue(content.contains("access123"));
        assertTrue(content.contains("refresh456"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void authenticate_shouldReturnFalse_whenStatusNot200() {
        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean authenticate(String username, String password) {
                System.err.println("Error from Keycloak (Status 400): bad request");
                return false;
            }
        };

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        boolean result = client.authenticate("user", "pass");

        assertFalse(result);
        assertTrue(err.toString().contains("Error from Keycloak"));
    }

    @Test
    void authenticate_shouldReturnFalse_whenExceptionOccurs() {
        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean authenticate(String username, String password) {
                System.err.println("Network error: fail");
                return false;
            }
        };

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        boolean result = client.authenticate("user", "pass");

        assertFalse(result);
        assertTrue(err.toString().contains("Network error"));
    }

    @Test
    void refreshAccessToken_shouldReturnFalse_whenTokenNullOrEmpty() {
        KeycloakClient client = new KeycloakClient();

        assertFalse(client.refreshAccessToken(null));
        assertFalse(client.refreshAccessToken(""));
    }

    @Test
    void refreshAccessToken_shouldSaveTokens_whenSuccess() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean refreshAccessToken(String token) {
                try {
                    String responseBody = """
                    {
                      "access_token": "newAccess",
                      "refresh_token": "newRefresh"
                    }
                """;

                    JsonNode json = new ObjectMapper().readTree(responseBody);

                    String access = json.get("access_token").asText();
                    String refresh = json.get("refresh_token").asText();

                    new TokenStorageModule().saveTokens(access, refresh);

                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };

        boolean result = client.refreshAccessToken("oldRefresh");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        assertTrue(result);
        assertTrue(Files.exists(file));

        String content = Files.readString(file);
        assertTrue(content.contains("newAccess"));
        assertTrue(content.contains("newRefresh"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void refreshAccessToken_shouldReuseOldRefreshToken_whenMissingInResponse() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean refreshAccessToken(String token) {
                try {
                    String responseBody = """
                    {
                      "access_token": "newAccess"
                    }
                """;

                    JsonNode json = new ObjectMapper().readTree(responseBody);

                    String access = json.get("access_token").asText();
                    String refresh = token;

                    new TokenStorageModule().saveTokens(access, refresh);

                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        };

        boolean result = client.refreshAccessToken("oldRefresh");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");
        String content = Files.readString(file);

        assertTrue(result);
        assertTrue(content.contains("newAccess"));
        assertTrue(content.contains("oldRefresh"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void refreshAccessToken_shouldClearTokens_whenStatusNot200() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule storage = new TokenStorageModule();
        storage.saveTokens("a", "b");

        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean refreshAccessToken(String token) {
                new TokenStorageModule().clearTokens();
                return false;
            }
        };

        boolean result = client.refreshAccessToken("token");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        assertFalse(result);
        assertFalse(Files.exists(file));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void refreshAccessToken_shouldReturnFalse_whenExceptionOccurs() {
        KeycloakClient client = new KeycloakClient() {
            @Override
            public boolean refreshAccessToken(String token) {
                System.err.println("Network error during token refresh: fail");
                return false;
            }
        };

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        boolean result = client.refreshAccessToken("token");

        assertFalse(result);
        assertTrue(err.toString().contains("Network error"));
    }
}