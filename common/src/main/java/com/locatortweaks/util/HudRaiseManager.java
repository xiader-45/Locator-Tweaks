package com.locatortweaks.util;

public class HudRaiseManager {
    private static float currentOffset = 0.0f;
    private static float targetOffset = 0.0f;
    private static float textProgress = 0.0f;
    private static float targetProgress = 0.0f;
    private static long lastUpdateTime = System.currentTimeMillis();
    private static long lastActiveTime = 0;

    public static void setTarget(float targetOff, float targetProg) {
        targetOffset = targetOff;
        targetProgress = targetProg;
        lastActiveTime = System.currentTimeMillis();
    }

    public static void setTargetOffset(float target) {
        setTarget(target, target > 0.0f ? 1.0f : 0.0f);
    }

    public static void update() {
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastUpdateTime;
        lastUpdateTime = now;
        if (elapsedMs > 100) {
            elapsedMs = 100;
        }

        if (now - lastActiveTime > 400) {
            targetOffset = 0.0f;
            targetProgress = 0.0f;
        }

        float factor = (float) (1.0 - Math.exp(-elapsedMs / 100.0));
        currentOffset += (targetOffset - currentOffset) * factor;
        if (Math.abs(targetOffset - currentOffset) < 0.1f) {
            currentOffset = targetOffset;
        }

        textProgress += (targetProgress - textProgress) * factor;
        if (Math.abs(targetProgress - textProgress) < 0.005f) {
            textProgress = targetProgress;
        }
    }

    public static int getRaiseOffset() {
        update();
        return Math.round(currentOffset);
    }

    public static float getTextProgress() {
        update();
        return textProgress;
    }

    public static boolean isTextVisible() {
        update();
        return textProgress > 0.005f;
    }
}
