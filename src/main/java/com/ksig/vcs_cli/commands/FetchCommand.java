package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

@Command(name = "fetch", description = "Fetch info for latest revision")
public class FetchCommand implements Callable<Integer> {

    private final BackendRestClient backendRestClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public FetchCommand() {
        this(new BackendRestClient());
    }

    public FetchCommand(BackendRestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    @Override
    public Integer call() {
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

        Path repoMetaDir = statusResult.repoRoot.resolve(GlobarParams.REPO_META_DIR);
        Path repoMetaJsonPath = repoMetaDir.resolve(GlobarParams.REPO_META_FILE_NAME);

        URI uri;
        try {
            uri = new URIBuilder(repoMeta.getUrl()).appendPath("fetch").build();
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
        } catch (Exception e) {
            System.err.println("Failed to fetch items from repository!");
            System.err.println(e.getMessage());
            return 1;
        }

        // ✅ FIXED PATH HERE
        Path itemMeta = repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME);

        try {
            Files.write(itemMeta, response.getBytes());

            List<ItemMeta> itemMetas =
                    mapper.readValue(response, new TypeReference<List<ItemMeta>>() {});

            long currentRevision = itemMetas.stream()
                    .mapToLong(ItemMeta::getRevisionNumber)
                    .max()
                    .orElse(0);

            repoMeta.setRevision(currentRevision);

            Files.writeString(repoMetaJsonPath,
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(repoMeta));

        } catch (IOException e) {
            System.err.println("Failed to write items to repository meta!");
            return 1;
        }

        return 0;
    }
}