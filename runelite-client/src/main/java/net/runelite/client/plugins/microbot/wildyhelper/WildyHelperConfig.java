package net.runelite.client.plugins.microbot.wildyhelper;

import net.runelite.client.config.*;

@ConfigGroup("wildyhelper")
public interface WildyHelperConfig extends Config
{
    @ConfigItem(
            keyName = "enableLogoutOnPker",
            name = "Logout on PKer",
            description = "Logs out when a dangerous player is detected"
    )
    default boolean enableLogoutOnPker() { return true; }

    @ConfigItem(
            keyName = "dangerItemsCsv",
            name = "Danger items (CSV)",
            description = "Any of these seen on an attackable player = PKer"
    )
    default String dangerItemsCsv() {
        return "Ancient staff, Heavy ballista, Webweaver bow, Dragon crossbow, Toxic staff of the dead, Zaryte crossbow";
    }

    @ConfigItem(
            keyName = "safePlayersCsv",
            name = "Safe player names (CSV)",
            description = "Ignore these names for PKer detection"
    )
    default String safePlayersCsv() { return ""; }

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Show target status overlay"
    )
    default boolean showOverlay() { return true; }

    @Range(max = 60)
    @ConfigItem(
            keyName = "freezeSeconds",
            name = "Freeze seconds",
            description = "Overlay countdown length when a freeze is detected"
    )
    default int freezeSeconds() { return 15; }
}
