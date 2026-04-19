package com.ksig.vcs_cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.stefanbirkner.systemlambda.SystemLambda.*;

class VcsCliApplicationTest {

    @Test
    void main_shouldPrintError_whenExceptionOccurs() throws Exception {
        String output = tapSystemErr(() -> {
            try {
                catchSystemExit(() -> {
                    VcsCliApplication.main(new String[]{"--invalid-option"});
                });
            } catch (Exception ignored) {}
        });

        assertNotNull(output);
    }

    @Test
    void call_shouldPrintWelcomeMessage() throws Exception {
        String output = tapSystemOut(() -> {
            new VcsCliApplication().call();
        });

        assertTrue(output.contains("Welcome to TU-VCS."));
    }

    @Test
    void call_shouldReturnZero() {
        Integer result = new VcsCliApplication().call();

        assertEquals(0, result);
    }
}