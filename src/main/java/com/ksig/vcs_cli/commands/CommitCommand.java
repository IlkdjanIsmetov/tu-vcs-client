package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.ItemRequest;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.enums.Action;
import com.ksig.vcs_cli.utils.RepositoryStatus;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.StringBody;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;

import static com.ksig.vcs_cli.models.enums.ItemType.DIRECTORY;
import static com.ksig.vcs_cli.models.enums.ItemType.FILE;

@Command(name = "commit", description = "Commit changes to the server")
public class CommitCommand implements Callable<Integer> {

    private final ObjectMapper mapper = new ObjectMapper();
    private final BackendRestClient backendRestClient = new BackendRestClient();

    @Option(names = {"-m", "--message"}, required = true, description = "Commit message")
    private String message;

    @Override
    public Integer call() throws Exception {
        Path currentWorkDir = Path.of(System.getProperty("user.dir"));

        RepositoryStatus.StatusResult statusResult = RepositoryStatus.analyzeWorkspace(currentWorkDir);

        if (statusResult == null) {
            System.err.println("fatal: not a tu-vcs repository");
            return 1;
        }

        Path repoMetaDir = statusResult.repoRoot.resolve(GlobarParams.REPO_META_DIR);
        Path repoMetaJsonPath = repoMetaDir.resolve(GlobarParams.REPO_META_FILE_NAME);
        Path itemsJsonPath = repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME);
        RepositoryMeta repoMeta = mapper.readValue(repoMetaJsonPath.toFile(), RepositoryMeta.class);
        UUID repositoryId = repoMeta.getId();

        List<ItemMeta> trackedList = mapper.readValue(
                itemsJsonPath.toFile(),
                new TypeReference<List<ItemMeta>>() {}
        );

        Map<String, ItemMeta> trackedItems = new HashMap<>();
        if (trackedList != null) {
            for (ItemMeta item : trackedList) {
                trackedItems.put(item.getPath(), item);
            }
        }
        if (trackedItems == null) trackedItems = new HashMap<>();

        List<ItemRequest> itemsToCommit = new ArrayList<>();
        List<File> filesToUpload = new ArrayList<>();

        for (Map.Entry<String, Path> entry : statusResult.added.entrySet()) {
            boolean isDirectory = Files.isDirectory(entry.getValue());
            ItemRequest req = createItemRequest(null, entry.getKey(), isDirectory, Action.ADD, entry.getValue());
            itemsToCommit.add(req);
            if (!isDirectory) filesToUpload.add(entry.getValue().toFile());
        }

        for (Map.Entry<String, Path> entry : statusResult.modified.entrySet()) {
            UUID existingId = trackedItems.get(entry.getKey()).getId();
            ItemRequest req = createItemRequest(existingId, entry.getKey(), false, Action.MODIFY, entry.getValue());
            itemsToCommit.add(req);
            filesToUpload.add(entry.getValue().toFile());
        }

        for (Map.Entry<String, ItemMeta> entry : statusResult.deleted.entrySet()) {
            ItemMeta meta = entry.getValue();
            ItemRequest req = new ItemRequest();
            req.setItemId(meta.getId());
            req.setPath(meta.getPath());
            req.setItemType(meta.getItemType() == FILE ? FILE : DIRECTORY);
            req.setAction(Action.DELETE);
            itemsToCommit.add(req);
        }

        if (itemsToCommit.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean.");
            return 0;
        }

        sendCommitRequest(repositoryId, itemsToCommit, filesToUpload, repoMeta.getUrl());
        System.out.println("Commit successful.");
        fetchAndUpdateLocalState(repositoryId, itemsJsonPath, repoMeta.getUrl());

        return 0;
    }

    private void sendCommitRequest(UUID repositoryId, List<ItemRequest> items, List<File> files, String url) throws Exception {
        URI uri = new URIBuilder(url)
                .appendPathSegments("repositories", repositoryId.toString(), "commit")
                .setParameter("message", message)
                .build();

        HttpPost httpPost = new HttpPost(uri);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        String jsonPaths = mapper.writeValueAsString(items);
        builder.addPart("paths", new StringBody(jsonPaths, ContentType.APPLICATION_JSON));

        for (File file : files) {
            String fileRefName = file.getPath().replace("\\", "/");
            builder.addPart("files", new FileBody(file, ContentType.APPLICATION_OCTET_STREAM, fileRefName));
        }

        httpPost.setEntity(builder.build());
        backendRestClient.executeAuthenticatedRequest(httpPost);
    }

    private void fetchAndUpdateLocalState(UUID repositoryId, Path itemsJsonPath, String url) throws Exception {
        URI uri = new URIBuilder(url)
                .appendPathSegments("repositories", repositoryId.toString(), "fetch")
                .setParameter("repositoryId", repositoryId.toString())
                .build();

        HttpGet httpGet = new HttpGet(uri);
        String response = backendRestClient.executeAuthenticatedRequest(httpGet);

        List<ItemMeta> fetchedItems = mapper.readValue(response, new TypeReference<List<ItemMeta>>() {});
        Files.writeString(itemsJsonPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fetchedItems));
        System.out.println("Local tracking state updated.");
    }

    private ItemRequest createItemRequest(UUID id, String path, boolean isDir, Action action, Path physicalPath) throws IOException {
        ItemRequest inView = new ItemRequest();
        inView.setItemId(id);
        inView.setPath(path);
        inView.setItemType(isDir ? DIRECTORY : FILE);
        inView.setAction(action);

        if (!isDir && action != Action.DELETE) {
            inView.setFileSize((int) Files.size(physicalPath));
            inView.setChecksum(RepositoryStatus.calculateChecksum(physicalPath));
            inView.setFileRef(physicalPath.toString().replace("\\", "/"));
        }
        return inView;
    }
}