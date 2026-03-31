package rearth.oritech.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.util.ColorHelper;

import java.util.*;

public class AugmentSelectionScreen extends Screen {
    
    private static final int LINE_COLOR = ColorHelper.argb(150 / 255f, 180 / 255f, 220 / 255f, 0.8f);
    private static final int EXIT_COLOR = ColorHelper.argb(160 / 255f, 180 / 255f, 180 / 255f, 0.3f);
    private static final int EXIT_HOVER_COLOR = ColorHelper.argb(160 / 255f, 180 / 255f, 220 / 255f, 0.5f);
    
    private final List<SelectionEntry> augmentEntries = new ArrayList<>();
    private final Map<ResourceLocation, Float> augmentSizes = new HashMap<>();
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
        for (var augment : PlayerAugments.allAugments.values()) {
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(graphics);
        
        super.render(graphics, mouseX, mouseY, delta);
        
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
    
    private void drawBackdrop(GuiGraphics graphics, SelectionEntry focused) {
        int screenSize = Math.min(width, height);
        double innerRadius = 0.175;
        double outerRadius = 0.4;
        int augmentCount = Math.max(1, augmentEntries.size());
        double segmentSize = Math.toRadians(360d / augmentCount) * 0.8f;
        
        for (int i = 0; i < augmentEntries.size(); i++) {
            var entry = augmentEntries.get(i);
            var augmentData = PlayerAugments.allAugments.get(entry.id());
            if (augmentData == null) continue;
            
            boolean active = focused == entry;
            int color = ColorHelper.argb(180 / 255f, 30 / 255f, 30 / 255f, 0.3f);
            if (augmentData.isEnabled(minecraft.player)) {
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
    
    private void drawFocusLines(GuiGraphics graphics, SelectionEntry focused, int mouseX, int mouseY) {
        drawLine(graphics, centerEntry.centerX(), centerEntry.centerY(), focused.centerX(), focused.centerY(), LINE_COLOR, 0);
        drawLine(graphics, focused.centerX(), focused.centerY(), mouseX, mouseY, LINE_COLOR, 0);
    }
    
    private void drawAugmentIcons(GuiGraphics graphics, SelectionEntry focused) {
        for (var entry : augmentEntries) {
            boolean active = focused == entry;
            float scale = augmentSizes.getOrDefault(entry.id(), 1f);
            int size = Math.round(entry.size() * scale);
            int drawX = entry.centerX() - size / 2;
            int drawY = entry.centerY() - size / 2;
            
            if (active) {
                graphics.fill(drawX - 2, drawY - 2, drawX + size + 2, drawY + size + 2, ColorHelper.argb(0.8f, 0.85f, 0.9f, 0.35f));
            }
            
            graphics.blit(entry.texture(), drawX, drawY, size, size, 0, 0, 24, 24, 24, 24);
        }
    }
    
    private void drawLabels(GuiGraphics graphics, SelectionEntry focused) {
        var centerLabel = Component.literal("Exit");
        if (focused.id() != null) {
            centerLabel = Component.translatable(PlayerModifierScreen.augmentKey(focused.id()));
        }
        
        int labelWidth = minecraft.font.width(centerLabel);
        graphics.drawString(minecraft.font, centerLabel, centerEntry.centerX() - labelWidth / 2, centerEntry.centerY() - 4, 0xFFE7EEF5, true);
        graphics.drawString(minecraft.font, Component.translatable("oritech.text.augment_toggle"), 12, height - 20, 0xFFE7EEF5, true);
        graphics.drawString(minecraft.font, Component.translatable("oritech.text.augment_toggle_title"), 5, 5, 0xFFE7EEF5, true);
    }
    
    private static void drawPieSegmented(GuiGraphics graphics, double centerAngle, double angleSize,
                                         int centerX, int centerY, double innerRadius, double outerRadius,
                                         double screenSize, int color, int segments) {
        double segmentSize = angleSize / segments;
        double begin = centerAngle - angleSize * 0.5f;
        
        for (int i = 0; i < segments; i++) {
            double fromAngle = begin + segmentSize * i;
            double toAngle = fromAngle + segmentSize;
            
            int ax = centerX + (int) Math.round(innerRadius * Math.cos(fromAngle) * screenSize);
            int ay = centerY + (int) Math.round(innerRadius * Math.sin(fromAngle) * screenSize);
            int bx = centerX + (int) Math.round(outerRadius * Math.cos(fromAngle) * screenSize);
            int by = centerY + (int) Math.round(outerRadius * Math.sin(fromAngle) * screenSize);
            int cx = centerX + (int) Math.round(innerRadius * Math.cos(toAngle) * screenSize);
            int cy = centerY + (int) Math.round(innerRadius * Math.sin(toAngle) * screenSize);
            int dx = centerX + (int) Math.round(outerRadius * Math.cos(toAngle) * screenSize);
            int dy = centerY + (int) Math.round(outerRadius * Math.sin(toAngle) * screenSize);
            
            drawRect(graphics, dx, dy, bx, by, ax, ay, cx, cy, color);
        }
    }
    
    private static void drawLine(GuiGraphics graphics, int fromX, int fromY, int toX, int toY, int color, float zIndex) {
        if (fromX == toX && fromY == toY) return;
        
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var pose = graphics.pose();
        pose.pushPose();
        
        var matrix = pose.last().pose();
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) {
            pose.popPose();
            return;
        }
        
        float normalX = (float) (-dy / length);
        float normalY = (float) (dx / length);
        
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, fromX - normalX, fromY - normalY, zIndex).setColor(color);
        buffer.addVertex(matrix, fromX + normalX, fromY + normalY, zIndex).setColor(color);
        buffer.addVertex(matrix, toX + normalX, toY + normalY, zIndex).setColor(color);
        buffer.addVertex(matrix, toX - normalX, toY - normalY, zIndex).setColor(color);
        graphics.flush();
        
        pose.popPose();
    }
    
    private static void drawRect(GuiGraphics graphics, int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy, int color) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var pose = graphics.pose();
        pose.pushPose();
        
        var matrix = pose.last().pose();
        var buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, ax, ay, 0).setColor(color);
        buffer.addVertex(matrix, bx, by, 0).setColor(color);
        buffer.addVertex(matrix, cx, cy, 0).setColor(color);
        buffer.addVertex(matrix, dx, dy, 0).setColor(color);
        graphics.flush();
        
        pose.popPose();
    }
    
    private static int squaredDistance(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return dx * dx + dy * dy;
    }
    
    private void toggleAugment(ResourceLocation id) {
        NetworkManager.sendToServer(new PlayerAugments.AugmentPlayerTogglePacket(id));
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
    
    private record SelectionEntry(@Nullable ResourceLocation id, @Nullable ResourceLocation texture, int centerX,
                                  int centerY, int size) {
    }
}