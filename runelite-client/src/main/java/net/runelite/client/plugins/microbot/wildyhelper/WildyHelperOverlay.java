package net.runelite.client.plugins.microbot.wildyhelper;

import net.runelite.client.ui.overlay.*;
import net.runelite.api.HeadIcon;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class WildyHelperOverlay extends OverlayPanel
{
    private boolean enabled = true;
    private String targetName = "-";
    private Instant frozenUntil = null;
    private HeadIcon overhead = null;
    private boolean hasFood = false;

    @Inject
    private WildyHelperOverlay() {
        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.HIGH);
        panelComponent.setPreferredSize(new Dimension(220, 0));
    }

    void setEnabled(boolean b) { enabled = b; }
    void setTargetName(String n) { targetName = (n == null ? "-" : n); }
    void setFrozenUntil(Instant t) { frozenUntil = t; }
    void setOverhead(HeadIcon h) { overhead = h; }
    void setHasFood(boolean f) { hasFood = f; }

    @Override
    public Dimension render(Graphics2D g) {
        if (!enabled || !WildyHelperScript.ACTIVE) return null;
        panelComponent.getChildren().clear();

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Current Target:")
                .right(targetName)
                .build());

        String frozenStr = "No";
        if (frozenUntil != null) {
            long secs = Math.max(0, Instant.now().until(frozenUntil, ChronoUnit.SECONDS));
            frozenStr = secs > 0 ? ("Yes (" + secs + "s)") : "No";
        }
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Target Frozen:")
                .right(frozenStr)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Target Overhead:")
                .right(overhead == null ? "None" : overhead.name())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Target Has Food:")
                .right(hasFood ? "Likely" : "Unknown")
                .build());

        return super.render(g);
    }
}
