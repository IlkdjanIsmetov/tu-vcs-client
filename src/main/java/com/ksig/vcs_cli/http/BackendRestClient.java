package com.ksig.vcs_cli.http;


import com.ksig.vcs_cli.auth.KeycloakClient;
import com.ksig.vcs_cli.auth.TokenStorageModule;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class BackendRestClient {

    private final TokenStorageModule tokenStorage;
    private final KeycloakClient keycloakClient;

    public BackendRestClient() {
        this.tokenStorage = new TokenStorageModule();
        this.keycloakClient = new KeycloakClient();
    }

    public String executeAuthenticatedRequest(HttpUriRequestBase request) {
        String accessToken = tokenStorage.getAccessToken();

        if (accessToken == null) {
            throw new IllegalStateException("Not logged in. Please run 'vcs login' first.");
        }

        request.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                
                if (response.getCode() == 401) {
                    System.out.println("Session expired. Attempting to refresh token...");
                    String refreshToken = tokenStorage.getRefreshToken();
                    
                    boolean refreshed = keycloakClient.refreshAccessToken(refreshToken);
                    
                    if (refreshed) {
                        String newAccessToken = tokenStorage.getAccessToken();
                        request.setHeader("Authorization", "Bearer " + newAccessToken);
                        
                        try (CloseableHttpResponse retryResponse = httpClient.execute(request)) {
                            return handleResponse(retryResponse);
                        }
                    } else {
                        throw new IllegalStateException("Session permanently expired. Please run 'vcs login' again.");
                    }
                }
                
                return handleResponse(response);
            }
            
        } catch (Exception e) {
            throw new RuntimeException("API Request failed: " + e.getMessage(), e);
        }
    }

    private String handleResponse(CloseableHttpResponse response) throws Exception {
        String body = EntityUtils.toString(response.getEntity());
        if (response.getCode() >= 200 && response.getCode() < 300) {
            return body;
        } else {
            throw new RuntimeException("Server returned status " + response.getCode() + ": " + body);
        }
    }
}