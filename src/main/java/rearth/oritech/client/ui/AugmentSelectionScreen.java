package rearth.oritech.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.util.ColorHelper;

import java.util.*;

public class AugmentSelectionScreen extends Screen {
    
    private static final int LINE_COLOR = ColorHelper.argb(150 / 255f, 180 / 255f, 220 / 255f, 0.8f);
    private static final int EXIT_COLOR = ColorHelper.argb(160 / 255f, 180 / 255f, 180 / 255f, 0.3f);
    private static final int EXIT_HOVER_COLOR = ColorHelper.argb(160 / 255f, 180 / 255f, 220 / 255f, 0.5f);
    
    private final List<SelectionEntry> augmentEntries = new ArrayList<>();
    private final Map<Identifier, Float> augmentSizes = new HashMap<>();
    private SelectionEntry centerEntry;
    private @Nullable SelectionEntry lastFocused;
    
    public AugmentSelectionScreen() {
        super(Component.empty());
    }
    
    @Override
    protected void init() {
        super.init();
        rebuildEntries();
    }
    
    private void rebuildEntries() {
        augmentEntries.clear();
        augmentSizes.clear();
        
        var player = Objects.requireNonNull(this.minecraft).player;
        if (player == null) return;
        
        var available = new ArrayList<Augment>();
        for (var augment : PlayerAugments.getAllAugments(player.registryAccess()).values()) {
            if (augment.isInstalled(player) && augment.toggleable) {
                available.add(augment);
            }
        }
        
        float radius = Math.min(width, height) * 0.28f;
        int iconSize = Math.max(24, height / 12);
        int centerX = width / 2;
        int centerY = height / 2;
        
        for (int i = 0; i < available.size(); i++) {
            var augment = available.get(i);
            double angle = (i / (double) available.size()) * Math.PI * 2 - Math.toRadians(90);
            int x = (int) Math.round(centerX + Math.cos(angle) * radius);
            int y = (int) Math.round(centerY + Math.sin(angle) * radius);
            augmentEntries.add(new SelectionEntry(augment.id, Oritech.id("textures/gui/" + augment.id.getPath() + ".png"), x, y, iconSize));
            augmentSizes.put(augment.id, 1f);
        }
        
        centerEntry = new SelectionEntry(null, null, centerX, centerY, iconSize);
    }
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
        
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        
        if (centerEntry == null) {
            return;
        }
        
        var focused = findFocused(mouseX, mouseY);
        lastFocused = focused;
        
