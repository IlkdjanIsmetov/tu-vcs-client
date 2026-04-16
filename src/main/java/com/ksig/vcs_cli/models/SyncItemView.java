package com.ksig.vcs_cli.models;

import com.ksig.vcs_cli.models.enums.ItemType;
import com.ksig.vcs_cli.models.enums.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncItemView {
    private UUID itemId;
    private String path;
    private SyncStatus status;
    private String serverChecksum;
    private String storageKey;
    private Long serverRevisionNumber;
    private ItemType itemType;
}