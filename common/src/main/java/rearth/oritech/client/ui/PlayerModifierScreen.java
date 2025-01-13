package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.block.entity.augmenter.PlayerModifierTestEntity;
import rearth.oritech.init.recipes.AugmentRecipe;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.SizedIngredient;

import java.util.*;
import java.util.stream.Stream;

import static rearth.oritech.client.ui.BasicMachineScreen.GUI_COMPONENTS;
import static rearth.oritech.client.ui.BasicMachineScreen.getEnergyTooltip;

public class PlayerModifierScreen extends BaseOwoHandledScreen<FlowLayout, PlayerModifierScreenHandler> {
    
    private static DraggableScrollContainer<FlowLayout> main;
    private static FlowLayout root;
    private static final HashMap<String, Pair<Vector2i, Vector2i>> dependencyLines = new HashMap<>();
    private static final HashMap<Identifier, AugmentUiState> shownAugments = new HashMap<>();
    private final Set<BoxComponent> highlighters = new HashSet<>();
    private final int backgroundAugmentFrameSize = 32;
    private final int augmentIconSize = 24;
    
    private static final float panelHeight = 0.8f;
    private TextureComponent energyIndicator;
    
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
        
        root = rootComponent;
        
        dependencyLines.clear();
        
        var outerContainer = Containers.horizontalFlow(Sizing.fill(60), Sizing.fill((int) (panelHeight * 100)));
        outerContainer.surface(Surface.PANEL);
        
        var movedPanel = Containers.horizontalFlow(Sizing.fixed(800), Sizing.fill());
        movedPanel.surface(Surface.tiled(Oritech.id("textures/block/machine_plating_block/empty.png"), 16, 16));
        movedPanel.margins(Insets.of(2));
        
        var innerContainer = new DraggableScrollContainer<>(ScrollContainer.ScrollDirection.HORIZONTAL, Sizing.fill(), Sizing.fill(), movedPanel);
        innerContainer.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        innerContainer.surface(Surface.PANEL_INSET);
        innerContainer.margins(Insets.of(6));
        main = innerContainer;
        
        rootComponent.child(outerContainer.positioning(Positioning.relative(50, 50)));
        outerContainer.child(innerContainer);
        
        addAvailableAugments(movedPanel);
        
        var energyPanel = Containers.verticalFlow(Sizing.content(3), Sizing.content(3));
        energyPanel.surface(Surface.PANEL);
        energyPanel.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        
        var loadResearchedAugments = Components.button(Text.translatable("\uD83D\uDD2C"), elem -> onLoadAugmentsClick());
        loadResearchedAugments.tooltip(Text.translatable("text.oritech.load_augments.tooltip"));
        loadResearchedAugments.margins(Insets.of(2));
        
        var energyPanelX = this.width * 0.2 - 22;
        var energyPanelY = this.height * 0.3;
        