        drawBackdrop(graphics, focused);
        drawFocusLines(graphics, focused, mouseX, mouseY);
        drawAugmentIcons(graphics, focused);
        drawLabels(graphics, focused);
    }
    
    private SelectionEntry findFocused(int mouseX, int mouseY) {
        SelectionEntry focused = centerEntry;
        int minDist = Integer.MAX_VALUE;
        
        for (var entry : augmentEntries) {
            int dist = squaredDistance(mouseX, mouseY, entry.centerX(), entry.centerY());
            if (dist < minDist) {
                minDist = dist;
                focused = entry;
            }
        }
        
        int centerDist = squaredDistance(mouseX, mouseY, centerEntry.centerX(), centerEntry.centerY()) * 2;
        if (centerDist < minDist) {
            focused = centerEntry;
        }
        
        return focused;
    }
    
    private void drawBackdrop(GuiGraphicsExtractor graphics, SelectionEntry focused) {
        var player = minecraft != null ? minecraft.player : null;
        if (player == null) return;
        
        int screenSize = Math.min(width, height);
        double innerRadius = 0.175;
        double outerRadius = 0.4;
        int augmentCount = Math.max(1, augmentEntries.size());
        double segmentSize = Math.toRadians(360d / augmentCount) * 0.8f;
        
        for (int i = 0; i < augmentEntries.size(); i++) {
            var entry = augmentEntries.get(i);
            var augmentData = PlayerAugments.getAugment(player.registryAccess(), entry.id());
            if (augmentData == null) continue;
            
            boolean active = focused == entry;
            int color = ColorHelper.argb(180 / 255f, 30 / 255f, 30 / 255f, 0.3f);
            if (augmentData.isEnabled(player)) {
                color = ColorHelper.argb(30 / 255f, 180 / 255f, 30 / 255f, 0.3f);
            }
            if (active) {
                color = ColorHelper.argb(160 / 255f, 180 / 255f, 220 / 255f, 0.5f);
            }
            
            double angle = (i / (double) augmentCount) * Math.PI * 2 - Math.toRadians(90);
            float lastSize = augmentSizes.getOrDefault(entry.id(), 1f);
            float targetSize = active ? 1.05f : 1f;
            float usedSize = Mth.lerp(0.15f, lastSize, targetSize);
            augmentSizes.put(entry.id(), usedSize);
            
            double activeInnerRadius = innerRadius / usedSize;
            double activeOuterRadius = outerRadius * usedSize;
            drawPieSegmented(graphics, angle, segmentSize, centerEntry.centerX(), centerEntry.centerY(), activeInnerRadius, activeOuterRadius, screenSize, color, 16);
        }
        
        int centerColor = focused == centerEntry ? EXIT_HOVER_COLOR : EXIT_COLOR;
        drawPieSegmented(graphics, 0, Math.toRadians(360), centerEntry.centerX(), centerEntry.centerY(), 0, innerRadius * 0.6, screenSize, centerColor, 32);
    }
    
    private void drawFocusLines(GuiGraphicsExtractor graphics, SelectionEntry focused, int mouseX, int mouseY) {
        drawLine(graphics, centerEntry.centerX(), centerEntry.centerY(), focused.centerX(), focused.centerY(), LINE_COLOR, 0);
        drawLine(graphics, focused.centerX(), focused.centerY(), mouseX, mouseY, LINE_COLOR, 0);
    }
    
    private void drawAugmentIcons(GuiGraphicsExtractor graphics, SelectionEntry focused) {
        for (var entry : augmentEntries) {
            boolean active = focused == entry;
            float scale = augmentSizes.getOrDefault(entry.id(), 1f);
            int size = Math.round(entry.size() * scale);
            int drawX = entry.centerX() - size / 2;
            int drawY = entry.centerY() - size / 2;
            if (entry.texture() == null) continue;
            
            if (active) {
                graphics.fill(drawX - 2, drawY - 2, drawX + size + 2, drawY + size + 2, ColorHelper.argb(0.8f, 0.85f, 0.9f, 0.35f));
            }
            
            graphics.blit(RenderPipelines.GUI_TEXTURED, entry.texture(), drawX, drawY, 0f, 0f, size, size, 24, 24, 24, 24);
        }
    }
    
    private void drawLabels(GuiGraphicsExtractor graphics, SelectionEntry focused) {
        if (minecraft == null) return;
        
        var centerLabel = Component.literal("Exit");
        if (focused.id() != null) {
            centerLabel = Component.translatable(PlayerModifierScreen.augmentKey(focused.id()));
        }
        
        int labelWidth = minecraft.font.width(centerLabel);
        graphics.text(minecraft.font, centerLabel, centerEntry.centerX() - labelWidth / 2, centerEntry.centerY() - 4, 0xFFE7EEF5, true);
        graphics.text(minecraft.font, Component.translatable("oritech.text.augment_toggle"), 12, height - 20, 0xFFE7EEF5, true);
        graphics.text(minecraft.font, Component.translatable("oritech.text.augment_toggle_title"), 5, 5, 0xFFE7EEF5, true);
    }
    
    // Stubbed: the immediate-mode vertex API (RenderSystem.setShader / bufferSource / addVertex)
    // used to draw arbitrary 2D pies, lines and quads no longer exists in 26.1. Reimplementing
    // arbitrary-rotation 2D shapes requires custom GuiRenderState submission, which is out of
    // scope for the initial migration. The radial selector will be visually plain until then.
    private static void drawPieSegmented(GuiGraphicsExtractor graphics, double centerAngle, double angleSize,
                                         int centerX, int centerY, double innerRadius, double outerRadius,
                                         double screenSize, int color, int segments) {
        // no-op
    }
    
    private static void drawLine(GuiGraphicsExtractor graphics, int fromX, int fromY, int toX, int toY, int color, float zIndex) {
        // no-op (see drawPieSegmented note)
    }
    
    private static void drawRect(GuiGraphicsExtractor graphics, int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy, int color) {
        // no-op (see drawPieSegmented note)
    }
    
    private static int squaredDistance(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return dx * dx + dy * dy;
    }
    
    private void toggleAugment(Identifier id) {
        PacketDistributor.sendToServer(new PlayerAugments.AugmentPlayerTogglePacket(id));
    }
    
    @Override
    public void onClose() {
        if (lastFocused != null && lastFocused.id() != null) {
            toggleAugment(lastFocused.id());
        }
        
        OritechClient.activeScreen = null;
        super.onClose();
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    private record SelectionEntry(@Nullable Identifier id, @Nullable Identifier texture, int centerX,
                                  int centerY, int size) {
    }
}
