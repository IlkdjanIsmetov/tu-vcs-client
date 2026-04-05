package com.ksig.vcs_cli.exceptions;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException() {
        super("Session permanently expired. Please run 'tu-vcs login' again.");
    }
}
