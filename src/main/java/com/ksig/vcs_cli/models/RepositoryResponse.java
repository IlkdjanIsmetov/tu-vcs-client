package com.ksig.vcs_cli.models;

import lombok.Data;

import java.util.UUID;

@Data
public class RepositoryResponse {
    private UUID id;
    private String name;
    private String description;
    private boolean requireApproval;
    private Long revision;
    private String url;
}
