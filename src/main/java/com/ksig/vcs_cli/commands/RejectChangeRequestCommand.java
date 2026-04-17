package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "rejectCR", description = "Reject changa request")
public class RejectChangeRequestCommand implements Callable<Integer> {
    private final BackendRestClient backendRestClient = new BackendRestClient();

    @Option(names = {"-i", "--id"}, required = true, description = "Change request id")
    private String changeRequestId;

    @Override
    public Integer call() throws Exception {
        RepositoryMeta repoMeta = null;
        try {
            repoMeta = RepositoryStatus.getRepositoryMeta();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NotARepoException e) {
            System.err.println(e.getMessage());
            return 1;
        }

        URI url = new URIBuilder(repoMeta.getUrl()).appendPath("change-request").appendPath(changeRequestId).appendPath("approve").build();
        HttpPost post = new HttpPost(url);
        try {
            backendRestClient.executeStringRequest(post);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }
}
