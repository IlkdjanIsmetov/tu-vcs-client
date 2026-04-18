package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryRequest;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create repository")
public class CreateRepositoryCommand implements Callable<Integer> {
    ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    static final BackendRestClient backendRestClient = new BackendRestClient();
    @Option(names = {"-n", "--name"}, required = true, description = "repository name")
    private String repository;

    @Option(names = {"-u", "--url"}, required = true, description = "url for the remote version control server")
    private String serverUrl;

    @Option(names = {"-ra", "reqApp"}, description = "Require approval by repo master for commits")
    private boolean requireApproval;

    @Override
    public Integer call() {
        RepositoryRequest repoRequest = createRequestModel();
        String response = null;
        try {
            response = sendRequestToServer(repoRequest);
        } catch (Exception e) {
            System.err.println("Failed to send repository request!");
            System.err.println(e.getMessage());
            return 1;
        }
        try {
            createMetaDir(response);
        } catch (FileAlreadyExistsException e) {
            System.err.println("This is already an tu-vcs repository");
            return 1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Repository created!");
        return 0;
    }

    private RepositoryRequest createRequestModel() {
        RepositoryRequest repoRequest = new RepositoryRequest();
        repoRequest.setRepositoryName(repository);
        repoRequest.setRequireApproval(requireApproval);
        String description = System.console().readLine("Enter description: ");
        repoRequest.setDescription(description);
        return repoRequest;
    }

    private String sendRequestToServer(RepositoryRequest repoRequest) throws Exception {
        URI uri = new URIBuilder(serverUrl).appendPath("repositories").appendPath("create").build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setEntity(new StringEntity(objectWriter.writeValueAsString(repoRequest)));
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        return backendRestClient.executeStringRequest(httpPost);
    }

    private void createMetaDir(String response) throws IOException {
        Path repoMetaDir = Path.of(System.getProperty("user.dir")).resolve(GlobarParams.REPO_META_DIR);
        Files.createDirectories(repoMetaDir);
        Path repoMeta = Files.createFile(repoMetaDir.resolve(GlobarParams.REPO_META_FILE_NAME));
        Path fileMeta = Files.createFile(repoMetaDir.resolve(GlobarParams.ITEMS_META_FILE_NAME));
        Files.write(repoMeta, response.getBytes());
        //няма item-и когато се създава репото
        Files.write(fileMeta, "[]".getBytes());
    }
}
