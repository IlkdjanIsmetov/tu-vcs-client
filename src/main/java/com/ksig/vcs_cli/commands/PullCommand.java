package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.async.PullItemsTask;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.LocalItemMetadata;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.SyncItemView;
import com.ksig.vcs_cli.models.enums.SyncStatus;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "pull", description = "Get latest changes from repository")
public class PullCommand implements Callable<Integer> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final BackendRestClient backendRestClient = new BackendRestClient();

    @Override
    public Integer call() throws Exception {
        //вземам инфоримация за репото
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
        List<ItemMeta> itemsMeta = null;
        try {
            repoMeta = mapper.readValue(repoMetaJsonPath.toFile(), RepositoryMeta.class);
            itemsMeta = mapper.readValue(itemsJsonPath.toFile(), new TypeReference<List<ItemMeta>>() {});
        } catch (IOException e) {
            System.err.println("Failed to read repository meta!");
            return 1;
        }

        long lastRevisionNumber;
        try {
            lastRevisionNumber = getLatestRevNumber(repoMeta);
        } catch (Exception e) {
            System.err.println("Failed to read last revision number!");
            System.err.println(e.getMessage());
            return 1;
        }
        if (repoMeta.getRevision() == lastRevisionNumber) {
            System.out.println("You are up to date!");
            return 0;
        }
        //правя рекуест
        List<LocalItemMetadata> requestItems = itemsMeta.stream().map(LocalItemMetadata::fromItemMeta).toList();
        URI baseURL = new URI(repoMeta.getUrl());
        URI uri = new URIBuilder(baseURL).appendPath("sync-status").build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setEntity(new StringEntity(mapper.writeValueAsString(requestItems)));
        String response = null;
        try {
            response = backendRestClient.executeStringRequest(httpPost);
        } catch (IOException e) {
            System.err.println("Failed to execute http request!");
            System.err.println(e.getMessage());
            return 1;
        }
        List<SyncItemView> itemsToSync = mapper.readValue(response,  new TypeReference<List<SyncItemView>>() {});
        //ако има конфликти прекратявам
        List<SyncItemView> conflictedItems = itemsToSync.stream().filter(item -> item.getStatus().equals(SyncStatus.CONFLICT)).toList();
        if (!conflictedItems.isEmpty()) {
            System.out.println("Conflicts found for files: ");
            for  (SyncItemView item : conflictedItems) {
                System.out.println(item.getPath());
            }
            System.out.println("Pull request has stopped. Consider fixing conflicts");
            return 1;
        }
        // за ламбдата
        RepositoryStatus.StatusResult finalStatusResult = statusResult;
        List<Future<Void>> asyncTasks = null;
        try (ExecutorService executor = Executors.newFixedThreadPool(itemsToSync.size())) {

            asyncTasks = itemsToSync.stream()
                    .peek(item -> item.setLocalPath(finalStatusResult.repoRoot.resolve(item.getPath())))
                    .map(item -> executor.submit(new PullItemsTask(item, backendRestClient, baseURL))).toList();

            for (Future<Void> future : asyncTasks) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    System.err.println("A synchronization task failed: " + e.getCause());

                    executor.shutdownNow();

                    return 1;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Main thread was interrupted while waiting.");
                    return 1;
                }
            }

            System.out.println("Pull successful.");
            try {
                fetchAndUpdateLocalState(itemsJsonPath, repoMeta, repoMetaJsonPath);
            } catch (Exception e) {
                System.err.println("Failed to fetch and update local state! Run tu-vcs fetch!");
            }
        }
        return 0;
    }

    private void fetchAndUpdateLocalState(Path itemsJsonPath, RepositoryMeta repoMeta, Path repoMetaJsonPath) throws Exception {
        URI uri = new URIBuilder(repoMeta.getUrl())
                .appendPath("fetch")
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
        //вземам най-големия revision, тест сегашния
        long currentRevision = fetchedItems.stream()
                .mapToLong(ItemMeta::getRevisionNumber)
                .max()
                .orElse(0);
        repoMeta.setRevision(currentRevision);
        Files.writeString(repoMetaJsonPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(repoMeta));
        System.out.println("Local tracking state updated.");
    }

    private long getLatestRevNumber(RepositoryMeta repoMeta) throws Exception {
        URI uri = new URIBuilder(repoMeta.getUrl()).appendPath("latestRevNumber").build();
        HttpGet httpGet = new HttpGet(uri);
        return Long.parseLong(backendRestClient.executeStringRequest(httpGet));
    }
}
