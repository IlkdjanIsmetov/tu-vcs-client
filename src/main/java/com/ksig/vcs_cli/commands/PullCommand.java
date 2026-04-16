package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.LocalItemMetadata;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.SyncItemView;
import com.ksig.vcs_cli.models.enums.SyncStatus;
import com.ksig.vcs_cli.utils.RepositoryStatus;
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
        //правя рекуест
        List<LocalItemMetadata> requestItems = itemsMeta.stream().map(LocalItemMetadata::fromItemMeta).toList();
        URI uri = new URIBuilder(repoMeta.getUrl()).appendPath("sync-status").build();
        HttpPost httpPost = new HttpPost(uri);
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
        List<SyncItemView> deletedItems = itemsToSync.stream()
                .filter(item -> item.getStatus().equals(SyncStatus.DELETED_REMOTE))
                .peek(item -> item.setPath(String.valueOf(finalStatusResult.repoRoot.resolve(item.getPath()))))
                .toList();
        List<SyncItemView> newItems =  itemsToSync.stream()
                .filter(item -> item.getStatus().equals(SyncStatus.NEW_REMOTE))
                .peek(item -> item.setPath(String.valueOf(finalStatusResult.repoRoot.resolve(item.getPath()))))
                .toList();
        List<SyncItemView> modifiedItems = itemsToSync.stream().filter(item -> item.getStatus().equals(SyncStatus.MODIFIED_REMOTE)).toList();
        return 0;

        //TODO FINISH LATER
    }
}
