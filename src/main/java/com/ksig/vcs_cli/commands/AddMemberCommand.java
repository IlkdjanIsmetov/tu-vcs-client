package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.models.enums.Role;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine;

import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "addMember", description = "Add member to this repository")
public class AddMemberCommand implements Callable<Integer> {

    @Option(names = {"-u", "--username"}, required = true, description = "User to to the repository")
    private String username;

    @Option(names = {"-r", "--role"}, required = true, description = "The role of the user to be added")
    private String roleString;

    @Override
    public Integer call() throws Exception {
        RepositoryMeta repoMeta = RepositoryStatus.getRepositoryMeta();
        Role role = null;
        try {
            role = Role.valueOf(roleString.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid role: " + roleString);
            return 1;
        }
        URI uri = new URIBuilder(repoMeta.getUrl())
                .appendPath("addMember")
                .setParameter("username", username)
                .setParameter("role", role.toString())
                .build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");
        BackendRestClient  backendRestClient = new BackendRestClient();
        backendRestClient.executeStringRequest(httpPost);
        return 0;
    }
}
