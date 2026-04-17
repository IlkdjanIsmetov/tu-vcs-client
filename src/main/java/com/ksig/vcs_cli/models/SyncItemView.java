package com.ksig.vcs_cli.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ksig.vcs_cli.models.enums.ItemType;
import com.ksig.vcs_cli.models.enums.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncItemView {
    private UUID itemId;
    private String path;
    @JsonIgnore
    private Path localPath;
    private SyncStatus status;
    private String serverChecksum;
    private String storageKey;
    private Long serverRevisionNumber;
    private ItemType itemType;
}