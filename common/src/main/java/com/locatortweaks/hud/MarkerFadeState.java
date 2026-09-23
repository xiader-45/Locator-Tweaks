package com.locatortweaks.hud;

public class MarkerFadeState {
    public float alpha;
    public PendingMarker lastMarker;
    public boolean fadingOut;
    public float introTooltipTimer;

    public MarkerFadeState(float alpha, boolean fadingOut) {
        this(alpha, fadingOut, 0.0F);
    }

    public MarkerFadeState(float alpha, boolean fadingOut, float introTooltipTimer) {
        this.alpha = alpha;
        this.fadingOut = fadingOut;
        this.introTooltipTimer = introTooltipTimer;
    }
}
