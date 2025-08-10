package net.runelite.client.plugins.microbot.wildyhelper;

import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.Microbot;

public class WildyHelperScript extends Script {
    public static volatile boolean ACTIVE = false;

    @Override
    public boolean run() {
        ACTIVE = true;
        Microbot.status = "WildyHelper running";
        // No loop needed; RuneLite events in the plugin do the work.
        return true;
    }

    @Override
    public void shutdown() {
        ACTIVE = false;
        Microbot.status = "WildyHelper stopped";
    }
}
