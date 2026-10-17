package im.nesp.creatorsmp.client.gui;

import im.nesp.creatorsmp.client.anticheat.CheatDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.List;

public class CheatWarningScreen extends Screen {
    private final Screen parentScreen;
    private final List<CheatDetector.DetectedCheat> detectedCheats;

    public CheatWarningScreen(Screen parentScreen, List<CheatDetector.DetectedCheat> detectedCheats) {
        super(Component.literal("CreatorSMP Anti-Cheat Warning"));
        this.parentScreen = parentScreen;
        this.detectedCheats = detectedCheats;
    }

    private int getCardWidth() {
        return Math.min(this.width - 40, 460);
    }

    private int getCardHeight() {
        int maxItemsHeight = Math.min(detectedCheats.size() * 16, 96);
        return Math.min(this.height - 30, 230 + maxItemsHeight);
    }

    private int getCardX() {
        return (this.width - getCardWidth()) / 2;
    }

    private int getCardY() {
        return (this.height - getCardHeight()) / 2;
    }

    @Override
    protected void init() {
        super.init();

        int cardHeight = getCardHeight();
        int cardY = getCardY();

        int buttonY = cardY + cardHeight - 30;
        int buttonWidth = 140;
        int gap = 16;
        int totalButtonsWidth = (buttonWidth * 2) + gap;
        int btn1X = (this.width - totalButtonsWidth) / 2;
        int btn2X = btn1X + buttonWidth + gap;

        // Button to open the mods folder directly in OS file explorer
        this.addRenderableWidget(Button.builder(
                Component.literal("Open Mods Folder").withStyle(ChatFormatting.AQUA),
                b -> {
                    File modsFolder = new File(this.minecraft.gameDirectory, "mods");
                    if (!modsFolder.exists()) {
                        modsFolder.mkdirs();
                    }
                    Util.getPlatform().openFile(modsFolder);
                }
        ).bounds(btn1X, buttonY, buttonWidth, 20).build());

        // Button to return safely to the previous screen
        this.addRenderableWidget(Button.builder(
                Component.literal("Back to Menu"),
                b -> this.onClose()
        ).bounds(btn2X, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the panorama & blur BEHIND our popup
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Render card background, borders, and inner list box (crisp and not blurred)
        int cardWidth = getCardWidth();
        int cardHeight = getCardHeight();
        int cardX = getCardX();
        int cardY = getCardY();

        // Card solid background
        guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, 0xF5140808);
        // Alert banner at top
        guiGraphics.fill(cardX, cardY, cardX + cardWidth, cardY + 34, 0xFF6A0D0D);
        // Outer glowing border
        guiGraphics.renderOutline(cardX, cardY, cardWidth, cardHeight, 0xFFFF3B3B);

        // Inset list box
        int boxX = cardX + 16;
        int boxY = cardY + 74;
        int boxWidth = cardWidth - 32;
        int boxHeight = cardHeight - 116;

        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xEE0B0404);
        guiGraphics.renderOutline(boxX, boxY, boxWidth, boxHeight, 0x66FF5555);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // super.render renders background (which renders our card in renderBackground) and then the buttons
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Render all text ON TOP of the card (fully readable, crisp, unblurred)
        int cardHeight = getCardHeight();
        int cardX = getCardX();
        int cardY = getCardY();

        // Header Title
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("⚠ CREATORSMP ANTI-CHEAT ⚠").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                this.width / 2,
                cardY + 12,
                0xFFFFFFFF
        );

        // Subtitle
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Unauthorized Modifications Detected!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                this.width / 2,
                cardY + 44,
                0xFFFFFFFF
        );

        // Description
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("You cannot join CreatorSMP while unauthorized cheats are installed:").withStyle(ChatFormatting.GRAY),
                this.width / 2,
                cardY + 58,
                0xFFCCCCCC
        );

        int boxX = cardX + 16;
        int boxY = cardY + 74;
        int boxHeight = cardHeight - 116;

        // Draw detected items inside the box
        int itemY = boxY + 8;
        int maxVisible = (boxHeight - 16) / 16;
        int count = Math.min(detectedCheats.size(), maxVisible);

        for (int i = 0; i < count; i++) {
            CheatDetector.DetectedCheat cheat = detectedCheats.get(i);

            Component text = Component.literal(" • ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal("[" + cheat.category() + "] ").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(cheat.displayName() + " ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("(" + cheat.identifier() + ")").withStyle(ChatFormatting.DARK_GRAY));

            guiGraphics.drawString(this.font, text, boxX + 8, itemY, 0xFFFFFFFF, false);
            itemY += 16;
        }

        if (detectedCheats.size() > maxVisible) {
            int remaining = detectedCheats.size() - maxVisible;
            Component moreText = Component.literal("   ... and " + remaining + " more unauthorized item(s)").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC);
            guiGraphics.drawString(this.font, moreText, boxX + 8, itemY, 0xFFFFFFFF, false);
        }

        // Action notice above buttons
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Remove these modifications to play on CreatorSMP.").withStyle(ChatFormatting.YELLOW),
                this.width / 2,
                cardY + cardHeight - 48,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
}
