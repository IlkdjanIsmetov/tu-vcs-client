package com.ksig.vcs_cli.commands;


import com.ksig.vcs_cli.auth.KeycloakClient;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

@Command(name = "login", description = "Authenticate with the centralized TU-VCS server")
public class LoginCommand implements Callable<Integer> {

    @Option(names = {"-u", "--username"}, required = true, description = "Your Keycloak username")
    private String username;

    @Option(names = {"-p", "--password"}, required = true, description = "Your password")
    private String password;

    @Override
    public Integer call() {
        System.out.println("Authenticating user: " + username + "...");
        
        KeycloakClient authClient = new KeycloakClient();
        boolean success = authClient.authenticate(username, password);
        
        if (success) {
            System.out.println("Login successful!");
            return 0;
        } else {
            System.err.println("Login failed. Please check your credentials.");
            return 1;
        }
    }
}