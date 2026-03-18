package com.ksig.vcs_cli.models;

import lombok.Data;

@Data
public class RepositoryRequest {
    private String repoName;
    private boolean requireApproval;
    private String description;
}
