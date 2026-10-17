package im.nesp.creatorsmp.client.anticheat;

import im.nesp.creatorsmp.CreatorSMPConfig;
import im.nesp.creatorsmp.CreatorSMPCore;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CheatDetector {

    public record DetectedCheat(String displayName, String category, String identifier) {
        @Override
        public String toString() {
            return "[" + category + "] " + displayName + " (" + identifier + ")";
        }
    }

    private record KnownCheat(String identifier, String displayName, String category) {}

    // Popular cheat clients & unauthorized modifications by Mod ID
    private static final List<KnownCheat> KNOWN_MOD_IDS = List.of(
            // X-Ray
            new KnownCheat("xray", "X-Ray Mod", "X-Ray"),
            new KnownCheat("advancedxray", "Advanced XRay", "X-Ray"),
            new KnownCheat("advanced_xray", "Advanced XRay", "X-Ray"),
            new KnownCheat("simple_xray", "Simple XRay", "X-Ray"),
            new KnownCheat("xrayreloaded", "XRay Reloaded", "X-Ray"),
            new KnownCheat("xray_reloaded", "XRay Reloaded", "X-Ray"),
            new KnownCheat("pro_xray", "Pro X-Ray", "X-Ray"),
            new KnownCheat("x_ray", "X-Ray", "X-Ray"),
            new KnownCheat("ore_detector", "Ore Detector", "X-Ray"),

            // Freecam
            new KnownCheat("freecam", "Freecam Utility", "Freecam"),
            new KnownCheat("freecammod", "Freecam Mod", "Freecam"),
            new KnownCheat("modfreecam", "Freecam Mod", "Freecam"),
            new KnownCheat("simple_freecam", "Simple Freecam", "Freecam"),
            new KnownCheat("tweaked_freecam", "Tweaked Freecam", "Freecam"),
            new KnownCheat("free_cam", "Freecam", "Freecam"),

            // Pathing & Automation
            new KnownCheat("baritone", "Baritone Auto-Pathing", "Bot/Automation"),
            new KnownCheat("baritone-api", "Baritone API", "Bot/Automation"),
            new KnownCheat("seedcracker", "SeedCracker", "World Exploit"),
            new KnownCheat("seedcrackerx", "SeedCrackerX", "World Exploit"),

            // Popular Cheat / Hack Clients
            new KnownCheat("meteor", "Meteor Client", "Cheat Client"),
            new KnownCheat("meteor-client", "Meteor Client", "Cheat Client"),
            new KnownCheat("meteordevelopment", "Meteor Client", "Cheat Client"),
            new KnownCheat("wurst", "Wurst Client", "Cheat Client"),
            new KnownCheat("wurst-client", "Wurst Client", "Cheat Client"),
            new KnownCheat("wurstclient", "Wurst Client", "Cheat Client"),
            new KnownCheat("aristois", "Aristois Client", "Cheat Client"),
            new KnownCheat("liquidbounce", "LiquidBounce Client", "Cheat Client"),
            new KnownCheat("liquidbounce-nextgen", "LiquidBounce Nextgen", "Cheat Client"),
            new KnownCheat("bleachhack", "BleachHack Client", "Cheat Client"),
            new KnownCheat("bleach", "Bleach Client", "Cheat Client"),
            new KnownCheat("rusherhack", "RusherHack Client", "Cheat Client"),
            new KnownCheat("future", "Future Client", "Cheat Client"),
            new KnownCheat("inertia", "Inertia Client", "Cheat Client"),
            new KnownCheat("impact", "Impact Client", "Cheat Client"),
            new KnownCheat("ares", "Ares Client", "Cheat Client"),
            new KnownCheat("lambda", "Lambda Client", "Cheat Client"),
            new KnownCheat("kamiblue", "KamiBlue Client", "Cheat Client"),
            new KnownCheat("kami", "Kami Client", "Cheat Client"),
            new KnownCheat("sigma", "Sigma Client", "Cheat Client"),
            new KnownCheat("mathax", "Mathax Client", "Cheat Client"),
            new KnownCheat("coffee", "Coffee Client", "Cheat Client"),
            new KnownCheat("atomic", "Atomic Client", "Cheat Client"),
            new KnownCheat("doomsday", "Doomsday Client", "Cheat Client"),
            new KnownCheat("cornos", "Cornos Client", "Cheat Client")
    );

    // Classpath / Injected reflection check signatures
    private static final List<KnownCheat> KNOWN_CLASSES = List.of(
            new KnownCheat("meteordevelopment.meteorclient.MeteorClient", "Meteor Client", "Cheat Client"),
            new KnownCheat("meteordevelopment.orbit.EventBus", "Meteor Framework (Orbit)", "Cheat Client"),
            new KnownCheat("net.wurstclient.WurstClient", "Wurst Client", "Cheat Client"),
            new KnownCheat("net.wurstclient.core.Wurst", "Wurst Client Core", "Cheat Client"),
            new KnownCheat("me.deftware.client.framework.main.bootstrap.Bootstrap", "Aristois Client", "Cheat Client"),
            new KnownCheat("me.deftware.client.framework.main.EMCMod", "Aristois EMC Framework", "Cheat Client"),
            new KnownCheat("net.ccbluex.liquidbounce.LiquidBounce", "LiquidBounce Client", "Cheat Client"),
            new KnownCheat("baritone.api.BaritoneAPI", "Baritone Auto-Pathing", "Bot/Automation"),
            new KnownCheat("baritone.client.Baritone", "Baritone Client", "Bot/Automation"),
            new KnownCheat("net.freecam.Freecam", "Freecam Mod", "Freecam"),
            new KnownCheat("me.shedaniel.freecam.Freecam", "Freecam Mod", "Freecam"),
            new KnownCheat("com.github.freecam.Freecam", "Freecam Mod", "Freecam"),
            new KnownCheat("net.fabricmc.freecam.Freecam", "Freecam Mod", "Freecam"),
            new KnownCheat("bleach.hack.BleachHack", "BleachHack Client", "Cheat Client"),
            new KnownCheat("org.rusherhack.client.RusherHack", "RusherHack Client", "Cheat Client"),
            new KnownCheat("net.futureclient.client.FutureClient", "Future Client", "Cheat Client"),
            new KnownCheat("me.rebotted.Inertia", "Inertia Client", "Cheat Client"),
            new KnownCheat("info.sigmaclient.Client", "Sigma Client", "Cheat Client"),
            new KnownCheat("ares.client.Ares", "Ares Client", "Cheat Client"),
            new KnownCheat("mathe172.mathax.client.MathaxClient", "Mathax Client", "Cheat Client"),
            new KnownCheat("me.atomic.client.Atomic", "Atomic Client", "Cheat Client"),
            new KnownCheat("coffee.client.Coffee", "Coffee Client", "Cheat Client"),
            new KnownCheat("org.coffeeclient.Coffee", "Coffee Client", "Cheat Client"),
            new KnownCheat("com.advancedxray.AdvancedXray", "Advanced X-Ray", "X-Ray"),
            new KnownCheat("pro.mister11.xray.Xray", "X-Ray Mod", "X-Ray"),
            new KnownCheat("org.anti.xray.Xray", "X-Ray Mod", "X-Ray")
    );

    /**
     * Scans currently loaded mods and runtime classes for unauthorized modifications.
     *
     * @return List of detected cheats, or an empty list if client is clean.
     */
    public static List<DetectedCheat> scan() {
        List<DetectedCheat> detected = new ArrayList<>();
        Set<String> detectedIds = new HashSet<>();
        List<? extends String> whitelist = CreatorSMPConfig.WHITELISTED_MODS.get();
        Set<String> whitelistSet = new HashSet<>();
        if (whitelist != null) {
            for (String w : whitelist) {
                whitelistSet.add(w.toLowerCase(Locale.ROOT).trim());
            }
        }

        // 1. Scan known Mod IDs via ModList
        for (KnownCheat cheat : KNOWN_MOD_IDS) {
            String id = cheat.identifier().toLowerCase(Locale.ROOT);
            if (whitelistSet.contains(id)) continue;

            if (ModList.get().isLoaded(id)) {
                if (detectedIds.add(id)) {
                    detected.add(new DetectedCheat(cheat.displayName(), cheat.category(), "modid: " + cheat.identifier()));
                }
            }
        }

        // 2. Scan all loaded mod metadata (name, description, id)
        try {
            for (IModInfo mod : ModList.get().getMods()) {
                String modId = mod.getModId().toLowerCase(Locale.ROOT);
                if (whitelistSet.contains(modId)) continue;
                if (modId.equals("creatorsmp_core") || modId.equals("minecraft") || modId.equals("neoforge")) continue;

                // Match mod IDs only; display names are user-controlled and too noisy.
                if ((modId.contains("xray") || modId.contains("x_ray"))
                        && !modId.contains("anti")) {
                    if (detectedIds.add(modId)) {
                        detected.add(new DetectedCheat(mod.getDisplayName(), "X-Ray", "modid: " + mod.getModId()));
                    }
                }

                if ((modId.contains("freecam") || modId.contains("free_cam"))
                        && !modId.contains("anti")) {
                    if (detectedIds.add(modId)) {
                        detected.add(new DetectedCheat(mod.getDisplayName(), "Freecam", "modid: " + mod.getModId()));
                    }
                }

                if (modId.contains("baritone")) {
                    if (detectedIds.add(modId)) {
                        detected.add(new DetectedCheat(mod.getDisplayName(), "Bot/Automation", "modid: " + mod.getModId()));
                    }
                }
            }
        } catch (Throwable t) {
            CreatorSMPCore.LOGGER.debug("Error while scanning mod list metadata", t);
        }

        // 3. ClassLoader reflection scan (for injected/agent-based cheat clients)
        ClassLoader cl = CheatDetector.class.getClassLoader();
        for (KnownCheat cheat : KNOWN_CLASSES) {
            String className = cheat.identifier();
            try {
                Class.forName(className, false, cl);
                if (detectedIds.add(className)) {
                    detected.add(new DetectedCheat(cheat.displayName(), cheat.category(), "class: " + className));
                }
            } catch (ClassNotFoundException ignored) {
                // Class not present, client is clean of this signature
            } catch (Throwable t) {
                // Ignore any security/verification exceptions
            }
        }

        return Collections.unmodifiableList(detected);
    }

    /**
     * Formats the list of detected cheats into a human-readable summary string.
     */
    public static String formatSummary(List<DetectedCheat> cheats) {
        if (cheats.isEmpty()) return "None";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cheats.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cheats.get(i).displayName()).append(" (").append(cheats.get(i).category()).append(")");
        }
        return sb.toString();
    }
}
