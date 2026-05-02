package com.gtnewhorizons.gametest;

/** JVM flags controlling GameTest bootstrap (see mixin + world setup code). */
public final class GameTestJvmFlags {

    public static final String PROPERTY = "gtnh.gametest";

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty(PROPERTY, "false"));

    private GameTestJvmFlags() {}

    public static boolean isEnabled() {
        return ENABLED;
    }
}
