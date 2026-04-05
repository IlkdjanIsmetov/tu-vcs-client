package com.ksig.vcs_cli.exceptions;

public class NotLoggedInException extends RuntimeException {
    public NotLoggedInException() {
        super("Not logged in. Please run 'tu-vcs login' first.");
    }
}
