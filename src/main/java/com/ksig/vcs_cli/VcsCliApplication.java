package com.ksig.vcs_cli;

import com.ksig.vcs_cli.commands.*;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "tu-vcs",
         mixinStandardHelpOptions = true, 
         version = "1.0",
         description = "Centralized Version Control System CLI",
         subcommands = { LoginCommand.class, CreateRepositoryCommand.class, FetchCommand.class, StatusCommand.class, CommitCommand.class, CloneCommand.class, })
public class VcsCliApplication implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Welcome to TU-VCS.");
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = 0;
        try {
            exitCode = new CommandLine(new VcsCliApplication()).execute(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        System.exit(exitCode);
    }
}