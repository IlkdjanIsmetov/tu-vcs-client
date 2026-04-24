package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;


class RejectChangeRequestCommandTest {
    @Test
    void call_shouldReturn1_whenNotARepo() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path tempDir = Files.createTempDirectory("not-repo");
        System.setProperty("user.dir", tempDir.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        RejectChangeRequestCommand command = new RejectChangeRequestCommand();

        Field idField = RejectChangeRequestCommand.class.getDeclaredField("changeRequestId");
        idField.setAccessible(true);
        idField.set(command, "123");

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("not a tu-vcs repository"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn0_andCallBackendWithCorrectUrl() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        System.setProperty("user.dir", repo.toString());

        RejectChangeRequestCommand command = new RejectChangeRequestCommand();

        Field idField = RejectChangeRequestCommand.class.getDeclaredField("changeRequestId");
        idField.setAccessible(true);
        idField.set(command, "42");

        FakeBackendRestClient fake = new FakeBackendRestClient();

        Field clientField = RejectChangeRequestCommand.class.getDeclaredField("backendRestClient");
        clientField.setAccessible(true);
        clientField.set(command, fake);

        int result = command.call();

        assertEquals(0, result);
        assertTrue(fake.called);
        assertFalse(fake.lastUri.toString().endsWith("/change-request/42/approve"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn1_whenBackendFails() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve("repo.json"),
                "{\"url\":\"http://localhost:8080\",\"revision\":1}");

        System.setProperty("user.dir", repo.toString());

        RejectChangeRequestCommand command = new RejectChangeRequestCommand();

        Field idField = RejectChangeRequestCommand.class.getDeclaredField("changeRequestId");
        idField.setAccessible(true);
        idField.set(command, "42");

        FakeBackendRestClient fake = new FakeBackendRestClient();
        fake.shouldThrow = true;

        Field clientField = RejectChangeRequestCommand.class.getDeclaredField("backendRestClient");
        clientField.setAccessible(true);
        clientField.set(command, fake);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Error"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldThrowRuntimeException_whenIOExceptionOccurs() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        System.setProperty("user.dir", repo.toString());

        RejectChangeRequestCommand command = new RejectChangeRequestCommand();

        Field idField = RejectChangeRequestCommand.class.getDeclaredField("changeRequestId");
        idField.setAccessible(true);
        idField.set(command, "42");

        assertThrows(RuntimeException.class, command::call);

        System.setProperty("user.dir", originalDir);
    }

    class FakeBackendRestClient extends BackendRestClient {

        boolean called = false;
        boolean shouldThrow = false;
        java.net.URI lastUri;

        @Override
        public String executeStringRequest(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request) throws Exception {

            called = true;
            lastUri = request.getUri();

            if (shouldThrow) {
                throw new RuntimeException("fail");
            }

            return "ok";
        }
    }
}