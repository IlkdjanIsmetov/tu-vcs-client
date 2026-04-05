package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "delete", description = "Delete this repository")
public class DeleteCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        System.out.println("Are you sure you want to delete this repository?");
        String yesNo = System.console().readLine("Y/N: ");
        if (yesNo.equalsIgnoreCase("N")) {
            return 0;
        }
        if (!yesNo.equalsIgnoreCase("Y")) {
            System.err.println("Please enter yes or no");
            return 1;
        }
        RepositoryMeta repoMeta = null;
        try {
            repoMeta = RepositoryStatus.getRepositoryMeta();
        } catch (IOException e) {
            System.err.println("Error getting repository meta");
            return 1;
        } catch (NotARepoException e) {
            System.err.println(e.getMessage());
            return 1;
        }
        URI uri = null;
        try {
            uri = new URIBuilder(repoMeta.getUrl())
                    .appendPath("delete")
                    .build();
        } catch (URISyntaxException e) {
            System.err.println("tu_vcs_repo/repo.json  url is invalid");
            return 1;
        }
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept", "application/json");
        httpDelete.setHeader("Content-Type", "application/json");
        BackendRestClient backendRestClient = new BackendRestClient();
        try {
            backendRestClient.executeStringRequest(httpDelete);
        } catch (Exception e) {
            System.err.println("Failed to delete repository! Try again later.");
            return 1;
        }
        Path root = null;
        try {
            root = RepositoryStatus.findRepositoryRoot();
        } catch (NotARepoException e) {
            throw new RuntimeException(e);
        }
        Path repoMetaDIr = root.resolve(GlobarParams.REPO_META_DIR);
        try {
            Files.deleteIfExists(repoMetaDIr);
        } catch (IOException e) {
            System.err.println("Failed to delete repo meta");
        }
        System.out.println("This repository is no longer a tu-vcs repository and has been deleted remotely.");
        return 0;
    }
}
