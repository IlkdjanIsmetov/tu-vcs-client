package com.ksig.vcs_cli.exceptions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotARepoExceptionTest {
    @Test
    void constructor_shouldSetCorrectMessage() {
        NotARepoException ex = new NotARepoException();

        assertEquals("This is not a tu-vcs repository!", ex.getMessage());
    }
}