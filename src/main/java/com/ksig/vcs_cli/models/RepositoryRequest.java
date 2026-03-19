package com.ksig.vcs_cli.models;

import lombok.Data;

@Data
public class RepositoryRequest {
    private String repositoryName;
    private boolean requireApproval;
    private String description;
}
