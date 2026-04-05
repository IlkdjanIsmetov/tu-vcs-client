package com.ksig.vcs_cli.exceptions;

public class NotARepoException extends Exception {
    public NotARepoException() {
        super("This is not a tu-vcs repository!");
    }
}
