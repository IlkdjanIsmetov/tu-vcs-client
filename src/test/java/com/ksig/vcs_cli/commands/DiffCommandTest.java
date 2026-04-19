package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class DiffCommandTest {
    @Test
    void call_shouldReturn1_whenNotARepo() throws Exception {
        String originalDir = System.getProperty("user.dir");
        PrintStream originalErr = System.err;

        Path temp = Files.createTempDirectory("not-repo");
        System.setProperty("user.dir", temp.toString());

        DiffCommand cmd = new DiffCommand();

        Field fileField = DiffCommand.class.getDeclaredField("filePath");
        fileField.setAccessible(true);
        fileField.set(cmd, "file.txt");

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Error"));

        System.setProperty("user.dir", originalDir);
        System.setErr(originalErr);
    }

    @Test
    void call_shouldReturn0_whenNoDiff() throws Exception {
        String originalDir = System.getProperty("user.dir");
        PrintStream originalOut = System.out;

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        Path file = repo.resolve("file.txt");
        Files.writeString(file, "line1\nline2");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend("line1\nline2");
        injectBackend(backend);

        DiffCommand cmd = new DiffCommand();
        setField(cmd, "filePath", "file.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = cmd.call();

        assertEquals(0, result);
        assertTrue(out.toString().isEmpty());

        System.setProperty("user.dir", originalDir);
        System.setOut(originalOut);
    }

    @Test
    void call_shouldPrintDiff_whenContentDiffers() throws Exception {
        String originalDir = System.getProperty("user.dir");
        PrintStream originalOut = System.out;

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        Path file = repo.resolve("file.txt");
        Files.writeString(file, "line1\nline2");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend("line1\nchanged");
        injectBackend(backend);

        DiffCommand cmd = new DiffCommand();
        setField(cmd, "filePath", "file.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        int result = cmd.call();

        String output = out.toString().replaceAll("\\u001B\\[[;\\d]*m", "");

        assertEquals(0, result);
        assertFalse(output.isEmpty());
        assertTrue(output.contains("changed"));

        System.setProperty("user.dir", originalDir);
        System.setOut(originalOut);
    }

    @Test
    void call_shouldReturn1_whenBackendFails() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path meta = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(meta);

        Files.writeString(meta.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        Path file = repo.resolve("file.txt");
        Files.writeString(file, "line1");

        System.setProperty("user.dir", repo.toString());

        FakeBackend backend = new FakeBackend(null);
        backend.shouldThrow = true;

        injectBackend(backend);

        DiffCommand cmd = new DiffCommand();

        setField(cmd, "filePath", "file.txt");

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        int result = cmd.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Error"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void generateDiff_shouldPrintNothing_whenListsAreEqual() throws Exception {
        DiffCommand cmd = new DiffCommand();

        Method method = DiffCommand.class.getDeclaredMethod(
                "generateDiff", List.class, List.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(cmd, List.of("a", "b"), List.of("a", "b"));

        assertTrue(out.toString().isEmpty());
    }

    @Test
    void generateDiff_shouldPrintAddedLine() throws Exception {
        DiffCommand cmd = new DiffCommand();

        Method method = DiffCommand.class.getDeclaredMethod(
                "generateDiff", List.class, List.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(cmd, List.of("a"), List.of("a", "b"));

        String output = out.toString();

        assertTrue(output.contains("+b"));
    }

    @Test
    void generateDiff_shouldPrintRemovedLine() throws Exception {
        DiffCommand cmd = new DiffCommand();

        Method method = DiffCommand.class.getDeclaredMethod(
                "generateDiff", List.class, List.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(cmd, List.of("a", "b"), List.of("a"));

        String output = out.toString();

        assertTrue(output.contains("-b"));
    }

    @Test
    void generateDiff_shouldPrintModifiedLines() throws Exception {
        DiffCommand cmd = new DiffCommand();

        Method method = DiffCommand.class.getDeclaredMethod(
                "generateDiff", List.class, List.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(cmd, List.of("a", "b"), List.of("a", "c"));

        String output = out.toString();

        assertTrue(output.contains("-b"));
        assertTrue(output.contains("+c"));
    }

    @Test
    void getFileContentByRevision_shouldReturnLines_withoutRevision() throws Exception {
        DiffCommand cmd = new DiffCommand();

        FakeBackend backend = new FakeBackend("line1\nline2");
        injectBackend(backend);

        Method method = DiffCommand.class.getDeclaredMethod(
                "getFileContentByRevision",
                String.class, Long.class, String.class
        );
        method.setAccessible(true);

        List<String> result = (List<String>) method.invoke(
                cmd, "file.txt", null, "http://localhost"
        );

        assertEquals(List.of("line1", "line2"), result);
    }

    @Test
    void getFileContentByRevision_shouldIncludeRevisionParameter() throws Exception {
        DiffCommand cmd = new DiffCommand();

        FakeBackend backend = new FakeBackend("line");
        injectBackend(backend);

        Method method = DiffCommand.class.getDeclaredMethod(
                "getFileContentByRevision",
                String.class, Long.class, String.class
        );
        method.setAccessible(true);

        method.invoke(cmd, "file.txt", 5L, "http://localhost");

        assertTrue(backend.lastUri.contains("rev=5"));
    }

    @Test
    void getFileContentByRevision_shouldHandleSingleLine() throws Exception {
        DiffCommand cmd = new DiffCommand();

        FakeBackend backend = new FakeBackend("onlyline");
        injectBackend(backend);

        Method method = DiffCommand.class.getDeclaredMethod(
                "getFileContentByRevision",
                String.class, Long.class, String.class
        );
        method.setAccessible(true);

        List<String> result = (List<String>) method.invoke(
                cmd, "file.txt", null, "http://localhost"
        );

        assertEquals(List.of("onlyline"), result);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void injectBackend(BackendRestClient backend) throws Exception {
        Field field = CreateRepositoryCommand.class.getDeclaredField("backendRestClient");
        field.setAccessible(true);
        field.set(null, backend);
    }

    class FakeBackend extends BackendRestClient {

        boolean shouldThrow = false;
        String response = "[]";
        String lastUri;

        FakeBackend() {}

        FakeBackend(String response) {
            this.response = response;
        }

        @Override
        public String executeStringRequest(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) {

            lastUri = request.getRequestUri();

            if (shouldThrow) {
                throw new RuntimeException("fail");
            }

            return response;
        }
    }

}