package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

class KickMemberCommandTest {
    @Test
    void call_shouldReturn0_whenBackendSucceeds() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend();

        KickMemberCommand command = new KickMemberCommand() {
            @Override
            public Integer call() throws Exception {
                RepositoryMeta repoMeta = RepositoryStatus.getRepositoryMeta();
                URI uri = new URIBuilder(repoMeta.getUrl())
                        .appendPath("kickMember")
                        .setParameter("username", "testUser")
                        .build();

                var httpDelete = new org.apache.hc.client5.http.classic.methods.HttpDelete(uri);

                backend.executeStringRequest(httpDelete);

                System.out.println("Successfully kicked member from the repository!");
                return 0;
            }
        };

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = command.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("Successfully kicked member"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn1_whenBackendFails() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend();
        backend.shouldThrow = true;

        KickMemberCommand command = new KickMemberCommand() {
            @Override
            public Integer call() throws Exception {
                RepositoryMeta repoMeta = RepositoryStatus.getRepositoryMeta();
                URI uri = new URIBuilder(repoMeta.getUrl())
                        .appendPath("kickMember")
                        .setParameter("username", "testUser")
                        .build();

                var httpDelete = new org.apache.hc.client5.http.classic.methods.HttpDelete(uri);

                try {
                    backend.executeStringRequest(httpDelete);
                } catch (Exception e) {
                    System.err.println("Failed to kick member from this repository!");
                    return 1;
                }

                return 0;
            }
        };

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Failed to kick member"));

        System.setProperty("user.dir", originalDir);
    }

    class FakeBackend extends BackendRestClient {

        boolean shouldThrow = false;

        @Override
        public String executeStringRequest(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) {

            if (shouldThrow) {
                throw new RuntimeException("fail");
            }
            return "ok";
        }
    }
}