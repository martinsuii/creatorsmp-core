package im.nesp.creatorsmp.server;

import im.nesp.creatorsmp.CreatorSMPCore;
import im.nesp.creatorsmp.ServerConfig;
import im.nesp.creatorsmp.client.ClientHandshakeHandler;
import im.nesp.creatorsmp.network.ModReportPayload;
import im.nesp.creatorsmp.network.ServerChallengePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Server-side anti-cheat handler.
 *
 * <p>The server sends a fresh challenge on login. A client with this mod replies with
 * its challenge and a mod list; clients that do not reply in time can be disconnected.
 * The report is client-supplied and must not be treated as tamper-proof evidence.
 */
@EventBusSubscriber(modid = CreatorSMPCore.MODID, bus = EventBusSubscriber.Bus.GAME)
public class ServerAntiCheatHandler {

    private record PendingChallenge(String id, int ticks) {}

    private static final Map<UUID, PendingChallenge> PENDING = new HashMap<>();

    // ── Known cheat signatures (mod IDs to flag) ─────────────────────────

    private static final List<String> CHEAT_MOD_IDS = List.of(
            "xray", "advancedxray", "advanced_xray", "simple_xray", "xrayreloaded",
            "xray_reloaded", "pro_xray", "x_ray", "ore_detector",
            "freecam", "freecammod", "modfreecam", "simple_freecam",
            "tweaked_freecam", "free_cam",
            "baritone", "baritone-api", "seedcracker", "seedcrackerx",
            "meteor", "meteor-client", "meteordevelopment",
            "wurst", "wurst-client", "wurstclient",
            "aristois", "liquidbounce", "liquidbounce-nextgen",
            "bleachhack", "bleach", "rusherhack", "future",
            "inertia", "impact", "ares", "lambda", "kamiblue", "kami",
            "sigma", "mathax", "coffee", "atomic", "doomsday", "cornos"
    );

    // ── Network payload registration (MOD bus) ────────────────────────────

    @EventBusSubscriber(modid = CreatorSMPCore.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static class NetworkRegistration {
        @SubscribeEvent
        public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
            PayloadRegistrar registrar = event.registrar("1");
            registrar.playToServer(
                    ModReportPayload.TYPE,
                    ModReportPayload.STREAM_CODEC,
                    ServerAntiCheatHandler::handleModReport
            );
                registrar.playToClient(
                    ServerChallengePayload.TYPE,
                    ServerChallengePayload.STREAM_CODEC,
                        ClientHandshakeHandler::handleChallenge
                );
                CreatorSMPCore.LOGGER.info("[CreatorSMP] Registered handshake payloads.");
        }
    }

    // ── Payload handler (called on server main thread) ────────────────────

    private static void handleModReport(ModReportPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            UUID uuid = player.getUUID();
            PendingChallenge pending = PENDING.get(uuid);
            if (pending == null || !pending.id().equals(payload.challengeId())) {
                CreatorSMPCore.LOGGER.warn("Ignoring invalid or expired mod report from {}", player.getName().getString());
                return;
            }
            PENDING.remove(uuid);

            if (!ServerConfig.ENFORCE_SERVER_ANTI_CHEAT.get()) return;

            // Build detected cheat list from reported mod IDs
            List<String> cheatsFound = new ArrayList<>();
            Set<String> whitelist = new HashSet<>(ServerConfig.SERVER_WHITELISTED_MODS.get()
                    .stream().map(s -> s.toLowerCase(Locale.ROOT).trim()).toList());

            for (ModReportPayload.ModEntry entry : payload.mods()) {
                String id = entry.modId().toLowerCase(Locale.ROOT).trim();
                if (id.isEmpty()) continue;
                if (whitelist.contains(id)) continue;

                // Exact match
                if (CHEAT_MOD_IDS.contains(id)) {
                    cheatsFound.add(entry.displayName() + " [" + entry.modId() + "]");
                    continue;
                }

                // Heuristic: contains suspicious keywords
                if ((id.contains("xray") || id.contains("x_ray"))
                        && !id.contains("anti")) {
                    cheatsFound.add(entry.displayName() + " [" + entry.modId() + "] (xray heuristic)");
                    continue;
                }
                if ((id.contains("freecam") || id.contains("free_cam"))
                        && !id.contains("anti")) {
                    cheatsFound.add(entry.displayName() + " [" + entry.modId() + "] (freecam heuristic)");
                    continue;
                }
                if (id.contains("baritone")) {
                    cheatsFound.add(entry.displayName() + " [" + entry.modId() + "] (baritone heuristic)");
                }
            }

            if (cheatsFound.isEmpty()) {
                CreatorSMPCore.LOGGER.info("[CreatorSMP] Player {} passed server cheat check.", player.getName().getString());
                return;
            }

            CreatorSMPCore.LOGGER.warn("[CreatorSMP] Player {} has cheats: {}", player.getName().getString(), cheatsFound);
            kickForCheats(player, cheatsFound);
        });
    }

    // ── Player join guard (kicks if client mod missing and required) ───────

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        PENDING.remove(uuid);
        if (!ServerConfig.REQUIRE_CLIENT_MOD.get() && !ServerConfig.ENFORCE_SERVER_ANTI_CHEAT.get()) return;

        String challenge = UUID.randomUUID().toString();
        PENDING.put(uuid, new PendingChallenge(challenge, 0));
        PacketDistributor.sendToPlayer(player, new ServerChallengePayload(challenge));
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();
        PENDING.remove(uuid);
    }

    // ── Helper ────────────────────────────────────────────────────────────

    public static void kickForCheats(ServerPlayer player, List<String> cheats) {
        String cheatList = String.join(", ", cheats);
        Component reason = Component.literal(
                "§c§l[CreatorSMP Anti-Cheat]\n\n"
                + "§eYou cannot join this server with unauthorized modifications!\n\n"
                + "§7Detected:\n§c" + cheatList + "\n\n"
                + "§fRemove these mods and reconnect.");
        player.connection.disconnect(reason);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingChallenge>> iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingChallenge> entry = iterator.next();
            PendingChallenge pending = entry.getValue();
            int ticks = pending.ticks() + 1;
            if (ticks < ServerConfig.HANDSHAKE_TIMEOUT_TICKS.get()) {
                entry.setValue(new PendingChallenge(pending.id(), ticks));
                continue;
            }

            iterator.remove();
            if (!ServerConfig.REQUIRE_CLIENT_MOD.get()) continue;
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                player.connection.disconnect(Component.literal(
                        "CreatorSMP Core is required to join this server. Install the matching client mod and reconnect."));
            }
        }
    }
}
