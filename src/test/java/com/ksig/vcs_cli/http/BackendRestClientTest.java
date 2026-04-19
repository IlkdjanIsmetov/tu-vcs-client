package com.ksig.vcs_cli.http;

import com.ksig.vcs_cli.auth.KeycloakClient;
import com.ksig.vcs_cli.auth.TokenStorageModule;
import com.ksig.vcs_cli.exceptions.NotLoggedInException;
import com.ksig.vcs_cli.exceptions.SessionExpiredException;

import org.junit.jupiter.api.Test;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BackendRestClientTest {

    private CloseableHttpClient createFakeClient(int status, String body) {
        return new CloseableHttpClient() {

            @Override
            protected CloseableHttpResponse doExecute(
                    org.apache.hc.core5.http.HttpHost target,
                    org.apache.hc.core5.http.ClassicHttpRequest request,
                    org.apache.hc.core5.http.protocol.HttpContext context) {

                BasicClassicHttpResponse response = new BasicClassicHttpResponse(status);
                response.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

                return CloseableHttpResponse.adapt(response);
            }

            @Override
            public void close() {}

            @Override
            public void close(org.apache.hc.core5.io.CloseMode closeMode) {}
        };
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private TokenStorageModule token(String access, String refresh) {
        return new TokenStorageModule() {
            @Override public String getAccessToken() { return access; }
            @Override public String getRefreshToken() { return refresh; }
        };
    }

    @Test
    void constructor_shouldInitializeFields() {
        assertNotNull(new BackendRestClient());
    }

    @Test
    void executeStringRequest_shouldReturnBody_whenStatusIs2xx() throws Exception {
        BackendRestClient client = new BackendRestClient();

        inject(client, "httpClient", createFakeClient(200, "ok"));
        inject(client, "tokenStorage", token("token", "refresh"));

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        String result = client.executeStringRequest(request);

        assertEquals("ok", result);
    }

    @Test
    void executeStringRequest_shouldThrowException_whenStatusIsNot2xx() throws Exception {
        BackendRestClient client = new BackendRestClient();

        inject(client, "httpClient", createFakeClient(400, "error"));
        inject(client, "tokenStorage", token("token", "refresh"));

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.executeStringRequest(request));

        assertTrue(ex.getMessage().contains("400"));
        assertTrue(ex.getMessage().contains("error"));
    }

    @Test
    void downloadFile_shouldDownloadFile_whenResponseIs2xx() throws Exception {
        BackendRestClient client = new BackendRestClient();

        CloseableHttpClient fake = new CloseableHttpClient() {
            @Override
            protected CloseableHttpResponse doExecute(
                    org.apache.hc.core5.http.HttpHost target,
                    org.apache.hc.core5.http.ClassicHttpRequest request,
                    org.apache.hc.core5.http.protocol.HttpContext context) {

                BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
                response.addHeader("Content-Disposition", "attachment; filename=\"file.txt\"");
                response.setEntity(new StringEntity("data", StandardCharsets.UTF_8));

                return CloseableHttpResponse.adapt(response);
            }

            @Override public void close() {}
            @Override public void close(org.apache.hc.core5.io.CloseMode closeMode) {}
        };

        inject(client, "httpClient", fake);
        inject(client, "tokenStorage", token("token", "refresh"));

        Path tempDir = Files.createTempDirectory("download");

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        Path result = client.downloadFile(request, tempDir);

        assertTrue(Files.exists(result));
        assertEquals("file.txt", result.getFileName().toString());
    }

    @Test
    void downloadFile_shouldThrowException_whenFileAlreadyExists() throws Exception {
        BackendRestClient client = new BackendRestClient();

        CloseableHttpClient fake = new CloseableHttpClient() {
            @Override
            protected CloseableHttpResponse doExecute(
                    org.apache.hc.core5.http.HttpHost target,
                    org.apache.hc.core5.http.ClassicHttpRequest request,
                    org.apache.hc.core5.http.protocol.HttpContext context) {

                BasicClassicHttpResponse response = new BasicClassicHttpResponse(200);
                response.addHeader("Content-Disposition", "attachment; filename=\"file.txt\"");
                response.setEntity(new StringEntity("data", StandardCharsets.UTF_8));

                return CloseableHttpResponse.adapt(response);
            }

            @Override public void close() {}
            @Override public void close(org.apache.hc.core5.io.CloseMode closeMode) {}
        };

        inject(client, "httpClient", fake);
        inject(client, "tokenStorage", token("token", "refresh"));

        Path tempDir = Files.createTempDirectory("download");
        Files.createFile(tempDir.resolve("file.txt"));

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        assertThrows(RuntimeException.class,
                () -> client.downloadFile(request, tempDir));
    }

    @Test
    void executeAuthenticatedRequest_shouldThrow_whenNoAccessToken() throws Exception {
        BackendRestClient client = new BackendRestClient();

        inject(client, "tokenStorage", token(null, null));

        Method m = BackendRestClient.class
                .getDeclaredMethod("executeAuthenticatedRequest", HttpUriRequestBase.class);
        m.setAccessible(true);

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        Throwable ex = assertThrows(Throwable.class,
                () -> m.invoke(client, request));

        assertTrue(ex instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(ex.getCause() instanceof NotLoggedInException);
    }

    @Test
    void executeAuthenticatedRequest_shouldReturnResponse_whenStatusIsOk() throws Exception {
        BackendRestClient client = new BackendRestClient();

        CloseableHttpResponse response =
                CloseableHttpResponse.adapt(new BasicClassicHttpResponse(200));

        CloseableHttpClient fake = new CloseableHttpClient() {
            @Override protected CloseableHttpResponse doExecute(
                    org.apache.hc.core5.http.HttpHost t,
                    org.apache.hc.core5.http.ClassicHttpRequest r,
                    org.apache.hc.core5.http.protocol.HttpContext c) {
                return response;
            }
            @Override public void close() {}
            @Override public void close(org.apache.hc.core5.io.CloseMode closeMode) {}
        };

        inject(client, "httpClient", fake);
        inject(client, "tokenStorage", token("token", "refresh"));

        Method m = BackendRestClient.class
                .getDeclaredMethod("executeAuthenticatedRequest", HttpUriRequestBase.class);
        m.setAccessible(true);

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        Object result = m.invoke(client, request);

        assertEquals(response, result);
    }

    @Test
    void executeAuthenticatedRequest_shouldThrow_when401AndRefreshFails() throws Exception {
        BackendRestClient client = new BackendRestClient();

        CloseableHttpResponse response401 =
                CloseableHttpResponse.adapt(new BasicClassicHttpResponse(401));

        CloseableHttpClient fake = new CloseableHttpClient() {
            @Override protected CloseableHttpResponse doExecute(
                    org.apache.hc.core5.http.HttpHost t,
                    org.apache.hc.core5.http.ClassicHttpRequest r,
                    org.apache.hc.core5.http.protocol.HttpContext c) {
                return response401;
            }
            @Override public void close() {}
            @Override public void close(org.apache.hc.core5.io.CloseMode closeMode) {}
        };

        inject(client, "httpClient", fake);
        inject(client, "tokenStorage", token("token", "refresh"));

        KeycloakClient keycloak = new KeycloakClient() {
            @Override public boolean refreshAccessToken(String r) {
                return false;
            }
        };

        inject(client, "keycloakClient", keycloak);

        Method m = BackendRestClient.class
                .getDeclaredMethod("executeAuthenticatedRequest", HttpUriRequestBase.class);
        m.setAccessible(true);

        HttpUriRequestBase request =
                new HttpUriRequestBase("GET", java.net.URI.create("http://localhost")) {};

        Exception ex = assertThrows(Exception.class, () -> m.invoke(client, request));

        assertTrue(ex instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(ex.getCause() instanceof SessionExpiredException);
    }
}