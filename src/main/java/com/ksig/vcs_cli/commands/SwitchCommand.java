package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.async.PullItemsTask;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.exceptions.NotLoggedInException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.SyncItemView;
import com.ksig.vcs_cli.models.enums.SyncStatus;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "switch", description = "Switch repository revision")
public class SwitchCommand implements Callable<Integer> {
    private final BackendRestClient backendRestClient;
    private final ObjectMapper mapper;

    @Option(names = {"-r", "--revision"}, required = false, description = "Revision to switch to")
    private long revision;

    @Option(names = {"-l", "--latest"}, required = false, description = "Switch to latest revision")
    private boolean latest;

    public SwitchCommand() {
        this.backendRestClient = new BackendRestClient();
        this.mapper = new ObjectMapper();
    }

    public SwitchCommand(BackendRestClient backendRestClient, ObjectMapper mapper) {
        this.backendRestClient = backendRestClient;
        this.mapper = mapper;
    }

    @Override
    public Integer call() throws Exception {
        RepositoryMeta repoMeta;
        RepositoryStatus.StatusResult statusResult;

        try {
            repoMeta = RepositoryStatus.getRepositoryMeta();
            statusResult = RepositoryStatus.analyzeWorkspace();
        } catch (IOException e) {
            System.err.println("Failed to retrieve repository meta");
            return 1;
        } catch (NotARepoException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        if (latest && revision > 0) {
            System.err.println("Error: You cannot specify both the 'latest' flag and a specific revision number.");
            return 1;
        }

        long lastRevisionNumber;
        try {
            lastRevisionNumber = getLatestRevNumber(repoMeta);
        } catch (NotLoggedInException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Failed to read last revision number!");
            System.err.println(e.getMessage());
            return 1;
        }

        if (!latest) {
            if (revision > lastRevisionNumber) {
                System.err.println("The revision number you want is greater than latest revision number!");
                return 1;
            } else if (revision <= 0) {
                System.err.println("Revision number must be greater than zero!");
                return 1;
            }
        }


        Path repoMetaDir = statusResult.repoRoot.resolve(GlobarParams.REPO_META_DIR);
        Path repoMetaJsonPath = repoMetaDir.resolve(GlobarParams.REPO_META_FILE_NAME);
        Path itemMetaJsonPath = repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME);
        URI baseURL = new URI(repoMeta.getUrl());
        URI uri;
        try {
            URIBuilder uriBuilder = new URIBuilder(baseURL).appendPath("fetch");
            if (!latest) {
                uriBuilder.addParameter("revisionNumber", String.valueOf(revision));
            }
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            System.err.println("tu_vcs_repo/repo.json url is invalid");
            return 1;
        }

        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-Type", "application/json");


        String response;
        try {
            response = backendRestClient.executeStringRequest(httpGet);
        } catch (NotLoggedInException e) {
            System.err.println(e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Failed to fetch items from repository!");
            System.err.println(e.getMessage());
            return 1;
        }

        List<ItemMeta> currentItems = mapper.readValue(
                itemMetaJsonPath.toFile(),
                new TypeReference<List<ItemMeta>>() {
                }
        );

        Map<String, ItemMeta> localItemsMap = currentItems.stream().collect(Collectors.toMap(ItemMeta::getPath, item -> item));


        List<ItemMeta> revisionItems = mapper.readValue(response, new TypeReference<List<ItemMeta>>() {
        });

        List<SyncItemView> itemsToSync = new ArrayList<SyncItemView>();

        for (ItemMeta revisionItem : revisionItems) {
            ItemMeta localItem = localItemsMap.get(revisionItem.getPath());
            if (localItem == null) {
                itemsToSync.add(buildSyncView(revisionItem, SyncStatus.NEW_REMOTE));
                continue;
            }

            if (localItem.getChecksum().equals(revisionItem.getChecksum())) {
                //едни същи са
                localItemsMap.remove(revisionItem.getPath());
            } else {
                itemsToSync.add(buildSyncView(revisionItem, SyncStatus.MODIFIED_REMOTE));
                localItemsMap.remove(revisionItem.getPath());
            }
        }

        for (ItemMeta localRemaining : localItemsMap.values()) {
            itemsToSync.add(SyncItemView.builder()
                    .path(localRemaining.getPath())
                    .status(SyncStatus.DELETED_REMOTE)
                    .build());
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

            System.out.println("Switch successful.");
            try {
                Files.write(itemMetaJsonPath, response.getBytes());
            } catch (Exception e) {
                System.err.println("Failed to write items metadata!");
                return 1;
            }
        }
        return 0;

    }

    private long getLatestRevNumber(RepositoryMeta repoMeta) throws Exception {
        URI uri = new URIBuilder(repoMeta.getUrl()).appendPath("latestRevNumber").build();
        HttpGet httpGet = new HttpGet(uri);
        return Long.parseLong(backendRestClient.executeStringRequest(httpGet));
    }

    private SyncItemView buildSyncView(ItemMeta remote, SyncStatus status) {
        return SyncItemView.builder()
                .itemId(remote.getId())
                .path(remote.getPath())
                .status(status)
                .serverChecksum(remote.getChecksum())
                .storageKey(remote.getStorageKey())
                .serverRevisionNumber(remote.getRevisionNumber())
                .itemType(remote.getItemType())
                .build();
    }
}
