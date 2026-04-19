package com.ksig.vcs_cli.utils;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ConsoleColorsTest {

    @Test
    void consoleColors_shouldHaveCorrectValues() {
        assertEquals("\u001B[0m", ConsoleColors.RESET);
        assertEquals("\u001B[31m", ConsoleColors.RED);
        assertEquals("\u001B[32m", ConsoleColors.GREEN);
        assertEquals("\u001B[33m", ConsoleColors.YELLOW);
    }

}