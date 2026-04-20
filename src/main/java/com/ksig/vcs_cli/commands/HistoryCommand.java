package com.ksig.vcs_cli.commands;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ksig.vcs_cli.exceptions.NotARepoException;
import com.ksig.vcs_cli.exceptions.NotLoggedInException;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.http.BackendRestClient;
import com.ksig.vcs_cli.models.CommitHistoryView;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine.Command;

@Command(name = "history", description = "See commit history of this repository")
public class HistoryCommand implements Callable<Integer> {
    private final BackendRestClient backendRestClient;
    private final ObjectMapper mapper;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public HistoryCommand() {
        backendRestClient = new BackendRestClient();
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public Integer call() throws Exception {
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
        try {
            repoMeta = mapper.readValue(repoMetaJsonPath.toFile(), RepositoryMeta.class);
        } catch (IOException e) {
            System.err.println("Failed to read repository meta!");
            return 1;
        }

        URI uri;
        try {
            uri = new URIBuilder(repoMeta.getUrl()).appendPath("history").build();
        } catch (URISyntaxException e) {
            System.err.println("Failed to build URI!");
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
            System.err.println("Failed to execute HTTP request!");
            return 1;
        }
        List<CommitHistoryView> history = mapper.readValue(response, new TypeReference<List<CommitHistoryView>>() {
        });
        printCommitHistory(history);
        return 0;
    }

    private void printCommitHistory(List<CommitHistoryView> commits) {
        if (commits == null || commits.isEmpty()) {
            System.out.println("No commits to display.");
            return;
        }

        int revWidth = 10;
        int userWidth = 15;
        int msgWidth = 40;
        int dateWidth = 20;


        String format = "| %-" + revWidth + "s | %-" + userWidth + "s | %-" + msgWidth + "s | %-" + dateWidth + "s |%n";

        String separator = "+-" + "-".repeat(revWidth) + "-+-"
                + "-".repeat(userWidth) + "-+-"
                + "-".repeat(msgWidth) + "-+-"
                + "-".repeat(dateWidth) + "-+";

        System.out.println(separator);
        System.out.printf(format, "Revision", "Username", "Message", "Date");
        System.out.println(separator);

        for (CommitHistoryView commit : commits) {
            String revStr = commit.getRevisionNumber() != null ? String.valueOf(commit.getRevisionNumber()) : "N/A";
            String username = commit.getUsername() != null ? commit.getUsername() : "Unknown";
            String dateStr = commit.getCreatedAt() != null ? DATE_FORMATTER.format(commit.getCreatedAt()) : "N/A";

            String message = commit.getMessage() != null ? commit.getMessage() : "";
            if (message.length() > msgWidth) {
                message = message.substring(0, msgWidth - 3) + "...";
            }

            System.out.printf(format, revStr, username, message, dateStr);
        }

        System.out.println(separator);
    }
}
