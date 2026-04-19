package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;


class FetchCommandTest {
    @Test
    void call_shouldReturn1_whenNotARepo() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path temp = Files.createTempDirectory("not-repo");
        System.setProperty("user.dir", temp.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        FetchCommand cmd = new FetchCommand();

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("not a tu-vcs repository"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn1_whenUrlInvalid() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"ht!tp://bad-url\",\"revision\":1}");
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        FetchCommand cmd = new FetchCommand();

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("url is invalid"));

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
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend();
        backend.shouldThrow = true;

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        FetchCommand cmd = new FetchCommand(backend);

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Failed to fetch items"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn0_andUpdateFiles() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        String response = """
[
  {
    "id": "11111111-1111-1111-1111-111111111111",
    "path": "a.txt",
    "itemType": "FILE",
    "revisionId": "22222222-2222-2222-2222-222222222222",
    "revisionNumber": 5,
    "checksum": "abc",
    "storageKey": "key"
  }
]
""";

        FakeBackend backend = new FakeBackend(response);

        FetchCommand cmd = new FetchCommand(backend);

        int result = cmd.call();

        assertEquals(0, result);

        String updatedRepo = Files.readString(meta.resolve("repo.json"));
        assertTrue(updatedRepo.contains("\"revision\" : 5"));

        System.setProperty("user.dir", originalDir);
    }

    class FakeBackend extends BackendRestClient {

        boolean shouldThrow = false;
        String response = "[]";

        FakeBackend() {}

        FakeBackend(String response) {
            this.response = response;
        }

        @Override
        public String executeStringRequest(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) {

            if (shouldThrow) {
                throw new RuntimeException("fail");
            }

            return response;
        }
    }

}