package com.ksig.vcs_cli.commands;

import com.ksig.vcs_cli.globalParams.GlobarParams;
import com.ksig.vcs_cli.utils.RepositoryStatus;
import picocli.CommandLine.Command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "status", description = "Current status of items in the repo")
public class StatusCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        try {
            RepositoryStatus.StatusResult result = RepositoryStatus.analyzeWorkspace();

            printStatus(
                    new ArrayList<>(result.added.keySet()),
                    new ArrayList<>(result.modified.keySet()),
                    new ArrayList<>(result.deleted.keySet())
            );

            return 0;

        } catch (IOException e) {
            System.err.println("Error reading repository state: " + e.getMessage());
            return 1;
        }
    }

    private void printStatus(List<String> added, List<String> modified, List<String> deleted) {
        if (added.isEmpty() && modified.isEmpty() && deleted.isEmpty()) {
            System.out.println("Nothing to commit, working tree clean.");
            return;
        }

        if (!added.isEmpty()) {
            for (String item : added) {
                System.out.println("ADDED: " + item);
            }
            System.out.println();
        }

        if (!modified.isEmpty()) {
            for (String item : modified) {
                System.out.println("MODIFIED: " + item);
            }
            System.out.println();
        }

        if (!deleted.isEmpty()) {
            for (String item : deleted) {
                System.out.println("DELETED: " + item);
            }
            System.out.println();
        }
    }
}