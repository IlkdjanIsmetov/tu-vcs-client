package com.ksig.vcs_cli.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocalItemMetadata {
    private String path;
    private String checksum;
    private Long lastPulledRevisionNumber;

    public static LocalItemMetadata fromItemMeta(ItemMeta itemMeta) {
        LocalItemMetadata localItemMetadata = new LocalItemMetadata();
        localItemMetadata.path = itemMeta.getPath();
        localItemMetadata.checksum = itemMeta.getChecksum();
        localItemMetadata.lastPulledRevisionNumber = itemMeta.getRevisionNumber();
        return localItemMetadata;
    }
}