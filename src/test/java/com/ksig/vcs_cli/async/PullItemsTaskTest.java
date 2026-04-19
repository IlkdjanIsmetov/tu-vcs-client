package com.ksig.vcs_cli.async;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.SyncItemView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;

import java.lang.reflect.Field;


class PullItemsTaskTest {
    @Test
    void constructor_shouldAssignFields() throws Exception {
        SyncItemView item = new SyncItemView();
        BackendRestClient client = new BackendRestClient();
        URI uri = new URI("http://localhost");

        PullItemsTask task = new PullItemsTask(item, client, uri);

        Field itemField = PullItemsTask.class.getDeclaredField("itemToSync");
        Field clientField = PullItemsTask.class.getDeclaredField("backendRestClient");
        Field uriField = PullItemsTask.class.getDeclaredField("baseURL");

        itemField.setAccessible(true);
        clientField.setAccessible(true);
        uriField.setAccessible(true);

        assertEquals(item, itemField.get(task));
        assertEquals(client, clientField.get(task));
        assertEquals(uri, uriField.get(task));
    }

    @Test
    void call_shouldDeleteFile_whenStatusDeletedRemote() throws Exception {
        Path tempFile = Files.createTempFile("test", ".txt");

        SyncItemView item = new SyncItemView();
        item.setStatus(com.ksig.vcs_cli.models.enums.SyncStatus.DELETED_REMOTE);
        item.setLocalPath(tempFile);
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        task.call();

        assertFalse(Files.exists(tempFile));
    }

    @Test
    void call_shouldDownloadFile_whenStatusNewRemote() throws Exception {
        FakeBackend backend = new FakeBackend();

        SyncItemView item = new SyncItemView();
        item.setStatus(com.ksig.vcs_cli.models.enums.SyncStatus.NEW_REMOTE);
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(Path.of("dummy.txt"));
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, backend, new URI("http://localhost"));

        task.call();

        assertTrue(backend.downloadCalled);
    }

    @Test
    void call_shouldCreateDirectory_whenNewRemoteDirectory() throws Exception {
        Path dir = Files.createTempDirectory("parent").resolve("newDir");

        SyncItemView item = new SyncItemView();
        item.setStatus(com.ksig.vcs_cli.models.enums.SyncStatus.NEW_REMOTE);
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.DIRECTORY);
        item.setLocalPath(dir);
        item.setPath("newDir");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        task.call();

        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void call_shouldReplaceFile_whenModifiedRemote() throws Exception {
        Path file = Files.createTempFile("test", ".txt");

        FakeBackend backend = new FakeBackend();

        SyncItemView item = new SyncItemView();
        item.setStatus(com.ksig.vcs_cli.models.enums.SyncStatus.MODIFIED_REMOTE);
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(file);
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, backend, new URI("http://localhost"));

        task.call();

        assertTrue(backend.downloadCalled);
    }

    @Test
    void deleteLocalFile_shouldDeleteFile() throws Exception {
        Path file = Files.createTempFile("test", ".txt");

        SyncItemView item = new SyncItemView();
        item.setLocalPath(file);
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("deleteLocalFile", SyncItemView.class);
        method.setAccessible(true);

        method.invoke(task, item);

        assertFalse(Files.exists(file));
    }

    @Test
    void deleteLocalFile_shouldHandleException_whenFileMissing() throws Exception {
        Path nonExisting = Path.of("non-existing-file.txt");

        SyncItemView item = new SyncItemView();
        item.setLocalPath(nonExisting);
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("deleteLocalFile", SyncItemView.class);
        method.setAccessible(true);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        method.invoke(task, item);

        assertTrue(err.toString().contains("Error deleting local file"));
    }

    @Test
    void downloadRemoteFile_shouldCreateDirectory_whenItemIsDirectory() throws Exception {
        Path dir = Files.createTempDirectory("parent").resolve("newDir");

        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.DIRECTORY);
        item.setLocalPath(dir);
        item.setPath("newDir");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("downloadRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        method.invoke(task, item);

        assertTrue(Files.exists(dir));
        assertTrue(Files.isDirectory(dir));
    }

    @Test
    void downloadRemoteFile_shouldCallBackend_whenItemIsFile() throws Exception {
        FakeBackend backend = new FakeBackend();

        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(Path.of("dummy.txt"));
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, backend, new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("downloadRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        method.invoke(task, item);

        assertTrue(backend.downloadCalled);
    }

    @Test
    void downloadRemoteFile_shouldHandleException() throws Exception {
        BackendRestClient failingBackend = new BackendRestClient() {
            @Override
            public Path downloadFileWithFileName(
                    org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                    Path path) {
                throw new RuntimeException("fail");
            }
        };

        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(Path.of("invalid/path/file.txt"));
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, failingBackend, new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("downloadRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        method.invoke(task, item);

        assertTrue(err.toString().contains("Error downloading remote file"));
    }

    @Test
    void handleModifiedRemoteFile_shouldIgnore_whenDirectory() throws Exception {
        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.DIRECTORY);
        item.setPath("dir");

        PullItemsTask task = new PullItemsTask(item, new FakeBackend(), new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("handleModifiedRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        method.invoke(task, item);

        assertTrue(out.toString().contains("Ignored modified status"));
    }

    @Test
    void handleModifiedRemoteFile_shouldReplaceFile_whenFile() throws Exception {
        Path file = Files.createTempFile("test", ".txt");

        FakeBackend backend = new FakeBackend();

        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(file);
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, backend, new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("handleModifiedRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        method.invoke(task, item);

        assertTrue(backend.downloadCalled);
    }

    @Test
    void handleModifiedRemoteFile_shouldHandleException() throws Exception {
        BackendRestClient failingBackend = new BackendRestClient() {
            @Override
            public Path downloadFileWithFileName(
                    org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                    Path path) {
                throw new RuntimeException("fail");
            }
        };

        SyncItemView item = new SyncItemView();
        item.setItemType(com.ksig.vcs_cli.models.enums.ItemType.FILE);
        item.setLocalPath(Path.of("invalid/path/file.txt"));
        item.setStorageKey("key");
        item.setPath("file.txt");

        PullItemsTask task = new PullItemsTask(item, failingBackend, new URI("http://localhost"));

        Method method = PullItemsTask.class.getDeclaredMethod("handleModifiedRemoteFile", SyncItemView.class);
        method.setAccessible(true);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        method.invoke(task, item);

        assertTrue(err.toString().contains("Error handling remote file"));
    }

    class FakeBackend extends BackendRestClient {

        boolean downloadCalled = false;

        @Override
        public Path downloadFileWithFileName(
                org.apache.hc.client5.http.classic.methods.HttpUriRequestBase request,
                Path path) {

            downloadCalled = true;
            return path;
        }
    }

}