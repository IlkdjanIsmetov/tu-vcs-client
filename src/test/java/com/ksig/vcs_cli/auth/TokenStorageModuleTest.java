package com.ksig.vcs_cli.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;

class TokenStorageModuleTest {

    @Test
    void constructor_shouldInitializeObjectMapper() throws Exception {
        TokenStorageModule module = new TokenStorageModule();

        Field field = TokenStorageModule.class.getDeclaredField("objectMapper");
        field.setAccessible(true);

        Object mapper = field.get(module);

        assertNotNull(mapper);
    }

    @Test
    void getCredentialsFilePath_shouldCreateDirectory_whenMissing() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        Method method = TokenStorageModule.class
                .getDeclaredMethod("getCredentialsFilePath");
        method.setAccessible(true);

        Path result = (Path) method.invoke(module);

        Path expectedDir = tempHome.resolve(".tu_vcs");

        assertTrue(Files.exists(expectedDir));
        assertEquals(expectedDir.resolve("credentials.json"), result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getCredentialsFilePath_shouldUseExistingDirectory() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path existingDir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(existingDir);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        Method method = TokenStorageModule.class
                .getDeclaredMethod("getCredentialsFilePath");
        method.setAccessible(true);

        Path result = (Path) method.invoke(module);

        assertTrue(Files.exists(existingDir));
        assertEquals(existingDir.resolve("credentials.json"), result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getCredentialsFilePath_shouldReturnCorrectFileName() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        Method method = TokenStorageModule.class
                .getDeclaredMethod("getCredentialsFilePath");
        method.setAccessible(true);

        Path result = (Path) method.invoke(module);

        assertEquals("credentials.json", result.getFileName().toString());

        System.setProperty("user.home", originalHome);
    }

    @Test
    void saveTokens_shouldWriteTokensToFile() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        module.saveTokens("access123", "refresh456");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        assertTrue(Files.exists(file));

        String content = Files.readString(file);

        assertTrue(content.contains("access123"));
        assertTrue(content.contains("refresh456"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void saveTokens_shouldCreateDirectoryIfMissing() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        module.saveTokens("a", "b");

        Path dir = tempHome.resolve(".tu_vcs");

        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void saveTokens_shouldOverwriteExistingFile() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        module.saveTokens("oldA", "oldR");
        module.saveTokens("newA", "newR");

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        String content = Files.readString(file);

        assertTrue(content.contains("newA"));
        assertTrue(content.contains("newR"));
        assertFalse(content.contains("oldA"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void saveTokens_shouldHandleIOException() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path fileAsDir = dir.resolve("credentials.json");
        Files.createDirectories(fileAsDir);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        module.saveTokens("a", "b");

        assertTrue(err.toString().contains("Failed to store credentials"));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getAccessToken_shouldReturnNull_whenFileDoesNotExist() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getAccessToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getAccessToken_shouldReturnToken_whenFileIsValid() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, """
        {
          "access_token": "abc123",
          "refresh_token": "xyz"
        }
    """);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getAccessToken();

        assertEquals("abc123", result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getAccessToken_shouldReturnNull_whenJsonInvalid() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, "invalid-json");

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getAccessToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getAccessToken_shouldReturnNull_whenAccessTokenMissing() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, """
        {
          "refresh_token": "xyz"
        }
    """);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getAccessToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getRefreshToken_shouldReturnNull_whenFileDoesNotExist() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getRefreshToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getRefreshToken_shouldReturnToken_whenFileIsValid() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, """
        {
          "access_token": "abc",
          "refresh_token": "refresh123"
        }
    """);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getRefreshToken();

        assertEquals("refresh123", result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getRefreshToken_shouldReturnNull_whenJsonInvalid() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, "invalid-json");

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getRefreshToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void getRefreshToken_shouldReturnNull_whenFieldMissing() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, """
        {
          "access_token": "abc"
        }
    """);

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        String result = module.getRefreshToken();

        assertNull(result);

        System.setProperty("user.home", originalHome);
    }

    @Test
    void clearTokens_shouldDeleteFile_whenExists() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        Path dir = tempHome.resolve(".tu_vcs");
        Files.createDirectories(dir);

        Path file = dir.resolve("credentials.json");
        Files.writeString(file, "data");

        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        module.clearTokens();

        assertFalse(Files.exists(file));

        System.setProperty("user.home", originalHome);
    }

    @Test
    void clearTokens_shouldDoNothing_whenFileDoesNotExist() throws Exception {
        String originalHome = System.getProperty("user.home");

        Path tempHome = Files.createTempDirectory("home");
        System.setProperty("user.home", tempHome.toString());

        TokenStorageModule module = new TokenStorageModule();

        module.clearTokens();

        Path file = tempHome.resolve(".tu_vcs").resolve("credentials.json");

        assertFalse(Files.exists(file));

        System.setProperty("user.home", originalHome);
    }
}