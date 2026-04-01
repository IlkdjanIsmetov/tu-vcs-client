package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "fetch", description = "Fetch info for latest revision")
public class FetchCommand implements Callable<Integer> {
    private static final BackendRestClient backendRestClient = new BackendRestClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Integer call() throws Exception {
        RepositoryMeta repoMeta = null;
        Path repoRoot = RepositoryStatus.findRepositoryRoot();
        Path repoMetaFile = repoRoot.resolve(GlobarParams.REPO_META_DIR).resolve(GlobarParams.REPO_META_FILE_NAME);
        repoMeta = objectMapper.readValue(repoMetaFile.toFile(), RepositoryMeta.class);
        URI uri = new URIBuilder(repoMeta.getUrl()).appendPath("fetch").build();
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-Type", "application/json");
        String response = backendRestClient.executeStringRequest(httpGet);
        Path itemMeta = Path.of(GlobarParams.REPO_META_DIR).resolve(GlobarParams.ITEMS_META_FILE_NAME);
        Files.write(itemMeta, response.getBytes());
        return 0;
    }
}
