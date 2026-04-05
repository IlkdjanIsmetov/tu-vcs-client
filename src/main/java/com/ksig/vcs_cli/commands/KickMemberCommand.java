package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "kickMember", description = "Kick member from this repository")
public class KickMemberCommand implements Callable<Integer> {
    @Option(names = {"-u", "--username"}, required = true, description = "User to to the repository")
    private String username;

    @Override
    public Integer call() throws Exception {
        RepositoryMeta repoMeta = RepositoryStatus.getRepositoryMeta();
        URI uri = new URIBuilder(repoMeta.getUrl())
                .appendPath("kickMember")
                .setParameter("username", username)
                .build();
        HttpDelete httpDelete = new HttpDelete(uri);
        httpDelete.setHeader("Accept", "application/json");
        httpDelete.setHeader("Content-Type", "application/json");
        BackendRestClient backendRestClient = new BackendRestClient();
        try {
            backendRestClient.executeStringRequest(httpDelete);
        } catch (Exception e) {
            System.err.println("Failed to kick member from this repository!");
            return 1;
        }
        return 0;
    }
}
