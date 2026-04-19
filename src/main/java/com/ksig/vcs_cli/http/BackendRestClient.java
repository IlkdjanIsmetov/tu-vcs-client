package com.ksig.vcs_cli.http;

import com.ksig.vcs_cli.auth.KeycloakClient;
import com.ksig.vcs_cli.auth.TokenStorageModule;
import com.ksig.vcs_cli.exceptions.NotLoggedInException;
import com.ksig.vcs_cli.exceptions.SessionExpiredException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BackendRestClient {

    private final TokenStorageModule tokenStorage;
    private final KeycloakClient keycloakClient;
    private final CloseableHttpClient httpClient;

    public BackendRestClient() {
        this.tokenStorage = new TokenStorageModule();
        this.keycloakClient = new KeycloakClient();
        this.httpClient = HttpClients.createDefault();
    }

    public String executeStringRequest(HttpUriRequestBase request) throws Exception {
        try (CloseableHttpResponse response = executeAuthenticatedRequest(request)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getCode() >= 200 && response.getCode() < 300) {
                return body;
            } else {
                throw new RuntimeException("Server returned status " + response.getCode() + ": " + body);
            }
        }
    }

    public Path downloadFile(HttpUriRequestBase request, Path downloadPath) throws Exception {
        try (CloseableHttpResponse response = executeAuthenticatedRequest(request)) {
            if (response.getCode() >= 200 && response.getCode() < 300) {
                String dispositionHeader = response.getHeader("Content-Disposition").getValue();
                String fileName = dispositionHeader.replaceFirst("(?i)^.*filename=\"([^\"]+)\".*$", "$1");
                downloadPath = downloadPath.resolve(fileName);

                if (Files.exists(downloadPath)) {
                    throw new RuntimeException("File " + fileName + " already exists");
                }

                Files.copy(response.getEntity().getContent(), downloadPath, StandardCopyOption.REPLACE_EXISTING);
                return downloadPath;
            } else {
                throw new RuntimeException("Server returned status " + response.getCode() + ": " + response.getEntity());
            }
        }
    }

    public Path downloadFileWithFileName(HttpUriRequestBase request, Path downloadPath) throws Exception {
        try (CloseableHttpResponse response = executeAuthenticatedRequest(request)) {
            if (response.getCode() >= 200 && response.getCode() < 300) {

                if (Files.exists(downloadPath)) {
                    throw new RuntimeException("File " + downloadPath + " already exists");
                }

                Files.copy(response.getEntity().getContent(), downloadPath, StandardCopyOption.REPLACE_EXISTING);
                return downloadPath;
            } else {
                throw new RuntimeException("Server returned status " + response.getCode() + ": " + response.getEntity());
            }
        }
    }

    private CloseableHttpResponse executeAuthenticatedRequest(HttpUriRequestBase request) {
        try {
            String accessToken = tokenStorage.getAccessToken();

            if (accessToken == null) {
                throw new NotLoggedInException();
            }

            request.setHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse response = httpClient.execute(request);

            if (response.getCode() == 401) {
                System.out.println("Session expired. Attempting to refresh token...");
                response.close();

                String refreshToken = tokenStorage.getRefreshToken();
                boolean refreshed = keycloakClient.refreshAccessToken(refreshToken);

                if (refreshed) {
                    String newAccessToken = tokenStorage.getAccessToken();
                    request.setHeader("Authorization", "Bearer " + newAccessToken);

                    return httpClient.execute(request);
                } else {
                    throw new SessionExpiredException();
                }
            }

            return response;

        } catch (SessionExpiredException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read tokens: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("API Request failed: " + e.getMessage());
        }
    }
}