        addEnergyBar(energyPanel);
        energyPanel.child(loadResearchedAugments.horizontalSizing(Sizing.fixed(18)));
        root.child(energyPanel.positioning(Positioning.absolute((int) energyPanelX, (int) energyPanelY)).zIndex(-1));
        
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        for (var augmentId : shownAugments.keySet()) {
            var augmentState = shownAugments.get(augmentId);
            var uiData = PlayerAugments.augmentAssets.get(augmentId);
            
            var isResearched = this.handler.blockEntity.researchedAugments.contains(augmentId);
            var isApplied = this.handler.blockEntity.hasPlayerAugment(augmentId, this.handler.player);
            
            var hasRequirements = true;
            var missingRequirements = new ArrayList<Text>();
            missingRequirements.add(Text.translatable("oritech.text.augment." + augmentId.getPath()).formatted(Formatting.BOLD));
            missingRequirements.add(Text.translatable("oritech.text.missing_requirements_title"));
            for (var requirementId : uiData.requirements()) {
                if (!this.handler.blockEntity.researchedAugments.contains(requirementId)) {
                    hasRequirements = false;
                    missingRequirements.add(Text.translatable("oritech.text.augment." + requirementId.getPath()).formatted(Formatting.ITALIC, Formatting.RED));
                } else {
                    missingRequirements.add(Text.translatable("oritech.text.augment." + requirementId.getPath()).formatted(Formatting.ITALIC, Formatting.DARK_GREEN));
                }
            }
            
            var operation = AugmentOperation.RESEARCH;
            var tooltipTitleText = Text.translatable("oritech.text.augment." + augmentId.getPath()).formatted(Formatting.BOLD);
            var tooltipOperation = "oritech.text.augment_op.research";
            var tooltipDesc = Text.translatable("oritech.text.augment." + augmentId.getPath() + ".desc").formatted(Formatting.ITALIC, Formatting.GRAY);
            
            var extraTooltips = new ArrayList<Text>();
            for (int i = 1; i < 8; i++) {
                var key = "oritech.text.augment." + augmentId.getPath() + ".desc." + i;
                if (I18n.hasTranslation(key))
                    extraTooltips.add(Text.translatable(key));
            }
            
            if (isApplied) {
                operation = AugmentOperation.REMOVE;
                tooltipOperation = "oritech.text.augment_op.remove";
            } else if (isResearched) {
                operation = AugmentOperation.ADD;
                tooltipOperation = "oritech.text.augment_op.apply";
            }
            
            var lastOp = augmentState.openOp;
            if (operation != lastOp) {
                
                var collectedTooltip = new ArrayList<TooltipComponent>();
                Stream.of(tooltipTitleText, Text.literal(""), Text.translatable(tooltipOperation), Text.literal(""), tooltipDesc)
                  .map(elem -> TooltipComponent.of(elem.asOrderedText()))
                  .forEach(collectedTooltip::add);
                
                extraTooltips.stream().map(elem -> TooltipComponent.of(elem.asOrderedText())).forEach(collectedTooltip::add);
                
                var backgroundTexture = Oritech.id("textures/gui/augments/background_open.png");
                
                if (isApplied) {
                    backgroundTexture = Oritech.id("textures/gui/augments/background_installed.png");
                } else if (isResearched) {
                    backgroundTexture = Oritech.id("textures/gui/augments/background_completed.png");
                } else {
                    // collect requirements / cost
                    var recipe = (AugmentRecipe) this.handler.player.getWorld().getRecipeManager().get(augmentId).get().value();
                    var inputs = recipe.getResearchCost();
                    var time = recipe.getTime();
                    
                    collectedTooltip.add(TooltipComponent.of(Text.translatable("oritech.text.augment_research_time: %s", time).asOrderedText()));
                    var inputsComponent = new SizedIngredientTooltipComponent(inputs);
                    collectedTooltip.add(inputsComponent);
                }
                
                augmentState.icon.tooltip(collectedTooltip);
                
                var scrollPanel = (DraggableScrollContainer<?>) augmentState.parent.parent();
                var scrollOffset = (int) scrollPanel.getScrollPosition();
                
                var oldBackground = augmentState.background;
                var newBackground = Components.texture(backgroundTexture, 0, 0, 16, 16, 16, 16);
                newBackground.sizing(Sizing.fixed(backgroundAugmentFrameSize), Sizing.fixed(backgroundAugmentFrameSize));
                newBackground.positioning(Positioning.absolute(oldBackground.x() - augmentState.parent.x() - scrollOffset, oldBackground.y() - augmentState.parent.y()));
                augmentState.parent.removeChild(oldBackground);
                augmentState.parent.child(newBackground.zIndex(2));
                
                augmentState.openOp = operation;
                
            }
            
            if (!hasRequirements && augmentState.blocker == null) {
                
                var blocker = Components.box(Sizing.fixed(augmentIconSize), Sizing.fixed(augmentIconSize));
                blocker.color(new Color(0.3f, 0.4f, 0.4f, 0.8f));
                blocker.fill(true);
                blocker.positioning(Positioning.absolute(augmentState.icon.x() - augmentState.parent.x(), augmentState.icon.y() - augmentState.parent.y()));
                blocker.zIndex(4);
                
                augmentState.parent.child(blocker);
                
                augmentState.blocker = blocker;
                
            } else if (hasRequirements && augmentState.blocker != null) {
                augmentState.parent.removeChild(augmentState.blocker);
                augmentState.blocker = null;
            }
            
            // update tooltip separately always
            if (!hasRequirements)
                augmentState.blocker.tooltip(missingRequirements);
            
        }
        
