package com.ksig.vcs_cli.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionExpiredExceptionTest {
    @Test
    void constructor_shouldSetCorrectMessage() {
        SessionExpiredException ex = new SessionExpiredException();

        assertEquals("Session permanently expired. Please run 'tu-vcs login' again.", ex.getMessage());
    }
}