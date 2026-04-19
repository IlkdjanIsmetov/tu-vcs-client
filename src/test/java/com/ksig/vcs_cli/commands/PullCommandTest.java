package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;

class PullCommandTest {
    @Test
    void call_shouldReturn1_whenNotARepo() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path temp = Files.createTempDirectory("not-repo");
        System.setProperty("user.dir", temp.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        PullCommand cmd = new PullCommand();

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("not a tu-vcs repository"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn0_whenUpToDate() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":5}");
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend("5");

        Field f = PullCommand.class.getDeclaredField("backendRestClient");
        f.setAccessible(true);
        f.set(cmd, backend);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = cmd.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("You are up to date"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn1_whenConflictsExist() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        PullCommand cmd = new PullCommand();

        String syncResponse = "[{\"path\":\"a.txt\",\"status\":\"CONFLICT\"}]";

        FakeBackend backend = new FakeBackend("2", syncResponse);

        Field f = PullCommand.class.getDeclaredField("backendRestClient");
        f.setAccessible(true);
        f.set(cmd, backend);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(out.toString().contains("Conflicts found"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn0_whenNoConflictsAndTasksExist() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");
        Files.writeString(meta.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        PullCommand cmd = new PullCommand();

        String syncResponse = "[{\"path\":\"a.txt\",\"status\":\"UP_TO_DATE\"}]";

        FakeBackend backend = new FakeBackend("2", syncResponse, "[]");

        Field f = PullCommand.class.getDeclaredField("backendRestClient");
        f.setAccessible(true);
        f.set(cmd, backend);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = cmd.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("Pull successful"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void fetchAndUpdateLocalState_shouldUpdateFilesAndRevision() throws Exception {
        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend(null, null,
                "[{\"path\":\"a.txt\",\"revisionNumber\":5},{\"path\":\"b.txt\",\"revisionNumber\":3}]");

        backend.callIndex = 2;

        Field field = PullCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(cmd, backend);

        Path tempDir = Files.createTempDirectory("repo");
        Path itemsPath = tempDir.resolve("items.json");
        Path repoMetaPath = tempDir.resolve("repo.json");

        RepositoryMeta meta = new RepositoryMeta();
        meta.setUrl("http://localhost");
        meta.setRevision(1L);

        Method method = PullCommand.class.getDeclaredMethod(
                "fetchAndUpdateLocalState",
                Path.class,
                RepositoryMeta.class,
                Path.class
        );
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(cmd, itemsPath, meta, repoMetaPath);

        assertTrue(Files.exists(itemsPath));
        assertTrue(Files.exists(repoMetaPath));
        assertEquals(5, meta.getRevision());
        assertTrue(out.toString().contains("Local tracking state updated."));
    }

    @Test
    void fetchAndUpdateLocalState_shouldHandleInvalidJson() throws Exception {
        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend(null, null, "invalid-json");
        backend.callIndex = 2;

        Field field = PullCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(cmd, backend);

        Path tempDir = Files.createTempDirectory("repo");
        Path itemsPath = tempDir.resolve("items.json");
        Path repoMetaPath = tempDir.resolve("repo.json");

        RepositoryMeta meta = new RepositoryMeta();
        meta.setUrl("http://localhost");

        Method method = PullCommand.class.getDeclaredMethod(
                "fetchAndUpdateLocalState",
                Path.class,
                RepositoryMeta.class,
                Path.class
        );
        method.setAccessible(true);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        method.invoke(cmd, itemsPath, meta, repoMetaPath);

        assertFalse(Files.exists(itemsPath));
        assertTrue(err.toString().contains("Fetch failed"));
    }

    @Test
    void fetchAndUpdateLocalState_shouldSetRevision0_whenEmptyList() throws Exception {
        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend(null, null, "[]");
        backend.callIndex = 2;

        Field field = PullCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(cmd, backend);

        Path tempDir = Files.createTempDirectory("repo");
        Path itemsPath = tempDir.resolve("items.json");
        Path repoMetaPath = tempDir.resolve("repo.json");

        RepositoryMeta meta = new RepositoryMeta();
        meta.setUrl("http://localhost");

        Method method = PullCommand.class.getDeclaredMethod(
                "fetchAndUpdateLocalState",
                Path.class,
                RepositoryMeta.class,
                Path.class
        );
        method.setAccessible(true);

        method.invoke(cmd, itemsPath, meta, repoMetaPath);

        assertEquals(0, meta.getRevision());
    }

    @Test
    void getLatestRevNumber_shouldReturnParsedLong() throws Exception {
        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend("42");
        backend.callIndex = 0;

        Field field = PullCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(cmd, backend);

        RepositoryMeta meta = new RepositoryMeta();
        meta.setUrl("http://localhost");

        Method method = PullCommand.class.getDeclaredMethod(
                "getLatestRevNumber",
                RepositoryMeta.class
        );
        method.setAccessible(true);

        long result = (long) method.invoke(cmd, meta);

        assertEquals(42L, result);
    }

    @Test
    void getLatestRevNumber_shouldThrow_whenInvalidNumber() throws Exception {
        PullCommand cmd = new PullCommand();

        FakeBackend backend = new FakeBackend("not-a-number");
        backend.callIndex = 0;

        Field field = PullCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(cmd, backend);

        RepositoryMeta meta = new RepositoryMeta();
        meta.setUrl("http://localhost");

        Method method = PullCommand.class.getDeclaredMethod(
                "getLatestRevNumber",
                RepositoryMeta.class
        );
        method.setAccessible(true);

        assertThrows(NumberFormatException.class, () -> {
            try {
                method.invoke(cmd, meta);
            } catch (Exception e) {
                throw (RuntimeException) e.getCause();
            }
        });
    }

    class FakeBackend extends BackendRestClient {

        String latestRev;
        String syncResponse;
        String fetchResponse;
        int callIndex = 0;

        FakeBackend(String latestRev) {
            this.latestRev = latestRev;
        }

        FakeBackend(String latestRev, String syncResponse) {
            this.latestRev = latestRev;
            this.syncResponse = syncResponse;
        }

        FakeBackend(String latestRev, String syncResponse, String fetchResponse) {
            this.latestRev = latestRev;
            this.syncResponse = syncResponse;
            this.fetchResponse = fetchResponse;
        }

        @Override
        public String executeStringRequest(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) {

            callIndex++;

            if (callIndex == 1) {
                return latestRev;
            }
            if (callIndex == 2) {
                return syncResponse;
            }
            return fetchResponse != null ? fetchResponse : "[]";
        }
    }
}