        // update energy bar
        updateEnergyBar();
        
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
    
    protected void updateEnergyBar() {
        
        var capacity = handler.blockEntity.getEnergyStorageForLink().getCapacity();
        var amount = handler.blockEntity.getEnergyStorageForLink().getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity, (int) PlayerModifierTestEntity.energyUsageRate, (int) PlayerModifierTestEntity.maxEnergyTransfer);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    private void addAvailableAugments(FlowLayout parent) {
        
        var maxHeight = this.height * 0.7f;
        var leftOffset = 20;
        
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var uiData = PlayerAugments.augmentAssets.get(augmentId);
            
            var position = new Vector2i(leftOffset + uiData.position().x * 4, (int) (uiData.position().y / 100f * maxHeight));
            
            var iconTexture = Oritech.id("textures/gui/augments/exoskeleton.png");
            var backgroundTexture = Oritech.id("textures/gui/augments/background_open.png");
            
            final var augmentOpId = augmentId;
            
            var icon = Components.texture(iconTexture, 0, 0, 32, 32, 32, 32);
            icon.mouseDown().subscribe((a, b, c) -> {
                onAugmentClick(augmentOpId, shownAugments.get(augmentOpId).openOp, false);
                return true;
            });
            icon.sizing(Sizing.fixed(augmentIconSize), Sizing.fixed(augmentIconSize));
            icon.positioning(Positioning.absolute(position.x - augmentIconSize / 2, position.y - augmentIconSize / 2));
            
            var background = Components.texture(backgroundTexture, 0, 0, 16, 16, 16, 16);
            background.sizing(Sizing.fixed(backgroundAugmentFrameSize), Sizing.fixed(backgroundAugmentFrameSize));
            background.positioning(Positioning.absolute(position.x - backgroundAugmentFrameSize / 2, position.y - backgroundAugmentFrameSize / 2));
            
            var highlight = Components.box(Sizing.fixed(backgroundAugmentFrameSize + 2), Sizing.fixed(backgroundAugmentFrameSize + 2));
            highlight.color(new Color(0.7f, 0.7f, 0.7f, 1f));
            highlight.positioning(Positioning.absolute(position.x - backgroundAugmentFrameSize / 2 - 1, position.y - backgroundAugmentFrameSize / 2 - 1));
            
            for (var dependencyId : uiData.requirements()) {
                var dependency = PlayerAugments.augmentAssets.get(dependencyId);
                var dependencyPos = new Vector2i(leftOffset + dependency.position().x * 4, (int) (dependency.position().y / 100f * maxHeight));
                
                var depId = augmentId.getPath() + "_" + dependencyId.getPath();
                dependencyLines.put(depId, new Pair<>(position, dependencyPos));
            }
            
            parent.child(highlight.zIndex(1));
            parent.child(background.zIndex(2));
            parent.child(icon.zIndex(3));
            
            highlighters.add(highlight);
            
            shownAugments.put(augmentId, new AugmentUiState(highlight, background, icon, null, AugmentOperation.NEEDS_INIT, parent));
            
        }
        
    }
    
    private void onAugmentClick(Identifier id, AugmentOperation operation, boolean confirmed) {
        
        if (!confirmed) {
            showAugmentDialog(id, operation);
            return;
        }
        
        var operationId = operation.ordinal();
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.AugmentInstallTriggerPacket(this.handler.blockPos, id, operationId));
    }
    
    private void onLoadAugmentsClick() {
        NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.LoadPlayerAugmentsToMachinePacket(this.handler.blockPos));
        
        var loadedAugmentsCount = 0;
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var augment = PlayerAugments.allAugments.get(augmentId);
            var isResearched = this.handler.blockEntity.researchedAugments.contains(augmentId);
            var isInstalled = augment.isInstalled(handler.player);
            
            if (isInstalled && !isResearched) {
                loadedAugmentsCount++;
            }
        }
        
        this.handler.player.sendMessage(Text.translatable("text.oritech.loaded_augments %s", loadedAugmentsCount));
        this.close();
        
    }
    
    private void showAugmentDialog(Identifier id, AugmentOperation operation) {
        
        var researchRecipe = (AugmentRecipe) this.handler.blockEntity.getWorld().getRecipeManager().get(id).get().value();
        
        var hasResources = true;
        
        var panel = Containers.verticalFlow(Sizing.fixed(250), Sizing.content(1));
        panel.padding(Insets.of(5));
        panel.surface(Surface.PANEL);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);
        
        var descriptionPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content(3));
        descriptionPanel.surface(Surface.PANEL_INSET);
        descriptionPanel.padding(Insets.of(3, 0, 3, 3));
        descriptionPanel.margins(Insets.of(4));
        
        var overlay = Containers.overlay(panel);
        
        var titleLabel = Components.label(Text.translatable("oritech.text.augment." + id.getPath()).formatted(Formatting.BOLD, Formatting.BLACK));
        titleLabel.margins(Insets.of(3, 1, 0, 0));
        
        descriptionPanel.child(Components.label(Text.translatable("oritech.text.augment." + id.getPath() + ".desc").formatted(Formatting.ITALIC, Formatting.GRAY)));
        for (int i = 1; i < 8; i++) {
            var key = "oritech.text.augment." + id.getPath() + ".desc." + i;
            if (I18n.hasTranslation(key))
                descriptionPanel.child(Components.label(Text.translatable(key).formatted(Formatting.ITALIC, Formatting.GRAY)));
            
        }
        
        if (!operation.equals(AugmentOperation.REMOVE)) {
            descriptionPanel.child(Components.label(Text.translatable("oritech.text.time: %s", researchRecipe.getTime()).formatted(Formatting.GRAY)).margins(Insets.of(4, 0, 0, 0)));
            descriptionPanel.child(Components.label(Text.translatable("oritech.text.resource_cost").formatted(Formatting.GRAY)).margins(Insets.of(4, 0, 0, 0)));
            
            var itemContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content(1));
            var shownCost = researchRecipe.getResearchCost();
            if (operation.equals(AugmentOperation.ADD))
                shownCost = researchRecipe.getApplyCost();
            
            for (var input : shownCost) {
                var shownItem = Arrays.stream(input.ingredient().getMatchingStacks()).findFirst().get().getItem();
                var shownStack = new ItemStack(shownItem, input.count());
                
                var shown = Components.item(shownStack).showOverlay(true).setTooltipFromStack(true);
                itemContainer.child(shown.margins(Insets.of(2)));
            }
            descriptionPanel.child(itemContainer);
        }
        
        var buttonPanel = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        buttonPanel.margins(Insets.of(2, 0, 4, 4));
        buttonPanel.horizontalAlignment(HorizontalAlignment.RIGHT);
        
        var confirmKey = "text.oritech.begin_research";
        if (operation.equals(AugmentOperation.ADD)) {
            confirmKey = "text.oritech.install";
        } else if (operation.equals(AugmentOperation.REMOVE)) {
            confirmKey = "text.oritech.remove";
        }
        
        var cancelButton = Components.button(Text.translatable("text.oritech.cancel"), component -> overlay.remove());
        var confirmButton = Components.button(Text.translatable(confirmKey), component -> {
            onAugmentClick(id, operation, true);
            overlay.remove();
        });
        
        buttonPanel.child(cancelButton.margins(Insets.of(2)));
        buttonPanel.child(confirmButton.margins(Insets.of(2, 2, 2, 0)));
        
        confirmButton.active(hasResources);
        
        panel.child(titleLabel);
        panel.child(descriptionPanel);
        panel.child(buttonPanel);
        
        overlay.zIndex(100);
        root.child(overlay);
        
    }
    
    private void addEnergyBar(FlowLayout panel) {
        
        var insetSize = 1;
        var tooltipText = Text.translatable("tooltip.oritech.energy_indicator", 10, 50);
        
        var width = 17;
        var height = 80;
        
        var frame = Containers.horizontalFlow(Sizing.fixed(width + insetSize * 2), Sizing.fixed(height + insetSize * 2));
        frame.surface(Surface.PANEL_INSET);
        frame.padding(Insets.of(insetSize));
        panel.child(frame);
        
        var indicator_background = Components.texture(GUI_COMPONENTS, 24, 0, 24, 96, 98, 96);
        indicator_background.sizing(Sizing.fixed(width), Sizing.fixed(height));
        
        energyIndicator = Components.texture(GUI_COMPONENTS, 0, 0, 24, (96), 98, 96);
        energyIndicator.sizing(Sizing.fixed(width), Sizing.fixed(height));
        energyIndicator.positioning(Positioning.absolute(0, 0));
        energyIndicator.tooltip(tooltipText);
        
        frame
          .child(indicator_background)
          .child(energyIndicator);
    }
    
    public enum AugmentOperation {
        RESEARCH, ADD, REMOVE, NEEDS_INIT
    }
    
    private static final class AugmentUiState {
        private BoxComponent highlight;
        private TextureComponent background;
        private TextureComponent icon;
        private BoxComponent blocker;
        private AugmentOperation openOp;
        private final FlowLayout parent;
        
        private AugmentUiState(BoxComponent highlight, TextureComponent background, TextureComponent icon, BoxComponent blocker, AugmentOperation openOp, FlowLayout parent) {
            this.highlight = highlight;
            this.background = background;
            this.icon = icon;
            this.blocker = blocker;
            this.openOp = openOp;
            this.parent = parent;
        }
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
            
            return false;
        }
        
        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            
            var offset = new Vector2i((int) -this.currentScrollPosition, 0).add(this.x, this.y);
            
            for (var dependency : dependencyLines.values()) {
                drawLine(context, new Vector2i(dependency.getLeft()).add(offset), new Vector2i(dependency.getRight()).add(offset), new Color(0.1f, 0.15f, 0.2f, 1f).argb());
            }
            
        }
        
        public double getScrollPosition() {
            return currentScrollPosition;
        }
        
    }
    
    public static class SizedIngredientTooltipComponent implements TooltipComponent {
        
        private final List<SizedIngredient> items;
        private final int size = 16;
        private final int spacing = 3;
        
        public SizedIngredientTooltipComponent(List<SizedIngredient> items) {
            this.items = items;
        }
        
        
        @Override
        public int getHeight() {
            return size + spacing + 5;
        }
        
        @Override
        public int getWidth(TextRenderer textRenderer) {
            return (size + spacing) * items.size();
        }
        
        @Override
        public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
            context.push();
            // context.getMatrices().translate(0, spacing, 0);
            
            for (int i = 0; i < items.size(); i++) {
                var ingredient = items.get(i);
                var stack = Arrays.stream(ingredient.ingredient().getMatchingStacks()).findFirst().get();
                stack = new ItemStack(stack.getItem(), ingredient.count());
                
                var itemX = x + (size + spacing) * i;
                var itemY = y + spacing;
                
                context.drawItem(stack, itemX, itemY);
                
                if (stack.getCount() > 1) {
                    context.getMatrices().translate(0, 0, 200);
                    context.drawText(textRenderer, String.valueOf(stack.getCount()), itemX + 19 - 2 - textRenderer.getWidth(String.valueOf(stack.getCount())), itemY + 6 + 3, 16777215, true);
                }
                
            }
            
            context.pop();
        }
    }
    
}
