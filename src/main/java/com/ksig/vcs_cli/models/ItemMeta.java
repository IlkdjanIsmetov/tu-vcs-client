package com.ksig.vcs_cli.models;

import com.ksig.vcs_cli.models.enums.ItemType;
import lombok.Data;

import java.util.UUID;

@Data
public class ItemMeta {
    private UUID id;
    private String path;
    private ItemType type;
    private UUID revisionId;
    private Long revisionNumber;
    private String checksum;
    private String storageKey;
}
