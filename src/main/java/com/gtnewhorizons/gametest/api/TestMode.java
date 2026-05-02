package com.gtnewhorizons.gametest.api;

/** How the game test runner behaves at completion and regarding visuals. */
public enum TestMode {

    /** CI / headless: no client visuals, exit process with non-zero on failures. */
    HEADLESS_CI,

    /** Developer / in-game: keep server up, commands, visual debugging. */
    INTERACTIVE
}
