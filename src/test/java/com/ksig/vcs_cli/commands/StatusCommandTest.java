package com.ksig.vcs_cli.commands;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.lang.reflect.Method;

class StatusCommandTest {
    @Test
    void call_shouldReturn1_whenNotARepo() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path tempDir = Files.createTempDirectory("not-repo");
        System.setProperty("user.dir", tempDir.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        StatusCommand command = new StatusCommand();

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("not a tu-vcs repository"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldPrintCleanMessage_whenNoChanges() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve("repo.json"), "{\"revision\":1}");
        Files.writeString(metaDir.resolve("items.json"), "[]");

        System.setProperty("user.dir", repo.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        StatusCommand command = new StatusCommand();

        int result = command.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("Nothing to commit, working tree clean."));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldPrintAdded_whenNewFileExists() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve("repo.json"), "{\"revision\":1}");
        Files.writeString(metaDir.resolve("items.json"), "[]");

        Files.writeString(repo.resolve("new.txt"), "data");

        System.setProperty("user.dir", repo.toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        StatusCommand command = new StatusCommand();

        int result = command.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("ADDED: new.txt"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void call_shouldReturn1_whenIOExceptionOccurs() throws Exception {
        String originalDir = System.getProperty("user.dir");

        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(".tu_vcs_repo");
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve("repo.json"), "{\"revision\":1}");

        System.setProperty("user.dir", repo.toString());

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        StatusCommand command = new StatusCommand();

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Error reading repository state"));

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void printStatus_shouldPrintCleanMessage_whenAllEmpty() throws Exception {
        StatusCommand command = new StatusCommand();

        Method method = StatusCommand.class.getDeclaredMethod(
                "printStatus", List.class, List.class, List.class, long.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(command, List.of(), List.of(), List.of(), 5L);

        String output = out.toString();

        assertTrue(output.contains("On revision: 5"));
        assertTrue(output.contains("Nothing to commit, working tree clean."));
    }

    @Test
    void printStatus_shouldPrintAddedItems() throws Exception {
        StatusCommand command = new StatusCommand();

        Method method = StatusCommand.class.getDeclaredMethod(
                "printStatus", List.class, List.class, List.class, long.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(command, List.of("file1.txt"), List.of(), List.of(), 1L);

        String output = out.toString();

        assertTrue(output.contains("ADDED: file1.txt"));
    }

    @Test
    void printStatus_shouldPrintModifiedItems() throws Exception {
        StatusCommand command = new StatusCommand();

        Method method = StatusCommand.class.getDeclaredMethod(
                "printStatus", List.class, List.class, List.class, long.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(command, List.of(), List.of("file2.txt"), List.of(), 1L);

        String output = out.toString();

        assertTrue(output.contains("MODIFIED: file2.txt"));
    }

    @Test
    void printStatus_shouldPrintDeletedItems() throws Exception {
        StatusCommand command = new StatusCommand();

        Method method = StatusCommand.class.getDeclaredMethod(
                "printStatus", List.class, List.class, List.class, long.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(command, List.of(), List.of(), List.of("file3.txt"), 1L);

        String output = out.toString();

        assertTrue(output.contains("DELETED: file3.txt"));
    }

    @Test
    void printStatus_shouldPrintAllCategories() throws Exception {
        StatusCommand command = new StatusCommand();

        Method method = StatusCommand.class.getDeclaredMethod(
                "printStatus", List.class, List.class, List.class, long.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(
                command,
                List.of("a.txt"),
                List.of("b.txt"),
                List.of("c.txt"),
                2L
        );

        String output = out.toString();

        assertTrue(output.contains("ADDED: a.txt"));
        assertTrue(output.contains("MODIFIED: b.txt"));
        assertTrue(output.contains("DELETED: c.txt"));
    }
}