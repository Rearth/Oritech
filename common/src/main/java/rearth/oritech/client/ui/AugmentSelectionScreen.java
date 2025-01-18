package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.OritechClient;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.network.NetworkContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class AugmentSelectionScreen extends BaseOwoScreen<FlowLayout> {
    
    private Component lastFocused;
    private LabelComponent noOpButton;
    private FlowLayout root;
    
    private final List<Component> augments = new ArrayList<>();
    private final HashMap<Component, Identifier> augmentIDs = new HashMap<>();
    private final HashMap<Component, Float> augmentSizes = new HashMap<>();
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        this.root = rootComponent;
        
        addAugments(rootComponent);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // update noop text
        if (lastFocused == noOpButton) {
            noOpButton.text(Text.literal("Exit"));
        } else if (lastFocused != null && lastFocused instanceof TextureComponent lastButton) {
            var focusedAugmentId = augmentIDs.get(lastButton);
            if (focusedAugmentId == null) return;
            var focusedAugment = Text.translatable("oritech.text.augment." + focusedAugmentId.getPath());
            noOpButton.text(focusedAugment);
        }
        
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        if (augments.isEmpty()) return;
        
        var mousingOver = augments.stream().findFirst().get();
        var minDist = Integer.MAX_VALUE;
        
        for (var augment : augments) {
            var mousePos = new Vector2i(mouseX, mouseY);
            var augmentPos = new Vector2i(augment.x() + augment.width() / 2, augment.y() + augment.height() / 2);
            
            var dist = mousePos.distance(augmentPos);
            
            if (augment == noOpButton) {
                dist *= 2;  // give no op less prio
            }
            if (dist < minDist) {
                minDist = (int) dist;
                mousingOver = augment;
            }
        }
        
        lastFocused = mousingOver;
        
        var centerPos = new Vector2i(noOpButton.x() + noOpButton.width() / 2, noOpButton.y() + noOpButton.height() / 2);
        var selectedPos = new Vector2i(lastFocused.x() + lastFocused.width() / 2, lastFocused.y() + lastFocused.height() / 2);
        var mousePos = new Vector2i(mouseX, mouseY);
        
        var mouseLineColor = new Color(150 / 255f, 180 / 255f, 220 / 255f, 0.8f).argb();
        drawLine(context, centerPos, selectedPos, mouseLineColor);
        drawLine(context, selectedPos, mousePos, mouseLineColor);
        
        var screenSize = root.height();
        var innerRadius = 0.175;
        var outerRadius = 0.4;
        var screenSizeX = root.width();
        var screenSizeY = root.height();
        var middleX = screenSizeX / 2f;
        var middleY = screenSizeY / 2f;
        
        var augmentCount = augments.size() - 1;
        var radSizePerElement = Math.toRadians((double) 360 / augmentCount) * 0.8f;
        var screenMiddle = new Vector2i((int) middleX, (int) middleY);
        
        for (int i = 0; i < augments.size(); i++) {
            var augment = augments.get(i);
            var augmentId = augmentIDs.get(augment);
            var augmentData = PlayerAugments.allAugments.get(augmentId);
            if (augmentData == null) continue;
            var isEnabled = augmentData.isEnabled(client.player);
            var active = mousingOver == augment;
            
            var color = new Color(180 / 255f, 30 / 255f, 30 / 255f, 0.3f).argb();  // red
            if (isEnabled) {
                color = new Color(30 / 255f, 180 / 255f, 30 / 255f, 0.3f).argb();  // green
            }
            if (active)
                color = new Color(160 / 255f, 180 / 255f, 220 / 255f, 0.5f).argb(); // white
            
            var augmentRad = (i / (float) augmentCount) * 2 * Math.PI - Math.toRadians(90);
            
            var sizeTarget = 1f;
            if (active) sizeTarget = 1.05f;
            var lastSize = augmentSizes.getOrDefault(augment, 1f);
            
            var usedSize = MathHelper.lerp(0.15, lastSize, sizeTarget);
            augmentSizes.put(augment, (float) usedSize);
            
            var activeInnerRadius = innerRadius / usedSize;
            var activeOuterRadius = outerRadius * usedSize;
            
            if (i != augments.size() - 1)
                drawPieSegmented(context, augmentRad, radSizePerElement, screenMiddle, activeInnerRadius, activeOuterRadius, screenSize, color, 16);
            
        }
        
        var centerSelected = mousingOver == noOpButton;
        var color = new Color(160 / 255f, 180 / 255f, 180 / 255f, 0.3f).argb();
        if (centerSelected) {
            color = new Color(160 / 255f, 180 / 255f, 220 / 255f, 0.5f).argb();
        }
        drawPieSegmented(context, 0, Math.toRadians(360), screenMiddle, 0, innerRadius * 0.6, screenSize, color, 32);
        
    }
    
    private static void drawPieSegmented(DrawContext context, double augmentRad, double radSize, Vector2i screenMiddle, double innerRadius, double outerRadius, double screenSize, int color, int segmentCount) {
        
        // total size
        var segmentSize = radSize / segmentCount;
        var augmentRadBegin = augmentRad - radSize * 0.5f;
        
        for (int i = 0; i < segmentCount; i++) {
            
            var fromRad = augmentRadBegin + segmentSize * i;
            var toRad = fromRad + segmentSize;
            
            var a = new Vector2i(screenMiddle).add(new Vector2i((int) (innerRadius * Math.cos(fromRad) * screenSize), (int) (innerRadius * Math.sin(fromRad) * screenSize)));
            var b = new Vector2i(screenMiddle).add(new Vector2i((int) (outerRadius * Math.cos(fromRad) * screenSize), (int) (outerRadius * Math.sin(fromRad) * screenSize)));
            var c = new Vector2i(screenMiddle).add(new Vector2i((int) (innerRadius * Math.cos(toRad) * screenSize), (int) (innerRadius * Math.sin(toRad) * screenSize)));
            var d = new Vector2i(screenMiddle).add(new Vector2i((int) (outerRadius * Math.cos(toRad) * screenSize), (int) (outerRadius * Math.sin(toRad) * screenSize)));
            drawRect(context, d, b, a, c, color);
        }
    }
    
    private static void drawLine(DrawContext context, Vector2i from, Vector2i to, int color) {
        
        if (from.distanceSquared(to) < 0.1) return;
        
        var matrices = context.getMatrices();
        matrices.push();
        
        var pos = matrices.peek().getPositionMatrix();
        var normal = getNormalVector(from, to).normalize();
        var offset = normal.mul(1);
        var zIndex = 0;
        
        var buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        buffer.vertex(pos, from.x - offset.x, from.y - offset.y, zIndex).color(color);
        buffer.vertex(pos, from.x + offset.x, from.y + offset.y, zIndex).color(color);
        buffer.vertex(pos, to.x + offset.x, to.y + offset.y, zIndex).color(color);
        buffer.vertex(pos, to.x - offset.x, to.y - offset.y, zIndex).color(color);
        context.draw();
        
        matrices.pop();
    }
    
    private static void drawRect(DrawContext context, Vector2i a, Vector2i b, Vector2i c, Vector2i d, int color) {
        
        var matrices = context.getMatrices();
        matrices.push();
        
        var pos = matrices.peek().getPositionMatrix();
        var zIndex = 0;
        
        var buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        buffer.vertex(pos, a.x, a.y, zIndex).color(color);
        buffer.vertex(pos, b.x, b.y, zIndex).color(color);
        buffer.vertex(pos, c.x, c.y, zIndex).color(color);
        buffer.vertex(pos, d.x, d.y, zIndex).color(color);
        context.draw();
        
        matrices.pop();
    }
    
    public static Vector2f getNormalVector(Vector2i point1, Vector2i point2) {
        int dx = point2.x - point1.x;
        int dy = point2.y - point1.y;
        
        // A 90-degree rotation can be achieved by swapping x and y and negating one of them
        return new Vector2f(-dy, dx);
    }
    
    private void addAugments(FlowLayout parent) {
        
        var player = Objects.requireNonNull(this.client).player;
        
        var augmentsToAdd = new ArrayList<PlayerAugments.PlayerAugment>();
        
        for (var augment : PlayerAugments.allAugments.values()) {
            var isInstalled = augment.isInstalled(player);
            var isToggleable = augment.toggleable;
            
            if (!isInstalled || !isToggleable) continue;
            
            augmentsToAdd.add(augment);
            
        }
        
        var augmentCount = augmentsToAdd.size();
        var radius = 32;
        
        var screenSizeX = this.width;
        var screenSizeY = this.height;
        var sideRelative = screenSizeY / (float) screenSizeX;
        
        for (int i = 0; i < augmentsToAdd.size(); i++) {
            var augment = augmentsToAdd.get(i);
            var angleRad = (i / (float) augmentCount) * 2 * Math.PI - Math.toRadians(90);
            var offsetX = radius * Math.cos(angleRad);
            var offsetY = radius * Math.sin(angleRad);
            
            final var id = augment.id;
            var iconTexture = Oritech.id("textures/gui/augments/" + id.getPath() + ".png");
            var label = Components.texture(iconTexture, 0, 0, 24, 24, 24, 24);
            label.positioning(Positioning.relative((int) (50 + offsetX * sideRelative), (int) (50 + offsetY)));
            label.sizing(Sizing.fixed(36));
            
            augments.add(label);
            parent.child(label);
            augmentIDs.put(label, id);
            augmentSizes.put(label, 1f);
            
        }
        
        var noOpLabel = Components.label(Text.literal("Nothing"));
        noOpLabel.positioning(Positioning.relative(50, 50));
        augments.add(noOpLabel);
        noOpButton = noOpLabel;
        parent.child(noOpLabel);
        
    }
    
    private void toggleAugment(Identifier id) {
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.AugmentPlayerTogglePacket(id));
    }
    
    @Override
    public void close() {
        
        if (lastFocused != null && augmentIDs.containsKey(lastFocused)) {
            var id = augmentIDs.get(lastFocused);
            toggleAugment(id);
        }
        
        OritechClient.activeScreen = null;
        super.close();
    }
}
