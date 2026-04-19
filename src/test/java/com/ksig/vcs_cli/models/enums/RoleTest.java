package com.ksig.vcs_cli.models.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoleTest {
    @Test
    void syncStatus_shouldContainAllExpectedValues() {
        assertArrayEquals(
                new SyncStatus[]{
                        SyncStatus.UP_TO_DATE,
                        SyncStatus.NEW_REMOTE,
                        SyncStatus.MODIFIED_REMOTE,
                        SyncStatus.DELETED_REMOTE,
                        SyncStatus.CONFLICT
                },
                SyncStatus.values()
        );
    }
}