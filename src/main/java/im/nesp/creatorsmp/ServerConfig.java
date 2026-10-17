package im.nesp.creatorsmp;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue REQUIRE_CLIENT_MOD;
    public static final ModConfigSpec.BooleanValue ENFORCE_SERVER_ANTI_CHEAT;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SERVER_WHITELISTED_MODS;
    public static final ModConfigSpec.IntValue HANDSHAKE_TIMEOUT_TICKS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.push("security");
        REQUIRE_CLIENT_MOD = BUILDER
                .comment("Disconnect clients that do not complete the CreatorSMP client handshake.")
                .define("requireClientMod", true);
        ENFORCE_SERVER_ANTI_CHEAT = BUILDER
                .comment("Check reported client mod IDs against the server's known unauthorized-mod list.")
                .define("enforceServerAntiCheat", true);
        SERVER_WHITELISTED_MODS = BUILDER
                .comment("Exact mod IDs excluded from the server's known-mod checks.")
                .defineList("whitelistedMods", List.of(), () -> "", value -> value instanceof String);
        HANDSHAKE_TIMEOUT_TICKS = BUILDER
                .comment("Ticks allowed for a joining client to answer the server challenge.")
                .defineInRange("handshakeTimeoutTicks", 100, 20, 600);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ServerConfig() {}
}