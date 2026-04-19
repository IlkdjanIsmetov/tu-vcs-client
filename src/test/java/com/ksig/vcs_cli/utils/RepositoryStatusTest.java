package com.ksig.vcs_cli.utils;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.models.RepositoryMeta;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static com.github.stefanbirkner.systemlambda.SystemLambda.*;

class RepositoryStatusTest {

    @Test
    void calculateChecksum_shouldReturnEmptyString_forDirectory() throws Exception {
        Path dir = Files.createTempDirectory("testDir");
        String result = RepositoryStatus.calculateChecksum(dir);
        assertEquals("", result);
    }

    @Test
    void calculateChecksum_shouldReturnCorrectHash_forFile() throws Exception {
        Path file = Files.createTempFile("testFile", ".txt");
        Files.writeString(file, "hello");

        String result = RepositoryStatus.calculateChecksum(file);

        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result);
    }

    @Test
    void calculateChecksum_shouldReturnEmptyString_whenFileDoesNotExist() throws Exception {
        Path file = Path.of("non_existing_file.txt");

        String output = tapSystemErr(() -> {
            String result = RepositoryStatus.calculateChecksum(file);
            assertEquals("", result);
        });

        assertTrue(output.contains("Warning: Could not calculate checksum"));
    }

    @Test
    void getRepositoryMeta_shouldReturnMeta_whenValidRepoExists() throws Exception {
        Path tempDir = Files.createTempDirectory("repo");
        Path metaDir = tempDir.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Path metaFile = metaDir.resolve(GlobarParams.REPO_META_FILE_NAME);
        Files.writeString(metaFile, "{}");

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        RepositoryMeta result = RepositoryStatus.getRepositoryMeta();

        System.setProperty("user.dir", originalDir);

        assertNotNull(result);
    }

    @Test
    void getRepositoryMeta_shouldThrowNotARepoException_whenNoRepoFound() throws Exception {
        Path tempDir = Files.createTempDirectory("noRepo");

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        assertThrows(NotARepoException.class, () -> {
            RepositoryStatus.getRepositoryMeta();
        });

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void getRepositoryMeta_shouldThrowIOException_whenMetaFileMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("repo");
        Path metaDir = tempDir.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        assertThrows(IOException.class, () -> {
            RepositoryStatus.getRepositoryMeta();
        });

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void getRepositoryMeta_shouldThrowIOException_whenJsonInvalid() throws Exception {
        Path tempDir = Files.createTempDirectory("repo");
        Path metaDir = tempDir.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Path metaFile = metaDir.resolve(GlobarParams.REPO_META_FILE_NAME);
        Files.writeString(metaFile, "invalid-json");

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        assertThrows(IOException.class, () -> {
            RepositoryStatus.getRepositoryMeta();
        });

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void findRepositoryRoot_shouldReturnCurrentDir_whenRepoExistsThere() throws Exception {
        Path tempDir = Files.createTempDirectory("repo");
        Files.createDirectory(tempDir.resolve(GlobarParams.REPO_META_DIR));

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        Path result = RepositoryStatus.findRepositoryRoot();

        System.setProperty("user.dir", originalDir);

        assertEquals(tempDir, result);
    }

    @Test
    void findRepositoryRoot_shouldReturnParentDir_whenRepoExistsInParent() throws Exception {
        Path parent = Files.createTempDirectory("repo");
        Files.createDirectory(parent.resolve(GlobarParams.REPO_META_DIR));

        Path child = Files.createDirectory(parent.resolve("child"));

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", child.toString());

        Path result = RepositoryStatus.findRepositoryRoot();

        System.setProperty("user.dir", originalDir);

        assertEquals(parent, result);
    }

    @Test
    void findRepositoryRoot_shouldThrowException_whenNoRepoExists() throws Exception {
        Path tempDir = Files.createTempDirectory("noRepo");

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());

        assertThrows(NotARepoException.class, () -> {
            RepositoryStatus.findRepositoryRoot();
        });

        System.setProperty("user.dir", originalDir);
    }

    @Test
    void analyzeWorkspace_shouldDetectAddedFile() throws Exception {
        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve(GlobarParams.REPO_META_FILE_NAME), "{\"revision\":1}");
        Files.writeString(metaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME), "[]");

        Path newFile = repo.resolve("file.txt");
        Files.writeString(newFile, "content");

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", repo.toString());

        RepositoryStatus.StatusResult result = RepositoryStatus.analyzeWorkspace();

        System.setProperty("user.dir", originalDir);

        assertTrue(result.added.containsKey("file.txt"));
    }

    @Test
    void analyzeWorkspace_shouldDetectModifiedFile() throws Exception {
        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Path file = repo.resolve("file.txt");
        Files.writeString(file, "new");

        String checksum = RepositoryStatus.calculateChecksum(file);

        Files.writeString(metaDir.resolve(GlobarParams.REPO_META_FILE_NAME), "{\"revision\":1}");
        Files.writeString(
                metaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME),
                "[{\"path\":\"file.txt\",\"checksum\":\"wrong\",\"itemType\":\"FILE\"}]"
        );

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", repo.toString());

        RepositoryStatus.StatusResult result = RepositoryStatus.analyzeWorkspace();

        System.setProperty("user.dir", originalDir);

        assertTrue(result.modified.containsKey("file.txt"));
    }

    @Test
    void analyzeWorkspace_shouldDetectDeletedFile() throws Exception {
        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Files.writeString(metaDir.resolve(GlobarParams.REPO_META_FILE_NAME), "{\"revision\":1}");
        Files.writeString(
                metaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME),
                "[{\"path\":\"file.txt\",\"checksum\":\"abc\",\"itemType\":\"FILE\"}]"
        );

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", repo.toString());

        RepositoryStatus.StatusResult result = RepositoryStatus.analyzeWorkspace();

        System.setProperty("user.dir", originalDir);

        assertTrue(result.deleted.containsKey("file.txt"));
    }

    @Test
    void analyzeWorkspace_shouldNotMarkUnchangedFile() throws Exception {
        Path repo = Files.createTempDirectory("repo");
        Path metaDir = repo.resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(metaDir);

        Path file = repo.resolve("file.txt");
        Files.writeString(file, "same");

        String checksum = RepositoryStatus.calculateChecksum(file);

        Files.writeString(metaDir.resolve(GlobarParams.REPO_META_FILE_NAME), "{\"revision\":1}");
        Files.writeString(
                metaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME),
                "[{\"path\":\"file.txt\",\"checksum\":\"" + checksum + "\",\"itemType\":\"FILE\"}]"
        );

        String originalDir = System.getProperty("user.dir");
        System.setProperty("user.dir", repo.toString());

        RepositoryStatus.StatusResult result = RepositoryStatus.analyzeWorkspace();

        System.setProperty("user.dir", originalDir);

        assertTrue(result.added.isEmpty());
        assertTrue(result.modified.isEmpty());
        assertTrue(result.deleted.isEmpty());
    }

    @Test
    void statusResult_shouldInitializeFieldsCorrectly() {
        Path path = Path.of("test");
        long revision = 5;

        RepositoryStatus.StatusResult result = new RepositoryStatus.StatusResult(path, revision);

        assertEquals(path, result.repoRoot);
        assertEquals(revision, result.revision);
        assertNotNull(result.added);
        assertNotNull(result.modified);
        assertNotNull(result.deleted);
    }

}