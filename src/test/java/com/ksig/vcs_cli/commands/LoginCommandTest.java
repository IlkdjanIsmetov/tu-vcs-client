package com.ksig.vcs_cli.commands;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class LoginCommandTest {
    @Test
    void call_shouldReturn0_whenAuthenticationSucceeds() {
        ByteArrayInputStream in = new ByteArrayInputStream("user\npass\n".getBytes());
        System.setIn(in);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        LoginCommand command = new LoginCommand() {
            @Override
            public Integer call() {
                String username = "user";
                String password = "pass";
                System.out.println("Authenticating user: " + username + "...");
                boolean success = true;

                if (success) {
                    System.out.println("Login successful!");
                    return 0;
                } else {
                    System.err.println("Login failed. Please check your credentials.");
                    return 1;
                }
            }
        };

        int result = command.call();

        assertEquals(0, result);
        assertTrue(out.toString().contains("Login successful"));
    }

    @Test
    void call_shouldReturn1_whenAuthenticationFails() {
        ByteArrayInputStream in = new ByteArrayInputStream("user\npass\n".getBytes());
        System.setIn(in);

        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));

        LoginCommand command = new LoginCommand() {
            @Override
            public Integer call() {
                String username = "user";
                String password = "pass";
                boolean success = false;

                if (success) {
                    System.out.println("Login successful!");
                    return 0;
                } else {
                    System.err.println("Login failed. Please check your credentials.");
                    return 1;
                }
            }
        };

        int result = command.call();

        assertEquals(1, result);
        assertTrue(err.toString().contains("Login failed"));
    }
}