package com.ksig.vcs_cli.models.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemTypeTest {
    @Test
    void itemType_shouldContainAllExpectedValues() {
        assertArrayEquals(
                new ItemType[]{
                        ItemType.FILE,
                        ItemType.DIRECTORY
                },
                ItemType.values()
        );
    }
}