package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.interaction.PlayerModifierTestEntity;
import rearth.oritech.network.NetworkContent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlayerModifierScreen extends BaseOwoHandledScreen<FlowLayout, PlayerModifierScreenHandler> {
    
    private static DraggableScrollContainer<FlowLayout> main;
    private static final HashSet<Pair<Vector2i, Vector2i>> dependencyLines = new HashSet<>();
    private final Set<BoxComponent> highlighters = new HashSet<>();
    
    public PlayerModifierScreen(PlayerModifierScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, CustomFlowRootContainer::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        dependencyLines.clear();
        
        var outerContainer = Containers.horizontalFlow(Sizing.fill(60), Sizing.fill(80));
        outerContainer.surface(Surface.PANEL);
        
        var movedPanel = Containers.horizontalFlow(Sizing.fixed(800), Sizing.fill());
        movedPanel.surface(Surface.tiled(Oritech.id("textures/block/machine_plating_block/empty.png"), 16, 16));
        movedPanel.margins(Insets.of(2));
        
        var innerContainer = new DraggableScrollContainer<>(ScrollContainer.ScrollDirection.HORIZONTAL, Sizing.fill(), Sizing.fill(), movedPanel);
        innerContainer.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        innerContainer.surface(Surface.PANEL_INSET);
        innerContainer.margins(Insets.of(6));
        main = innerContainer;
        
        rootComponent.child(outerContainer);
        outerContainer.child(innerContainer);
        
        addAvailableAugments(movedPanel);
        
    }
    
    @Override
    public void render(DrawContext vanillaContext, int mouseX, int mouseY, float delta) {
        
        for (var highlight : highlighters) {
            var isActive = highlight.isInBoundingBox(mouseX, mouseY);
            if (isActive) {
                highlight.color(new Color(0.7f, 0.7f, 0.7f, 1f));
            } else {
                highlight.color(new Color(0.7f, 0.7f, 0.7f, 0f));
            }
        }
        
        super.render(vanillaContext, mouseX, mouseY, delta);
    }
    
    private void addAvailableAugments(FlowLayout parent) {
        
        var maxHeight = this.height * 0.7f;
        var leftOffset = 20;
        
        for (var augmentId : PlayerModifierTestEntity.allAugments.keySet()) {
            var uiData = PlayerModifierTestEntity.augmentAssets.get(augmentId);
            var isResearched = this.handler.blockEntity.researchedAugments.contains(augmentId);
            var isApplied = this.handler.blockEntity.hasPlayerAugment(augmentId, this.handler.player);
            
            var hasRequirements = true;
            var missingRequirements = new ArrayList<Text>();
            missingRequirements.add(Text.translatable("oritech.text.augment." + augmentId.getPath()).formatted(Formatting.BOLD));
            missingRequirements.add(Text.translatable("oritech.text.missing_requirements_title"));
            for (var requirementId : uiData.requirements()) {
                if (!this.handler.blockEntity.researchedAugments.contains(requirementId)) {
                    hasRequirements = false;
                    missingRequirements.add(Text.translatable("oritech.text.augment." + requirementId.getPath()).formatted(Formatting.ITALIC));
                }
            }
            
            var text = augmentId.getPath();
            var operation = AugmentOperation.RESEARCH;
            if (isApplied) {
                text += " remove";
                operation = AugmentOperation.REMOVE;
            } else if (isResearched) {
                text += " apply";
                operation = AugmentOperation.ADD;
            } else {
                text += " research";
            }
            
            final var finalText = text;
            final var finalOperation = operation;
            final var augmentOpId = augmentId;
            
            var position = new Vector2i(leftOffset + uiData.position().x * 4, (int) (uiData.position().y / 100f * maxHeight));
            
            var iconTexture = Oritech.id("textures/gui/augments/exoskeleton.png");
            var backgroundTexture = Oritech.id("textures/gui/augments/background_open.png");
            
            if (isApplied) {
                backgroundTexture = Oritech.id("textures/gui/augments/background_installed.png");
            } else if (isResearched) {
                backgroundTexture = Oritech.id("textures/gui/augments/background_completed.png");
            }
            
            var iconSize = 24;
            var backgroundSize = 32;
            
            var icon = Components.texture(iconTexture, 0, 0, 32, 32, 32, 32);
            icon.mouseDown().subscribe((a, b, c) -> {triggerAugmentOperation(augmentOpId, finalOperation); return true;});
            icon.sizing(Sizing.fixed(iconSize), Sizing.fixed(iconSize));
            icon.positioning(Positioning.absolute(position.x - iconSize / 2, position.y - iconSize / 2));
            icon.tooltip(Text.literal(finalText));
            
            var background = Components.texture(backgroundTexture, 0, 0, 16, 16, 16, 16);
            background.sizing(Sizing.fixed(backgroundSize), Sizing.fixed(backgroundSize));
            background.positioning(Positioning.absolute(position.x - backgroundSize / 2, position.y - backgroundSize / 2));
            
            var highlight = Components.box(Sizing.fixed(backgroundSize + 2), Sizing.fixed(backgroundSize + 2));
            highlight.color(new Color(0.7f, 0.7f, 0.7f, 1f));
            highlight.positioning(Positioning.absolute(position.x - backgroundSize / 2 - 1, position.y - backgroundSize / 2 - 1));
            
            var blocker = Components.box(Sizing.fixed(backgroundSize), Sizing.fixed(backgroundSize));
            blocker.color(new Color(0.3f, 0.4f, 0.4f, 0.8f));
            blocker.zIndex(2);
            blocker.fill(true);
            blocker.tooltip(missingRequirements);
            blocker.positioning(Positioning.absolute(position.x - backgroundSize / 2, position.y - backgroundSize / 2));
            
            for (var dependencyId : uiData.requirements()) {
                var dependency = PlayerModifierTestEntity.augmentAssets.get(dependencyId);
                var dependencyPos = new Vector2i(leftOffset + dependency.position().x * 4, (int) (dependency.position().y / 100f * maxHeight));
                
                dependencyLines.add(new Pair<>(position, dependencyPos));
            }
            
            parent.child(highlight);
            parent.child(background);
            parent.child(icon);
            
            if (!hasRequirements)
                parent.child(blocker);
            
            highlighters.add(highlight);
            
        }
        
    }
    
    private void triggerAugmentOperation(Identifier id, AugmentOperation operation) {
        var operationId = operation.ordinal();
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.AugmentInstallTriggerPacket(this.handler.blockPos, id, operationId));
    }
    
    public enum AugmentOperation {
        RESEARCH, ADD, REMOVE
    }
    
    private static void drawLine(DrawContext context, Vector2i from, Vector2i to, int color) {
        
        if (from.distanceSquared(to) < 0.1) return;
        
        var matrices = context.getMatrices();
        matrices.push();
        
        var pos = matrices.peek().getPositionMatrix();
        var normal = AugmentSelectionScreen.getNormalVector(from, to).normalize();
        var offset = normal.mul(1);
        var zIndex = 1.1f;
        
        var buffer = context.getVertexConsumers().getBuffer(RenderLayer.getGui());
        buffer.vertex(pos, from.x - offset.x, from.y - offset.y, zIndex).color(color);
        buffer.vertex(pos, from.x + offset.x, from.y + offset.y, zIndex).color(color);
        buffer.vertex(pos, to.x + offset.x, to.y + offset.y, zIndex).color(color);
        buffer.vertex(pos, to.x - offset.x, to.y - offset.y, zIndex).color(color);
        context.draw();
        
        matrices.pop();
    }
    
    private static class CustomFlowRootContainer extends FlowLayout {
        
        public static FlowLayout verticalFlow(Sizing horizontalSizing, Sizing verticalSizing) {
            return new CustomFlowRootContainer(horizontalSizing, verticalSizing, FlowLayout.Algorithm.VERTICAL);
        }
        
        protected CustomFlowRootContainer(Sizing horizontalSizing, Sizing verticalSizing, Algorithm algorithm) {
            super(horizontalSizing, verticalSizing, algorithm);
        }
        
        @Override
        public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
            if (main != null)
                return main.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
            return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
        }
    }
    
    private static class DraggableScrollContainer<C extends Component> extends ScrollContainer<C> {
        
        protected DraggableScrollContainer(ScrollDirection direction, Sizing horizontalSizing, Sizing verticalSizing, C child) {
            super(direction, horizontalSizing, verticalSizing, child);
        }
        
        @Override
        public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
            var inScrollBar = this.isInScrollbar(this.x + mouseX, this.y + mouseY);
            
            double delta = this.direction.choose(deltaX, deltaY);
            double selfSize = this.direction.sizeGetter.apply(this) - this.direction.insetGetter.apply(this.padding.get());
            double scalar = (this.maxScroll) / (selfSize - this.lastScrollbarLength);
            if (!Double.isFinite(scalar)) scalar = 0;
            
            scalar *= -0.5f;
            
            this.scrollBy(delta * scalar, true, false);
            
            if (inScrollBar)
                this.scrollbaring = true;
            
            return true;
        }
        
        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            
            var offset = new Vector2i((int) -this.currentScrollPosition, 0).add(this.x, this.y);
            
            for (var dependency : dependencyLines) {
                drawLine(context, new Vector2i(dependency.getLeft()).add(offset), new Vector2i(dependency.getRight()).add(offset), new Color(0.1f, 0.15f, 0.2f, 1f).argb());
            }
            
        }
    }
    
}
