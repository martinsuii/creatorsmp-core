package im.nesp.creatorsmp.client;

import im.nesp.creatorsmp.CreatorSMPConfig;
import im.nesp.creatorsmp.CreatorSMPCore;
import im.nesp.creatorsmp.client.anticheat.CheatDetector;
import im.nesp.creatorsmp.client.gui.CheatWarningScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

@EventBusSubscriber(modid = CreatorSMPCore.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class TitleScreenHandler {

    @SubscribeEvent
    public static void onScreenInitPost(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen titleScreen)) {
            return;
        }

        Button multiplayerButton = null;
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof Button button) {
                Component message = button.getMessage();
                if (message.getContents() instanceof TranslatableContents translatable) {
                    if ("menu.multiplayer".equals(translatable.getKey())) {
                        multiplayerButton = button;
                        break;
                    }
                } else if (message.getString().equalsIgnoreCase(Component.translatable("menu.multiplayer").getString())) {
                    multiplayerButton = button;
                    break;
                }
            }
        }

        int btnX;
        int btnY;
        int btnWidth;
        int btnHeight = 20;

        boolean splitRow = CreatorSMPConfig.SPLIT_MULTIPLAYER_ROW.get();

        if (multiplayerButton != null) {
            btnY = multiplayerButton.getY();
            if (splitRow) {
                // Resize multiplayer button to 98px (left side of standard 200px row)
                multiplayerButton.setWidth(98);
                multiplayerButton.setX(titleScreen.width / 2 - 100);

                // Place "Join CreatorSMP" button next to it (right side)
                btnX = titleScreen.width / 2 + 2;
                btnWidth = 98;
            } else {
                // Position to the right of the full multiplayer button
                btnX = multiplayerButton.getX() + multiplayerButton.getWidth() + 4;
                btnWidth = 100;
            }
        } else {
            // Fallback layout if multiplayer button could not be found
            btnY = titleScreen.height / 4 + 72;
            btnX = titleScreen.width / 2 + 2;
            btnWidth = 98;
        }

        String buttonTextStr = CreatorSMPConfig.BUTTON_TEXT.get();
        Component buttonText = Component.literal(buttonTextStr).withStyle(ChatFormatting.GOLD);

        Button.Builder builder = Button.builder(buttonText, b -> {
            joinServer(titleScreen);
        }).bounds(btnX, btnY, btnWidth, btnHeight);

        if (CreatorSMPConfig.SHOW_TOOLTIP.get()) {
            String ip = CreatorSMPConfig.SERVER_IP.get();
            int port = CreatorSMPConfig.SERVER_PORT.get();
            String host = (port == 25565) ? ip : (ip + ":" + port);
            builder.tooltip(Tooltip.create(
                    Component.literal("Connect directly to ").append(Component.literal(host).withStyle(ChatFormatting.YELLOW))
            ));
        }

        event.addListener(builder.build());
    }

    private static void joinServer(TitleScreen screen) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Anti-Cheat Check: If unauthorized mods/clients are detected, block and show warning screen
        if (CreatorSMPConfig.ENFORCE_ANTI_CHEAT.get()) {
            List<CheatDetector.DetectedCheat> detectedCheats = CheatDetector.scan();
            if (!detectedCheats.isEmpty()) {
                CreatorSMPCore.LOGGER.warn("CreatorSMP join attempt blocked due to detected unauthorized mods: {}", detectedCheats);
                mc.setScreen(new CheatWarningScreen(screen, detectedCheats));
                return;
            }
        }

        // 2. Otherwise proceed with standard direct connection
        String ip = CreatorSMPConfig.SERVER_IP.get();
        int port = CreatorSMPConfig.SERVER_PORT.get();
        String host = (port == 25565) ? ip : (ip + ":" + port);
        String serverName = CreatorSMPConfig.SERVER_NAME.get();

        CreatorSMPCore.LOGGER.info("Connecting to {} ({}) via CreatorSMP join button", serverName, host);

        ServerAddress serverAddress = ServerAddress.parseString(host);
        ServerData serverData = new ServerData(serverName, host, ServerData.Type.OTHER);

        ConnectScreen.startConnecting(screen, mc, serverAddress, serverData, false, null);
    }
}
