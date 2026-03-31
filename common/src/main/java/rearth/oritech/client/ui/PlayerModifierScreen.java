package rearth.oritech.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.screen.Insets;
import rearth.oritech.api.screen.OritechSurface;
import rearth.oritech.api.screen.UIComponent;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.api.screen.data.EnergyDisplayWidget;
import rearth.oritech.api.screen.widgets.*;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.recipes.AugmentDataRecipe;
import rearth.oritech.util.ColorHelper;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.TooltipHelper;

import java.util.*;

public class PlayerModifierScreen extends OritechWidgetScreen<PlayerModifierScreenHandler> {
    
    private static final int CONTENT_WIDTH = 900;
    private static final int NODE_SIZE = 32;
    private static final int ICON_SIZE = 24;
    private static final int PANEL_PADDING = 8;
    private static final int GRAPH_LEFT_OFFSET = 20;
    private static final int GRAPH_BG = ColorHelper.argb(0.11f, 0.13f, 0.17f, 0.95f);
    private static final int HIGHLIGHT_COLOR = ColorHelper.argb(0.7f, 0.7f, 0.7f, 0.9f);
    private static final int BLOCKER_COLOR = ColorHelper.argb(0.3f, 0.4f, 0.4f, 0.78f);
    private static final int LINE_COLOR = ColorHelper.argb(0.1f, 0.15f, 0.2f, 1f);
    
    private final Map<ResourceLocation, AugmentNodeWidget> augmentNodes = new LinkedHashMap<>();
    private final List<DependencyLine> dependencyLines = new ArrayList<>();
    private final List<LabelWidget> researchLabels = new ArrayList<>();
    
    private OverlayWidget dialogOverlay;
    
