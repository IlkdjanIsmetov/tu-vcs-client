package com.ksig.vcs_cli.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.ksig.vcs_cli.globalParams.GlobarParams.*;

public class KeycloakClient {

    private final ObjectMapper objectMapper;

    public KeycloakClient() {
        this.objectMapper = new ObjectMapper();
    }

    public boolean authenticate(String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(KEYCLOAK_TOKEN_URL);

            List<NameValuePair> formData = new ArrayList<>();
            formData.add(new BasicNameValuePair("client_id", KEYCLOACK_CLIENT_ID));
            formData.add(new BasicNameValuePair("grant_type", "password"));
            formData.add(new BasicNameValuePair("username", username));
            formData.add(new BasicNameValuePair("password", password));
            httpPost.setEntity(new UrlEncodedFormEntity(formData, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getCode() == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    String accessToken = jsonResponse.get("access_token").asText();
                    String refreshToken = jsonResponse.has("refresh_token") ? jsonResponse.get("refresh_token").asText() : null;

                    TokenStorageModule storage = new TokenStorageModule();
                    storage.saveTokens(accessToken, refreshToken);
                    return true;
                } else {
                    System.err.println("Error from Keycloak (Status " + response.getCode() + "): " + responseBody);
                    return false;
                }
            }

        } catch (Exception e) {
            System.err.println("Network error: " + e.getMessage());
            return false;
        }
    }


    public boolean refreshAccessToken(String currentRefreshToken) {
        if (currentRefreshToken == null || currentRefreshToken.isEmpty()) {
            return false;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(KEYCLOAK_TOKEN_URL);

            List<NameValuePair> formData = new ArrayList<>();
            formData.add(new BasicNameValuePair("client_id", CLIENT_ID));
            formData.add(new BasicNameValuePair("grant_type", "refresh_token"));
            formData.add(new BasicNameValuePair("refresh_token", currentRefreshToken));

            httpPost.setEntity(new UrlEncodedFormEntity(formData, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getCode() == 200) {
                    JsonNode jsonResponse = objectMapper.readTree(responseBody);
                    String newAccessToken = jsonResponse.get("access_token").asText();
                    String newRefreshToken = jsonResponse.has("refresh_token") ?
                            jsonResponse.get("refresh_token").asText() : currentRefreshToken;

                    TokenStorageModule storage = new TokenStorageModule();
                    storage.saveTokens(newAccessToken, newRefreshToken);
                    return true;
                } else {
                    TokenStorageModule storage = new TokenStorageModule();
                    storage.clearTokens();
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("Network error during token refresh: " + e.getMessage());
            return false;
        }
    }
}