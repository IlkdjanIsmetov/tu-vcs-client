package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.exceptions.NotARepoException;
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
    public Integer call() {
        RepositoryStatus.StatusResult statusResult = null;
        try {
            statusResult = RepositoryStatus.analyzeWorkspace();
        } catch (IOException e) {
            System.err.println("Failed to analyze workspace!");
            return 1;
        } catch (NotARepoException e) {
            System.err.println(e.getMessage());
            return 1;
        }


        Path repoMetaDir = statusResult.repoRoot.resolve(GlobarParams.REPO_META_DIR);
        Path repoMetaJsonPath = repoMetaDir.resolve(GlobarParams.REPO_META_FILE_NAME);
        Path itemsJsonPath = repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME);
        RepositoryMeta repoMeta = null;
        try {
            repoMeta = mapper.readValue(repoMetaJsonPath.toFile(), RepositoryMeta.class);
        } catch (IOException e) {
            System.err.println("Failed to read repository meta!");
            return 1;
        }
        UUID repositoryId = repoMeta.getId();

        List<ItemMeta> trackedList = null;
        try {
            trackedList = mapper.readValue(
                    itemsJsonPath.toFile(),
                    new TypeReference<List<ItemMeta>>() {}
            );
        } catch (IOException e) {
            System.err.println("Failed to read items meta!");
            return 1;
        }

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

        String response = null;
        try {
            response = sendCommitRequest(repositoryId, itemsToCommit, filesToUpload, repoMeta.getUrl());
        } catch (Exception e) {
            System.err.println("Failed to send commit request!");
            System.out.println(e + e.getMessage());
            return 1;
        }
        if (response.equals("OK")) {
            System.out.println("Commit successful.");
            try {
                fetchAndUpdateLocalState(repositoryId, itemsJsonPath, repoMeta.getUrl());
            } catch (Exception e) {
                System.err.println("Failed to fetch and update local state! Run tu-vcs fetch!");
            }
        } else {
            System.err.println("Commit failed! Try again later.");
            return 1;
        }
        return 0;
    }

    private String sendCommitRequest(UUID repositoryId, List<ItemRequest> items, List<File> files, String url) throws Exception {
        URI uri = new URIBuilder(url)
                .appendPath("commit")
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
        return backendRestClient.executeStringRequest(httpPost);
    }

    private void fetchAndUpdateLocalState(UUID repositoryId, Path itemsJsonPath, String url) throws Exception {
        URI uri = new URIBuilder(url)
                .appendPath("fetch")
                .setParameter("repositoryId", repositoryId.toString())
                .build();

        HttpGet httpGet = new HttpGet(uri);
        String response = backendRestClient.executeStringRequest(httpGet);

        List<ItemMeta> fetchedItems = null;
        try {
            fetchedItems = mapper.readValue(response, new TypeReference<List<ItemMeta>>() {});
        } catch (Exception e) {
            System.err.println("Fetch failed! Try tu-vcs fetch again later.");
            return;
        }
        Files.writeString(itemsJsonPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fetchedItems));
        System.out.println("Local tracking state updated.");
    }

    private ItemRequest createItemRequest(UUID id, String path, boolean isDir, Action action, Path physicalPath) {
        ItemRequest inView = new ItemRequest();
        inView.setItemId(id);
        inView.setPath(path);
        inView.setItemType(isDir ? DIRECTORY : FILE);
        inView.setAction(action);

        if (!isDir && action != Action.DELETE) {
            try {
                inView.setFileSize((int) Files.size(physicalPath));
            }  catch (IOException e) {
                inView.setFileSize(0);
            }
            inView.setChecksum(RepositoryStatus.calculateChecksum(physicalPath));
            inView.setFileRef(physicalPath.toString().replace("\\", "/"));
        }
        return inView;
    }
}