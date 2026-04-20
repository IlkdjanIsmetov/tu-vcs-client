package com.ksig.vcs_cli.models;

import lombok.Data;

import java.time.Instant;

@Data
public class CommitHistoryView {
    private Long revisionNumber;
    private String username;
    private String message;
    private Instant createdAt;
}