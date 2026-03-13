package com.ksig.vcs_cli;

import com.ksig.vcs_cli.commands.LoginCommand;
import com.ksig.vcs_cli.commands.TestCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(name = "tu-vcs",
         mixinStandardHelpOptions = true, 
         version = "1.0",
         description = "Centralized Version Control System CLI",
         subcommands = { LoginCommand.class, TestCommand.class })
public class VcsCliApplication implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Welcome to TU-VCS.");
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new VcsCliApplication()).execute(args);
        System.exit(exitCode);
    }
}