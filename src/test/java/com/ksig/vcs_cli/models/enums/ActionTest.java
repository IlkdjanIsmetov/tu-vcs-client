package com.ksig.vcs_cli.models.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ActionTest {
    @Test
    void action_shouldContainAllExpectedValues() {
        assertArrayEquals(
                new Action[]{
                        Action.ADD,
                        Action.MODIFY,
                        Action.DELETE
                },
                Action.values()
        );
    }
}