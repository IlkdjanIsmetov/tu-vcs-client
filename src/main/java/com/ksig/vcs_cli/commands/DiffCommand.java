package com.ksig.vcs_cli.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.models.ItemMeta;
import com.ksig.vcs_cli.models.RepositoryMeta;
import com.ksig.vcs_cli.utils.ConsoleColors;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.net.URIBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static com.ksig.vcs_cli.commands.CreateRepositoryCommand.backendRestClient;
import static com.ksig.vcs_cli.utils.RepositoryStatus.getRepositoryMeta;

@Command(name = "diff", description = "Show changes in a specific file")
public class DiffCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-r", "--revision"}, description = "Compare against a specific revision")
    private Long revision;

    @Parameters(index = "0")
    private String filePath;

    @Override
    public Integer call() throws Exception {
        try {
            Path repoRoot = RepositoryStatus.findRepositoryRoot();

            RepositoryMeta repoMata = RepositoryStatus.getRepositoryMeta();
            List<String> currentLines = Files.readAllLines(repoRoot.resolve(filePath));

            List<String> baseLines = getFileContentByRevision(filePath, revision, repoMata.getUrl());

            generateDiff(baseLines, currentLines);
            return 0;
        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private void generateDiff(List<String> original, List<String> revised) {
        Patch<String> patch = DiffUtils.diff(original, revised);

        for (AbstractDelta<String> delta : patch.getDeltas()) {
            int originPos = delta.getSource().getPosition() + 1;
            int revisedPos = delta.getTarget().getPosition() + 1;

            System.out.println(ConsoleColors.YELLOW + "@@ -" + originPos + " +" + revisedPos + ConsoleColors.RESET);

            for (String line : delta.getSource().getLines()) {
                System.out.println(ConsoleColors.RED + "-" + line + ConsoleColors.RESET);
            }
            for (String line : delta.getTarget().getLines()) {
                System.out.println(ConsoleColors.GREEN + "+" + line + ConsoleColors.RESET);
            }
        }
    }

    private List<String> getFileContentByRevision(String path, Long revision, String baseUrl) throws Exception {
        URIBuilder urlBuilder = new URIBuilder(baseUrl).appendPath("content").addParameter("path", path);

        if (revision != null) {
            urlBuilder.addParameter("rev", String.valueOf(revision));
        }

        HttpGet request = new HttpGet(urlBuilder.toString());
        String content = backendRestClient.executeStringRequest(request);
        return Arrays.asList(content.split("\\R"));
    }
}

