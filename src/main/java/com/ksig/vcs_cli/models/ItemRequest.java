package com.ksig.vcs_cli.models;


import com.ksig.vcs_cli.models.enums.Action;
import com.ksig.vcs_cli.models.enums.ItemType;
import lombok.Data;

import java.util.UUID;

@Data
public class ItemRequest {
    private UUID itemId;
    private String path;
    private ItemType itemType;
    private Action action;
    private int fileSize;
    private String checksum;
    private String fileRef;
}
