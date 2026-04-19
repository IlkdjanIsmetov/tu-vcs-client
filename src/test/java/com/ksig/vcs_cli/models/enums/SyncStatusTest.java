package com.ksig.vcs_cli.models.enums;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SyncStatusTest {
    @Test
    void syncStatus_shouldContainAllExpectedValues() {
        SyncStatus[] values = SyncStatus.values();

        assertEquals(5, values.length);
        assertEquals(SyncStatus.UP_TO_DATE, values[0]);
        assertEquals(SyncStatus.NEW_REMOTE, values[1]);
        assertEquals(SyncStatus.MODIFIED_REMOTE, values[2]);
        assertEquals(SyncStatus.DELETED_REMOTE, values[3]);
        assertEquals(SyncStatus.CONFLICT, values[4]);
    }
}