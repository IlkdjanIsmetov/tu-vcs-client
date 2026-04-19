package com.ksig.vcs_cli.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotLoggedInExceptionTest {
    @Test
    void constructor_shouldSetCorrectMessage() {
        NotLoggedInException ex = new NotLoggedInException();

        assertEquals("Not logged in. Please run 'tu-vcs login' first.", ex.getMessage());
    }
}