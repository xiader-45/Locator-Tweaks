package com.locatortweaks.util;

public class LocatorFadeManager {
    private static volatile boolean resetRequested = false;

    public static void requestReset() {
        resetRequested = true;
    }

    public static boolean isResetRequested() {
        return resetRequested;
    }

    public static void clearResetRequest() {
        resetRequested = false;
    }
}
