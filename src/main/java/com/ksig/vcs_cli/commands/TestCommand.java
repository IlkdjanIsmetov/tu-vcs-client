package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.http.BackendRestClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.net.URI;
import java.util.concurrent.Callable;

import static com.ksig.vcs_cli.globalParams.GlobarParams.APP_BASE_URL;

@Command(name = "test", description = "Test command")
public class TestCommand implements Callable<Integer> {
    private static final BackendRestClient backendRestClient = new BackendRestClient();
    @Override
    public Integer call() throws Exception {
        URI uri = new URIBuilder(APP_BASE_URL).appendPath("private").appendPath("test").build();
        HttpGet httpGet = new HttpGet(uri);
        String response = backendRestClient.executeAuthenticatedRequest(httpGet);
        System.out.println(response);
        return 0;
    }
}
