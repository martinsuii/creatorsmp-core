package im.nesp.creatorsmp.client.anticheat;

import im.nesp.creatorsmp.CreatorSMPConfig;
import im.nesp.creatorsmp.CreatorSMPCore;
import im.nesp.creatorsmp.client.gui.CheatWarningScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

@EventBusSubscriber(modid = CreatorSMPCore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ConnectionInterceptor {

    /**
     * Intercepts opening screens before connection packets are sent.
     * Intercepts connections initiated via the Multiplayer list, Direct Connect menu, or external scripts.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (!CreatorSMPConfig.ENFORCE_ANTI_CHEAT.get()) {
            return;
        }

        Screen newScreen = event.getNewScreen();
        if (!(newScreen instanceof ConnectScreen connectScreen)) {
            return;
        }

        List<CheatDetector.DetectedCheat> detectedCheats = CheatDetector.scan();
        if (detectedCheats.isEmpty()) {
            return;
        }

        Screen currentScreen = event.getCurrentScreen();
        String targetIp = extractTargetServer(currentScreen);

        boolean isCreatorTarget = false;
        if (CreatorSMPConfig.CHECK_ALL_SERVERS.get()) {
            isCreatorTarget = true;
        } else if (targetIp != null && CreatorSMPConfig.isTargetServer(targetIp)) {
            isCreatorTarget = true;
        }

        if (isCreatorTarget) {
            CreatorSMPCore.LOGGER.warn("Intercepted manual connection to CreatorSMP ({}) with cheats loaded: {}", targetIp, detectedCheats);

            // Abort connect thread on the ConnectScreen
            try {
                Field abortedField = ConnectScreen.class.getDeclaredField("aborted");
                abortedField.setAccessible(true);
                abortedField.setBoolean(connectScreen, true);
            } catch (Throwable ignored) {}

            // Replace ConnectScreen with our beautiful warning screen
            Screen returnScreen = (currentScreen != null) ? currentScreen : newScreen;
            event.setNewScreen(new CheatWarningScreen(returnScreen, detectedCheats));
        }
    }

    /**
     * Secondary defense layer: Intercepts network login packet in case connection screen was bypassed
     * (e.g. quick play, reconnect, mod shortcuts). Disconnects immediately with a warning notice.
     */
    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!CreatorSMPConfig.ENFORCE_ANTI_CHEAT.get()) {
            return;
        }

        List<CheatDetector.DetectedCheat> detectedCheats = CheatDetector.scan();
        if (detectedCheats.isEmpty()) {
            return;
        }

        SocketAddress address = event.getConnection().getRemoteAddress();
        String host = (address != null) ? address.toString() : "";
        if (address instanceof InetSocketAddress inet) {
            host = inet.getHostString();
            if (inet.getAddress() != null) {
                host = host + "/" + inet.getAddress().getHostAddress();
            }
        }

        ServerData current = Minecraft.getInstance().getCurrentServer();
        String serverIp = (current != null) ? current.ip : host;

        if (CreatorSMPConfig.isTargetServer(serverIp) || CreatorSMPConfig.isTargetServer(host)) {
            CreatorSMPCore.LOGGER.warn("Disconnecting client from CreatorSMP! Detected cheats: {}", detectedCheats);

            Component disconnectMessage = Component.literal("§c§l[CreatorSMP Anti-Cheat Warning]\n\n")
                    .append(Component.literal("§eYou cannot join CreatorSMP with unauthorized modifications installed!\n\n"))
                    .append(Component.literal("§7Detected Cheats:\n"))
                    .append(Component.literal("§c" + CheatDetector.formatSummary(detectedCheats) + "\n\n"))
                    .append(Component.literal("§fPlease remove these files from your mods directory before reconnecting."));

            event.getConnection().disconnect(disconnectMessage);
        }
    }

    private static String extractTargetServer(Screen currentScreen) {
        // 1. Check if joined via Direct Connection Screen
        if (currentScreen instanceof DirectJoinServerScreen directScreen) {
            try {
                Field ipField = DirectJoinServerScreen.class.getDeclaredField("ipEdit");
                ipField.setAccessible(true);
                EditBox editBox = (EditBox) ipField.get(directScreen);
                if (editBox != null && !editBox.getValue().isBlank()) {
                    return editBox.getValue();
                }

                Field dataField = DirectJoinServerScreen.class.getDeclaredField("serverData");
                dataField.setAccessible(true);
                ServerData data = (ServerData) dataField.get(directScreen);
                if (data != null && data.ip != null) {
                    return data.ip;
                }
            } catch (Throwable ignored) {}
        }

        // 2. Check if joined via Multiplayer Server List
        if (currentScreen instanceof JoinMultiplayerScreen multiplayerScreen) {
            // Check direct connect field on Multiplayer screen
            try {
                Field editingField = JoinMultiplayerScreen.class.getDeclaredField("editingServer");
                editingField.setAccessible(true);
                ServerData editingData = (ServerData) editingField.get(multiplayerScreen);
                if (editingData != null && editingData.ip != null && !editingData.ip.isBlank()) {
                    return editingData.ip;
                }
            } catch (Throwable ignored) {}

            // Check selected entry in server list
            try {
                Field listField = JoinMultiplayerScreen.class.getDeclaredField("serverSelectionList");
                listField.setAccessible(true);
                ServerSelectionList list = (ServerSelectionList) listField.get(multiplayerScreen);
                if (list != null) {
                    ServerSelectionList.Entry selected = list.getSelected();
                    if (selected instanceof ServerSelectionList.OnlineServerEntry onlineEntry) {
                        ServerData data = onlineEntry.getServerData();
                        if (data != null && data.ip != null) {
                            return data.ip;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 3. Fallback: Search active ConnectScreen connection threads
        try {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if (t.getClass().getName().contains("ConnectScreen$1")) {
                    try {
                        Field serverDataField = t.getClass().getDeclaredField("val$serverData");
                        serverDataField.setAccessible(true);
                        ServerData sd = (ServerData) serverDataField.get(t);
                        if (sd != null && sd.ip != null && !sd.ip.isBlank()) {
                            return sd.ip;
                        }
                    } catch (Throwable ignored) {}

                    try {
                        Field serverAddressField = t.getClass().getDeclaredField("val$serverAddress");
                        serverAddressField.setAccessible(true);
                        ServerAddress sa = (ServerAddress) serverAddressField.get(t);
                        if (sa != null && sa.getHost() != null) {
                            return sa.getHost();
                        }
                    } catch (Throwable ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
