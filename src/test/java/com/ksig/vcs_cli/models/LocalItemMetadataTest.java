package com.ksig.vcs_cli.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LocalItemMetadataTest {
    @Test
    void fromItemMeta_shouldMapAllFieldsCorrectly() {
        ItemMeta itemMeta = new ItemMeta();
        itemMeta.setPath("file.txt");
        itemMeta.setChecksum("abc123");
        itemMeta.setRevisionNumber(10L);

        LocalItemMetadata result = LocalItemMetadata.fromItemMeta(itemMeta);

        assertEquals("file.txt", result.getPath());
        assertEquals("abc123", result.getChecksum());
        assertEquals(10, result.getLastPulledRevisionNumber());
    }

    @Test
    void fromItemMeta_shouldThrowException_whenItemMetaIsNull() {
        assertThrows(NullPointerException.class, () -> {
            LocalItemMetadata.fromItemMeta(null);
        });
    }
}