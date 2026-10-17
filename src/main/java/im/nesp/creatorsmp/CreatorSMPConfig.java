package im.nesp.creatorsmp;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

public class CreatorSMPConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    // Server settings
    public static final ModConfigSpec.ConfigValue<String> SERVER_IP;
    public static final ModConfigSpec.IntValue SERVER_PORT;
    public static final ModConfigSpec.ConfigValue<String> SERVER_NAME;

    // GUI settings
    public static final ModConfigSpec.ConfigValue<String> BUTTON_TEXT;
    public static final ModConfigSpec.BooleanValue SPLIT_MULTIPLAYER_ROW;
    public static final ModConfigSpec.BooleanValue SHOW_TOOLTIP;

    // Anti-cheat settings
    public static final ModConfigSpec.BooleanValue ENFORCE_ANTI_CHEAT;
    public static final ModConfigSpec.BooleanValue CHECK_ALL_SERVERS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELISTED_MODS;

    static {
        BUILDER.push("server");
        SERVER_IP = BUILDER
                .comment("The server hostname or IP address to connect to.")
                .define("serverIp", "mc.nesp.im");
        SERVER_PORT = BUILDER
                .comment("The server port to connect to.")
                .defineInRange("serverPort", 25565, 1, 65535);
        SERVER_NAME = BUILDER
                .comment("The display name of the server.")
                .define("serverName", "CreatorSMP");
        BUILDER.pop();

        BUILDER.push("gui");
        BUTTON_TEXT = BUILDER
                .comment("Text displayed on the Title Screen join button.")
                .define("buttonText", "Join CreatorSMP");
        SPLIT_MULTIPLAYER_ROW = BUILDER
                .comment("Whether to split the multiplayer row in two equal halves (98px each) side by side. If false, places the button next to the 200px multiplayer button.")
                .define("splitMultiplayerRow", true);
        SHOW_TOOLTIP = BUILDER
                .comment("Whether to show a tooltip displaying the server address when hovering over the button.")
                .define("showTooltip", true);
        BUILDER.pop();

        BUILDER.push("anticheat");
        ENFORCE_ANTI_CHEAT = BUILDER
                .comment("Whether to prevent joining CreatorSMP when cheat clients, freecam, or xray mods are detected.")
                .define("enforceAntiCheat", true);
        CHECK_ALL_SERVERS = BUILDER
                .comment("If true, anti-cheat detection will block connections to ALL multiplayer servers, not just CreatorSMP.")
                .define("checkAllServers", false);
        WHITELISTED_MODS = BUILDER
                .comment("List of mod IDs to ignore in the anti-cheat scanner.")
                .defineList("whitelistedMods", List.of(), () -> "", o -> o instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    /**
     * Checks if the given address string or socket address points to CreatorSMP.
     */
    public static boolean isTargetServer(String address) {
        if (CHECK_ALL_SERVERS.get()) {
            return true;
        }
        if (address == null || address.isBlank()) {
            return false;
        }

        // Netty addresses may be in the format "mc.nesp.im/185.207.106.232:25565" or "/185.207.106.232:25565"
        String raw = address.trim().toLowerCase(Locale.ROOT);
        if (raw.contains("/")) {
            String[] segments = raw.split("/");
            for (String segment : segments) {
                if (checkSingleAddress(segment)) {
                    return true;
                }
            }
        }

        return checkSingleAddress(raw);
    }

    private static boolean checkSingleAddress(String segment) {
        if (segment == null || segment.isBlank()) return false;
        String clean = segment.trim().toLowerCase(Locale.ROOT);
        if (clean.contains(":")) {
            clean = clean.split(":")[0];
        }

        String configured = SERVER_IP.get().trim().toLowerCase(Locale.ROOT);
        if (configured.contains(":")) {
            configured = configured.split(":")[0];
        }

        if (clean.equals(configured) || clean.equals("mc.nesp.im")) {
            return true;
        }

        // Check if numeric IP matches resolved target host IP
        try {
            InetAddress target = InetAddress.getByName(configured);
            if (clean.equals(target.getHostAddress())) {
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            InetAddress defaultTarget = InetAddress.getByName("mc.nesp.im");
            if (clean.equals(defaultTarget.getHostAddress())) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }
}