    public PlayerModifierScreen(PlayerModifierScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title, 0, 0);
    }
    
    @Override
    protected void buildComponents() {
        setPanelSize(width, height);
        augmentNodes.clear();
        dependencyLines.clear();
        researchLabels.clear();
        dialogOverlay = null;
        
        if (menu.blockEntity == null) {
            onClose();
            return;
        }
        
        int mainWidth = Math.min(width - 240, 600);
        int mainHeight = 260;
        
        int mainX = (width - mainWidth) / 2;
        int mainY = (height - mainHeight) / 2;
        
        int researchWidth = 130;
        int researchX = mainX + mainWidth;
        int researchY = mainY + 8;
        
        int energyWidth = 30;
        int energyX = mainX - energyWidth + 1;
        int energyY = mainY + 24;
        
        var mainPanel = new SurfaceWidget(mainX, mainY, mainWidth, mainHeight);
        mainPanel.withSurface(OritechSurface.PANEL);
        mainPanel.withZIndex(-12);
        addComponent(mainPanel);
        
        var graphInset = new SurfaceWidget(mainX + PANEL_PADDING, mainY + PANEL_PADDING,
          mainWidth - PANEL_PADDING * 2, mainHeight - PANEL_PADDING * 2);
        graphInset.withSurface(OritechSurface.PANEL_INSET);
        graphInset.withZIndex(-11);
        addComponent(graphInset);
        
        var graphBackdrop = BoxWidget.filled(mainX + PANEL_PADDING + 3, mainY + PANEL_PADDING + 3,
          mainWidth - (PANEL_PADDING + 3) * 2, mainHeight - (PANEL_PADDING + 3) * 2, GRAPH_BG);
        graphBackdrop.withZIndex(-10);
        addComponent(graphBackdrop);
        
        buildAugmentCanvas(mainX, mainY, mainWidth, mainHeight);
        buildResearchPanels(researchX, researchY, researchWidth);
        buildEnergyPanel(energyX, energyY);
    }
    
    private void buildAugmentCanvas(int mainX, int mainY, int mainWidth, int mainHeight) {
        int viewportX = mainX + PANEL_PADDING;
        int viewportY = mainY + PANEL_PADDING;
        int viewportWidth = mainWidth - PANEL_PADDING * 2;
        int viewportHeight = mainHeight - PANEL_PADDING * 2;
        int graphHeight = Math.max(140, viewportHeight - 10);
        
        var augmentCanvas = new ScrollWidget(viewportX, viewportY, viewportWidth, viewportHeight)
                              .withHorizontalScroll(true)
                              .withVerticalScroll(false)
                              .withScrollSpeed(16)
                              .withDragScrolling(true);
        augmentCanvas.setSurface(OritechSurface.PANEL_INSET);
        addComponent(augmentCanvas);
        
        int maxY = 0;
        var lineWidget = new DependencyLineWidget(0, 0, CONTENT_WIDTH, graphHeight + NODE_SIZE + 16);
        lineWidget.withZIndex(0);
        augmentCanvas.addChild(lineWidget);
        
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var recipeEntry = menu.player.level().getRecipeManager().byKey(augmentId);
            if (recipeEntry.isEmpty() || !(recipeEntry.get().value() instanceof AugmentDataRecipe recipe)) continue;
            
            int x = GRAPH_LEFT_OFFSET + recipe.getUiX() * 4 - NODE_SIZE / 2;
            int y = (int) (recipe.getUiY() / 100f * graphHeight) - NODE_SIZE / 2;
            maxY = Math.max(maxY, y + NODE_SIZE + 12);
            
            var node = new AugmentNodeWidget(x, y, augmentId);
            node.withZIndex(10);
            augmentNodes.put(augmentId, node);
            augmentCanvas.addChild(node);
        }
        
        for (var augmentId : augmentNodes.keySet()) {
            var recipeEntry = menu.player.level().getRecipeManager().byKey(augmentId);
            if (recipeEntry.isEmpty() || !(recipeEntry.get().value() instanceof AugmentDataRecipe recipe)) continue;
            
            var fromNode = augmentNodes.get(augmentId);
            for (var dependencyId : recipe.getRequirements()) {
                var dependencyNode = augmentNodes.get(dependencyId);
                if (dependencyNode == null) continue;
                dependencyLines.add(new DependencyLine(fromNode.centerX(), fromNode.centerY(), dependencyNode.centerX(), dependencyNode.centerY()));
            }
        }
        
        augmentCanvas.setContentDimensions(CONTENT_WIDTH, Math.max(graphHeight, maxY));
    }
    
    private void buildResearchPanels(int x, int y, int width) {
        for (int i = 0; i < 3; i++) researchLabels.add(null);
        
        int offsetY = y;
        for (int index = 0; index < 3; index++) {
            var researchState = menu.blockEntity.availableStations.getOrDefault(index, null);
            if (researchState == null) continue;
            
            int panelHeight = 52;
            var panel = new SurfaceWidget(x, offsetY, width, panelHeight);
            panel.withSurface(OritechSurface.PANEL);
            panel.withZIndex(-8);
            addComponent(panel);
            
            var title = new LabelWidget(x + 6, offsetY + 7, width - 12, 10,
              researchState.type.getName().copy().withStyle(ChatFormatting.BOLD));
            title.withAlignment(LabelWidget.Alignment.CENTER);
            title.withDarkColor();
            title.withWrap(true);
            addComponent(title);
            
            var status = new LabelWidget(x + 6, offsetY + 30, width - 12, 18, Component.literal(" "));
            status.withAlignment(LabelWidget.Alignment.CENTER);
            status.withDarkColor();
            status.withWrap(true);
            addComponent(status);
            
            researchLabels.set(index, status);
            offsetY += panelHeight + 10;
        }
    }
    
    private void buildEnergyPanel(int x, int y) {
        int panelWidth = 30;
        int panelHeight = 140;
        
        var panel = new SurfaceWidget(x, y, panelWidth, panelHeight);
        panel.withSurface(OritechSurface.PANEL);
        panel.withZIndex(-30);
        addComponent(panel);
        
        addComponent(new EnergyDisplayWidget(DisplayDataSource.CreateEnergy(
          this.menu.blockEntity.getEnergyStorage(null),
          new ScreenProvider.BarConfiguration(x + 7, y + 8, 17, 80),
          this.menu.blockEntity)).withSurface(OritechSurface.PANEL_INSET).withPadding(Insets.of(1)));
        
        var loadButton = ButtonWidget.darkPanel(x + 6, y + 93, 19, 18,
          Component.literal("\uD83D\uDD2C"), btn -> onLoadAugmentsClick()).withTextColor(LabelWidget.BRIGHT_TEXT);
        loadButton.withTooltip(Component.translatable("text.oritech.load_augments.tooltip"));
        addComponent(loadButton);
        
        var invButton = ButtonWidget.darkPanel(x + 6, y + 93 + 18 + 3, 19, 18,
          Component.literal("\uD83E\uDDF0"), btn -> onOpenInvClicked()).withTextColor(LabelWidget.BRIGHT_TEXT);
        invButton.withTooltip(Component.translatable("text.oritech.open_inv.tooltip"));
        addComponent(invButton);
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        
        for (var component : components) {
            component.tick();
        }
        
        if (menu.blockEntity == null) return;
        
        updateResearchPanels();
        updateAugmentNodes();
    }
    
    private void updateResearchPanels() {
        for (int i = 0; i < 3; i++) {
            if (i >= researchLabels.size()) continue;
            var panelData = researchLabels.get(i);
            var researchData = this.menu.blockEntity.availableStations.get(i);
            if (researchData == null || panelData == null) continue;
            
            Component text;
            var time = this.menu.blockEntity.getLevel().getGameTime();
            if (!researchData.working) {
                var ticks = time % 20 / 7;
                text = Component.translatable("text.oritech.accelerator.ui.waiting." + ticks);
            } else {
                var remainingTicks = researchData.researchStartedAt + researchData.workTime - time;
                var remainingSeconds = (int) (remainingTicks / 20f);
                text = Component.translatable("text.oritech.augmenter_active", remainingSeconds);
            }
            
            panelData.setText(text);
        }
    }
    
    private void updateAugmentNodes() {
        for (var entry : augmentNodes.entrySet()) {
            var augmentId = entry.getKey();
            var node = entry.getValue();
            var recipeEntry = this.menu.player.level().getRecipeManager().byKey(augmentId);
            if (recipeEntry.isEmpty() || !(recipeEntry.get().value() instanceof AugmentDataRecipe augmentRecipe))
                continue;
            
            var isResearched = this.menu.blockEntity.researchedAugments.contains(augmentId);
            var isResearching = this.menu.blockEntity.availableStations.values().stream()
                                  .filter(Objects::nonNull)
                                  .anyMatch(station -> station.selectedResearch.equals(augmentId));
            var isApplied = this.menu.blockEntity.hasPlayerAugment(augmentId, this.menu.player);
            
            var operation = resolveOperation(isApplied, isResearched, isResearching);
            
            var missingRequirements = new ArrayList<Component>();
            var hasRequirements = true;
            
            for (var requirementId : augmentRecipe.getRequirements()) {
                if (!this.menu.blockEntity.researchedAugments.contains(requirementId)) {
                    hasRequirements = false;
                    missingRequirements.add(Component.translatable(augmentKey(requirementId)).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED));
                }
            }
            
            var requiredStationBlock = BuiltInRegistries.BLOCK.get(augmentRecipe.getRequiredStation());
            var hasResearchStation = this.menu.blockEntity.availableStations.values().stream()
                                       .filter(Objects::nonNull)
                                       .anyMatch(station -> station.type.equals(requiredStationBlock));
            
            if (!hasResearchStation) {
                hasRequirements = false;
                missingRequirements.add(Component.translatable("oritech.text.required_station", requiredStationBlock.getName()).withStyle(ChatFormatting.RED));
            }
            
            node.setOperation(operation);
            node.setBlocked(operation == PlayerAugments.AugmentApplicatorOperation.RESEARCH && !hasRequirements);
            node.setTooltip(buildNodeTooltip(augmentId, augmentRecipe, operation, missingRequirements));
        }
    }
    
    private List<Component> buildNodeTooltip(ResourceLocation augmentId, AugmentDataRecipe recipe,
                                             PlayerAugments.AugmentApplicatorOperation operation,
                                             List<Component> missingRequirements) {
        var tooltip = new ArrayList<Component>();
        tooltip.add(Component.translatable(operationKey(operation))
                      .append(Component.literal(" "))
                      .append(Component.translatable(augmentKey(augmentId)).withStyle(ChatFormatting.BOLD)));
        
        tooltip.add(Component.translatable(augmentKey(augmentId) + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (int i = 1; i < 8; i++) {
            var key = augmentKey(augmentId) + ".desc." + i;
            if (I18n.exists(key)) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        
        if (operation == PlayerAugments.AugmentApplicatorOperation.RESEARCH) {
            tooltip.add(Component.translatable("oritech.text.augment_research_time", recipe.getTime() / 20));
            tooltip.add(Component.translatable("oritech.text.energy_cost", TooltipHelper.getEnergyText(recipe.getRfCost())));
        }
        
        if (!missingRequirements.isEmpty()) {
            tooltip.add(Component.translatable("oritech.text.missing_requirements_title").withStyle(ChatFormatting.BOLD, ChatFormatting.RED));
            tooltip.addAll(missingRequirements);
        }
        
        return tooltip;
    }
    
    private PlayerAugments.AugmentApplicatorOperation resolveOperation(boolean isApplied, boolean isResearched, boolean isResearching) {
        if (isApplied) return PlayerAugments.AugmentApplicatorOperation.REMOVE;
        if (isResearched) return PlayerAugments.AugmentApplicatorOperation.ADD;
        if (isResearching) return PlayerAugments.AugmentApplicatorOperation.NONE;
        return PlayerAugments.AugmentApplicatorOperation.RESEARCH;
    }
    
    private void onAugmentClick(ResourceLocation id, PlayerAugments.AugmentApplicatorOperation operation, boolean confirmed) {
        if (!confirmed) {
            showAugmentDialog(id, operation);
            return;
        }
        
        NetworkManager.sendToServer(new PlayerAugments.AugmentInstallTriggerPacket(this.menu.blockPos, id, operation.ordinal()));
    }
    
    private void onLoadAugmentsClick() {
        NetworkManager.sendToServer(new PlayerAugments.LoadPlayerAugmentsToMachinePacket(this.menu.blockPos));
        
        int loadedAugmentsCount = 0;
        for (var entry : PlayerAugments.allAugments.entrySet()) {
            var augment = entry.getValue();
            var isResearched = this.menu.blockEntity.researchedAugments.contains(entry.getKey());
            var isInstalled = augment.isInstalled(menu.player);
            
            if (isInstalled && !isResearched) {
                loadedAugmentsCount++;
            }
        }
        
        this.menu.player.sendSystemMessage(Component.translatable("text.oritech.loaded_augments", loadedAugmentsCount));
        this.onClose();
    }
    
    private void onOpenInvClicked() {
        this.onClose();
        NetworkManager.sendToServer(new PlayerAugments.OpenAugmentScreenPacket(this.menu.blockPos));
    }
    
    private void showAugmentDialog(ResourceLocation id, PlayerAugments.AugmentApplicatorOperation operation) {
        removeDialogOverlay();
        
        var recipeEntry = this.menu.blockEntity.getLevel().getRecipeManager().byKey(id);
        if (recipeEntry.isEmpty() || !(recipeEntry.get().value() instanceof AugmentDataRecipe recipe)) return;
        
        boolean isCreative = this.menu.player.isCreative();
        boolean hasResources = true;
        boolean hasEnergy = true;
        
        var requiredStationBlock = BuiltInRegistries.BLOCK.get(recipe.getRequiredStation());
        boolean hasRequiredStation = false;
        for (int i = 0; i < 3; i++) {
            var station = this.menu.blockEntity.availableStations.getOrDefault(i, null);
            if (station == null || station.working) continue;
            if (requiredStationBlock.equals(station.type)) {
                hasRequiredStation = true;
                break;
            }
        }
        
        var shownCost = operation == PlayerAugments.AugmentApplicatorOperation.ADD
                          ? recipe.getApplyCost()
                          : recipe.getResearchCost();
        
        if (operation != PlayerAugments.AugmentApplicatorOperation.REMOVE) {
            for (var wantedInput : shownCost) {
                var type = wantedInput.ingredient();
                var count = wantedInput.count();
                var machineMatching = this.menu.blockEntity.inventory.heldStacks.stream().filter(type).mapToInt(ItemStack::getCount).sum();
                var playerMatching = this.menu.player.getInventory().items.stream().filter(type).mapToInt(ItemStack::getCount).sum();
                if (machineMatching + playerMatching < count) {
                    hasResources = false;
                    break;
                }
            }
        }
        
        if (operation == PlayerAugments.AugmentApplicatorOperation.RESEARCH) {
            hasEnergy = this.menu.blockEntity.getEnergyStorageForMultiblock(null).getAmount() >= recipe.getRfCost();
        }
        
        var overlay = new OverlayWidget(width, height).withDismissHandler(this::removeDialogOverlay);
        int panelWidth = 320;
        int contentWidth = panelWidth - 24;
        int panelX = (width - panelWidth) / 2;
        int panelY = (height - 210) / 2;
        int cursorY = panelY + 10;
        
        var dialogChildren = new ArrayList<UIComponent>();
        
        var title = new LabelWidget(panelX + 12, cursorY, contentWidth, 10,
          Component.translatable(augmentKey(id)).withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK));
        title.withAlignment(LabelWidget.Alignment.CENTER);
        dialogChildren.add(title);
        cursorY += 20;
        
        cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
          Component.translatable(augmentKey(id) + ".desc").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), LabelWidget.Alignment.LEFT);
        for (int i = 1; i < 8; i++) {
            var key = augmentKey(id) + ".desc." + i;
            if (I18n.exists(key)) {
                cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
                  Component.translatable(key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), LabelWidget.Alignment.LEFT);
            }
        }
        
        cursorY += 4;
        cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
          Component.translatable("oritech.text.required_station", requiredStationBlock.getName()), LabelWidget.Alignment.LEFT);
        
        if (operation == PlayerAugments.AugmentApplicatorOperation.RESEARCH) {
            cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
              Component.translatable("oritech.text.augment_research_time", recipe.getTime() / 20), LabelWidget.Alignment.LEFT);
            cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
              Component.translatable("oritech.text.energy_cost", TooltipHelper.getEnergyText(recipe.getRfCost())), LabelWidget.Alignment.LEFT);
        }
        
        if (operation != PlayerAugments.AugmentApplicatorOperation.REMOVE) {
            cursorY += 4;
            cursorY = addWrappedLabel(dialogChildren, panelX + 12, cursorY, contentWidth,
              Component.translatable("oritech.text.augment_resource_cost"), LabelWidget.Alignment.LEFT);
            
            int itemX = panelX + 12;
            int itemY = cursorY;
            for (var input : shownCost) {
                if (input.ingredient().getItems().length == 0) continue;
                
                var shownStack = Arrays.stream(input.ingredient().getItems()).findFirst().orElse(ItemStack.EMPTY).copy();
                shownStack.setCount(input.count());
                
                var itemWidget = new ItemWidget(itemX, itemY, shownStack).withTooltipFromStack(false);
                var tooltip = new ArrayList<Component>();
                tooltip.add(Component.translatable("oritech.text.augment_ingredient_tip").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));
                Arrays.stream(input.ingredient().getItems())
                  .map(ItemStack::getHoverName)
                  .forEach(tooltip::add);
                itemWidget.withTooltip(tooltip);
                dialogChildren.add(itemWidget);
                itemX += 20;
            }
            cursorY += 24;
        }
        
        boolean confirmAllowed = switch (operation) {
            case NONE -> false;
            case RESEARCH -> hasRequiredStation && hasResources && hasEnergy;
            case ADD -> hasResources;
            case REMOVE -> true;
            default -> true;
        };
        
        var confirmKey = switch (operation) {
            case ADD -> "text.oritech.install";
            case REMOVE -> "text.oritech.remove";
            case NONE -> "text.oritech.noop";
            default -> "text.oritech.begin_research";
        };
        
        if ((!hasResources || !hasEnergy) && isCreative) {
            confirmAllowed = operation != PlayerAugments.AugmentApplicatorOperation.NONE && (operation != PlayerAugments.AugmentApplicatorOperation.RESEARCH || hasRequiredStation);
        }
        
        int buttonY = cursorY + 8;
        int panelHeight = buttonY - panelY + 30;
        
        var panelBackground = new SurfaceWidget(panelX, panelY, panelWidth, panelHeight, OritechSurface.PANEL);
        panelBackground.withZIndex(-10);
        overlay.addChild(panelBackground);
        
        var panelInset = new SurfaceWidget(panelX + 5, panelY + 25, panelWidth - 10, cursorY - panelY - 23, OritechSurface.PANEL_INSET);
        panelInset.withZIndex(-10);
        overlay.addChild(panelInset);
        
        
        for (var child : dialogChildren) overlay.addChild(child);
        
        var confirmLabel = (!hasResources || !hasEnergy) && isCreative && operation != PlayerAugments.AugmentApplicatorOperation.NONE
                             ? Component.literal("[C] ").withColor(ColorHelper.argb(214 / 255f, 26 / 255f, 173 / 255f)).append(Component.translatable(confirmKey).withColor(ColorHelper.WHITE))
                             : Component.translatable(confirmKey);
        
        var confirmWidth = LabelWidget.getTextWidth(confirmLabel) + 4;
        var cancelWidth = LabelWidget.getTextWidth(Component.translatable("text.oritech.cancel")) + 4;
        
        var cancelButton = ButtonWidget.darkPanel(panelX + panelWidth - confirmWidth - 8 - cancelWidth - 12, buttonY, cancelWidth, 16,
            Component.translatable("text.oritech.cancel"), btn -> removeDialogOverlay())
                             .withTextColor(LabelWidget.BRIGHT_TEXT)
                             .withSurfacePadding(Insets.of(3));
        overlay.addChild(cancelButton);
        
        var confirmButton = ButtonWidget.darkPanel(panelX + panelWidth - confirmWidth - 8, buttonY, confirmWidth, 16,
          confirmLabel, btn -> {
              onAugmentClick(id, operation, true);
              removeDialogOverlay();
          }).withTextColor(LabelWidget.BRIGHT_TEXT).withSurfacePadding(Insets.of(3));
        
        confirmButton.setActive(confirmAllowed);
        overlay.addChild(confirmButton);
        
        dialogOverlay = overlay;
        addComponent(overlay);
    }
    
    private int addWrappedLabel(List<UIComponent> children, int x, int y, int width, Component text, LabelWidget.Alignment alignment) {
        int height = Math.max(10, Minecraft.getInstance().font.split(text, width).size() * Minecraft.getInstance().font.lineHeight);
        var label = new LabelWidget(x, y, width, height, text);
        label.withWrap(true);
        label.withAlignment(alignment);
        label.withBrightColor();
        children.add(label);
        return y + height + 4;
    }
    
    private void removeDialogOverlay() {
        if (dialogOverlay != null) {
            removeComponent(dialogOverlay);
            dialogOverlay = null;
        }
    }
    
    private static String operationKey(PlayerAugments.AugmentApplicatorOperation operation) {
        return switch (operation) {
            case ADD -> "oritech.text.augment_op.apply";
            case REMOVE -> "oritech.text.augment_op.remove";
            case NONE -> "oritech.text.augment_op.pending";
            default -> "oritech.text.augment_op.research";
        };
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
    
    @Override
    public boolean shouldCreateTitle() {
        return false;
    }
    
    @Override
    public BlockState getTitleState() {
        return menu.blockEntity != null ? menu.blockEntity.getBlockState() : BlockContent.AUGMENT_APPLICATION_BLOCK.defaultBlockState();
    }
    
    public static String augmentKey(ResourceLocation id) {
        return "oritech.text." + id.getPath().replace('/', '.');
    }
    
    private record DependencyLine(int fromX, int fromY, int toX, int toY) {
    }
    
    private final class DependencyLineWidget extends UIComponent {
        
        private DependencyLineWidget(int x, int y, int width, int height) {
            super(x, y, width, height);
        }
        
        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            for (var dependency : dependencyLines) {
                drawLine(graphics, dependency.fromX(), dependency.fromY(), dependency.toX(), dependency.toY(), LINE_COLOR, 0f);
            }
        }
    }
    
    private final class AugmentNodeWidget extends UIComponent {
        
        private final ResourceLocation augmentId;
        private final ResourceLocation iconTexture;
        private PlayerAugments.AugmentApplicatorOperation operation = PlayerAugments.AugmentApplicatorOperation.NEEDS_INIT;
        private boolean blocked;
        
        private AugmentNodeWidget(int x, int y, ResourceLocation augmentId) {
            super(x, y, NODE_SIZE, NODE_SIZE);
            this.augmentId = augmentId;
            this.iconTexture = Oritech.id("textures/gui/" + augmentId.getPath() + ".png");
        }
        
        private void setOperation(PlayerAugments.AugmentApplicatorOperation operation) {
            this.operation = operation;
        }
        
        private PlayerAugments.AugmentApplicatorOperation getOperation() {
            return operation;
        }
        
        private void setBlocked(boolean blocked) {
            this.blocked = blocked;
        }
        
        private int centerX() {
            return x + width / 2;
        }
        
        private int centerY() {
            return y + height / 2;
        }
        
        @Override
        protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean hovered = isMouseOver(mouseX, mouseY);
            if (hovered) {
                graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, HIGHLIGHT_COLOR);
            }
            
            var backgroundTexture = switch (operation) {
                case ADD -> Oritech.id("textures/gui/augment/background_completed.png");
                case REMOVE -> Oritech.id("textures/gui/augment/background_installed.png");
                case NONE -> Oritech.id("textures/gui/augment/background_pending.png");
                default -> Oritech.id("textures/gui/augment/background_open.png");
            };
            
            graphics.blit(backgroundTexture, x, y, 0, 0, width, height, 16, 16);
            
            int iconOffset = (width - ICON_SIZE) / 2;
            graphics.blit(iconTexture, x + iconOffset, y + iconOffset, 0, 0, ICON_SIZE, ICON_SIZE, 24, 24);
            
            if (blocked) {
                graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, BLOCKER_COLOR);
            }
        }
        
        @Override
        public boolean handleClick(double mouseX, double mouseY, int button) {
            if (button != 0) return false;
            if (!blocked) {
                onAugmentClick(augmentId, getOperation(), false);
            }
            return true;
        }
    }
}