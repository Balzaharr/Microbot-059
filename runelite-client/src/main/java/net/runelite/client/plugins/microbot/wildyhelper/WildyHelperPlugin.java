package net.runelite.client.plugins.microbot.wildyhelper;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Wildy Helper",
        description = "QoL helper: quick target info + PKer safety",
        tags = {"pk","wildy","qol"}
)
public class WildyHelperPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private WildyHelperOverlay overlay;
    @Inject private WildyHelperConfig config;

    // Microbot-style script wrapper
    private final WildyHelperScript script = new WildyHelperScript();

    private final TargetState target = new TargetState();
    private Set<String> dangerItems;
    private Set<String> safePlayers;

    private static final Set<Integer> FREEZE_GFX = new HashSet<>(Arrays.asList(
            181, 128, 197, 166, 363, 367 // snare/entangle and ice series
    ));

    @Provides WildyHelperConfig provideConfig(ConfigManager cm) { return cm.getConfig(WildyHelperConfig.class); }

    // Microbot entry points (same pattern as your other scripts)
    public void run()      { script.run(); }
    public void shutdown() { script.shutdown(); }

    @Override protected void startUp() {
        overlayManager.add(overlay);
        reloadLists();
        target.reset();
        log.info("Wildy Helper started");
    }

    @Override protected void shutDown() {
        overlayManager.remove(overlay);
        target.reset();
        WildyHelperScript.ACTIVE = false;
        log.info("Wildy Helper stopped");
    }

    private void reloadLists() {
        dangerItems = Arrays.stream(config.dangerItemsCsv().split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).collect(Collectors.toSet());

        safePlayers = Arrays.stream(config.safePlayersCsv().split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(String::toLowerCase).collect(Collectors.toSet());
    }

    // ===== Events =====
    @Subscribe
    public void onConfigChanged(ConfigChanged e) {
        if ("wildyhelper".equals(e.getGroup())) reloadLists();
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged e) {
        if (!WildyHelperScript.ACTIVE) return;
        final Actor src = e.getSource();
        if (src != null && src.equals(client.getLocalPlayer())) {
            final Actor tgt = e.getTarget();
            target.set((tgt instanceof Player) ? (Player) tgt : null);
        }
    }

    @Subscribe
    public void onGameTick(GameTick t) {
        if (!WildyHelperScript.ACTIVE) return;

        if (!target.isValid(client))
            target.findCandidate(client);

        overlay.setEnabled(config.showOverlay());
        overlay.setTargetName(target.getName());
        overlay.setFrozenUntil(target.frozenUntil);
        overlay.setOverhead(target.getOverheadIcon());
        overlay.setHasFood(target.hasFood);

        if (config.enableLogoutOnPker() && isInWilderness() && isDangerNearby())
            requestLogout();
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated e) {
        if (!WildyHelperScript.ACTIVE || target.player == null) return;
        final GraphicsObject go = e.getGraphicsObject();
        if (FREEZE_GFX.contains(go.getId())) {
            final WorldPoint gp = WorldPoint.fromLocal(client, go.getLocation());
            final WorldPoint tp = target.player.getWorldLocation();
            if (gp != null && tp != null && gp.distanceTo(tp) <= 1)
                target.markFrozen(config.freezeSeconds());
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged e) {
        if (!WildyHelperScript.ACTIVE) return;
        if (!(e.getActor() instanceof Player)) return;
        final Player p = (Player) e.getActor();
        if (p.equals(target.player) && p.getAnimation() == 829) // eat/drink anim
            target.hasFood = true;
    }

    // ===== Helpers =====
    private boolean isInWilderness() {
        final Player me = client.getLocalPlayer();
        if (me == null) return false;
        return client.getVarpValue(VarPlayer.WILDERNESS_LEVEL) > 0;
    }

    private boolean isDangerNearby() {
        final Player me = client.getLocalPlayer();
        if (me == null) return false;

        final int wildy = client.getVarpValue(VarPlayer.WILDERNESS_LEVEL);
        final int myCb = me.getCombatLevel();

        for (Player p : client.getPlayers()) {
            if (p == null || p == me) continue;
            if (safePlayers.contains(p.getName().toLowerCase())) continue;
            if (Math.abs(p.getCombatLevel() - myCb) > wildy) continue;

            // Simple danger heuristic: skulled or clearly kitted for PvP.
            if (p.getSkullIcon() != null) return true;
            if (looksDangerousByName(p)) return true;
        }
        return false;
    }

    private boolean looksDangerousByName(Player p) {
        final String n = p.getName().toLowerCase();
        if (n.contains("pure") || n.contains("pk")) return true;
        // TODO: Upgrade using ItemManager to resolve equipment names vs dangerItems.
        return false;
    }

    private void requestLogout() {
        try {
            net.runelite.client.plugins.microbot.util.player.Rs2Player.logout();
        } catch (Throwable t) {
            Widget w = client.getWidget(WidgetInfo.LOGOUT_BUTTON);
            if (w != null) client.invokeMenuAction("", "", 1, MenuAction.CC_OP.getId(), w.getId(), -1);
        }
    }

    // ===== Target state =====
    @Getter
    static class TargetState {
        private Player player;
        private String name = "-";
        private HeadIcon overheadIcon;
        boolean hasFood = false;
        Instant frozenUntil = null;

        void reset() {
            player = null; name = "-"; overheadIcon = null;
            hasFood = false; frozenUntil = null;
        }

        void set(Player p) {
            player = p;
            name = p != null ? p.getName() : "-";
            overheadIcon = p != null ? p.getOverheadIcon() : null;
            hasFood = false;
        }

        boolean isValid(Client c) {
            if (player == null) return false;
            if (player.getWorldLocation() == null) return false;
            overheadIcon = player.getOverheadIcon();
            return true;
        }

        void findCandidate(Client c) {
            Player me = c.getLocalPlayer();
            if (me == null) { set(null); return; }

            Actor inter = me.getInteracting();
            if (inter instanceof Player) { set((Player) inter); return; }

            int wildy = c.getVarpValue(VarPlayer.WILDERNESS_LEVEL);
            int myCb = me.getCombatLevel();
            Player best = null; int bestDist = Integer.MAX_VALUE;

            for (Player p : c.getPlayers()) {
                if (p == null || p == me) continue;
                if (Math.abs(p.getCombatLevel() - myCb) > wildy) continue;
                WorldPoint mp = me.getWorldLocation(), tp = p.getWorldLocation();
                if (mp == null || tp == null) continue;
                int d = mp.distanceTo(tp);
                if (d < bestDist) { best = p; bestDist = d; }
            }
            set(best);
        }

        void markFrozen(int seconds) {
            frozenUntil = Instant.now().plusSeconds(seconds);
        }
    }
}
