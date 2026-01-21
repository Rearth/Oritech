package rearth.oritech.client.ui;

import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextureComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.AugmentApplicationEntity;
import rearth.oritech.block.entity.augmenter.PlayerAugments;
import rearth.oritech.init.recipes.AugmentDataRecipe;
import rearth.oritech.util.SizedIngredient;
import rearth.oritech.util.TooltipHelper;

import java.util.*;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import static rearth.oritech.client.ui.BasicMachineScreen.*;

public class PlayerModifierScreen extends BaseOwoHandledScreen<FlowLayout, PlayerModifierScreenHandler> {
    
    private static DraggableScrollContainer<FlowLayout> main;
    private static FlowLayout root;
    private static final HashMap<String, Tuple<Vector2i, Vector2i>> dependencyLines = new HashMap<>();
    private static final HashMap<ResourceLocation, AugmentUiState> shownAugments = new HashMap<>();
    private final Set<BoxComponent> highlighters = new HashSet<>();
    private final List<LabelComponent> researchLabels = new ArrayList<>();
    private final int backgroundAugmentFrameSize = 32;
    private final int augmentIconSize = 24;
    
    private static final float panelHeight = 0.8f;
    private TextureComponent energyIndicator;
    
    public PlayerModifierScreen(PlayerModifierScreenHandler handler, Inventory inventory, Component title) {
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
        
        if (menu.blockEntity == null) {
            this.onClose();
            return;
        }
        
        dependencyLines.clear();
        shownAugments.clear();
        
        var outerContainer = Containers.horizontalFlow(Sizing.fill(60), Sizing.fill((int) (panelHeight * 100)));
        outerContainer.surface(ORITECH_PANEL);
        
        var movedPanel = Containers.horizontalFlow(Sizing.fixed(900), Sizing.fill());
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
        
        var researchWidth = 120;
        var researchContainer = Containers.verticalFlow(Sizing.fixed(researchWidth), Sizing.content());
        researchContainer.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        
        addResearchPanels(researchContainer, researchWidth);
        
        var energyPanel = Containers.verticalFlow(Sizing.content(3), Sizing.content(3));
        energyPanel.surface(ORITECH_PANEL);
        energyPanel.alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        
        var loadResearchedAugments = Components.button(Component.translatable("\uD83D\uDD2C"), elem -> onLoadAugmentsClick());
        loadResearchedAugments.tooltip(Component.translatable("text.oritech.load_augments.tooltip"));
        loadResearchedAugments.margins(Insets.of(2));
        loadResearchedAugments.renderer(ORITECH_BUTTON);
        
        var openInvScreen = Components.button(Component.translatable("\uD83E\uDDF0"), elem -> onOpenInvClicked());
        openInvScreen.tooltip(Component.translatable("text.oritech.open_inv.tooltip"));
        openInvScreen.margins(Insets.of(2));
        openInvScreen.renderer(ORITECH_BUTTON);
        
        var energyPanelX = this.width * 0.2 - 22;
        var energyPanelY = this.height * 0.3;
        
        var researchPanelX = this.width * 0.8 - 2;
        var researchPanelY = this.height * 0.2;
        
        addEnergyBar(energyPanel);
        energyPanel.child(loadResearchedAugments.horizontalSizing(Sizing.fixed(18)));
        energyPanel.child(openInvScreen.horizontalSizing(Sizing.fixed(18)));
        
        root.child(energyPanel.positioning(Positioning.absolute((int) energyPanelX, (int) energyPanelY)).zIndex(-1));
        root.child(researchContainer.positioning(Positioning.absolute((int) researchPanelX, (int) researchPanelY)).zIndex(-1));
        
    }
    
    @Override
    protected void containerTick() {
        super.containerTick();
        if (menu.blockEntity == null) return;
        
        // update research panels
        for (int i = 0; i < 3; i++) {
            if (i >= researchLabels.size()) continue;
            var panelData = researchLabels.get(i);
            var researchData = this.menu.blockEntity.availableStations.get(i);
            if (researchData == null || panelData == null) continue;
            
            var baseKey = Component.literal("");
            var time = this.menu.blockEntity.getLevel().getGameTime();
            
            var isIdle = !researchData.working;
            if (isIdle) {
                var ticks = time % 20 / 7;
                baseKey = Component.translatable("text.oritech.accelerator.ui.waiting." + ticks);
            } else {
                var remainingTicks = researchData.researchStartedAt + researchData.workTime - time;
                var remainingSeconds = (int) (remainingTicks / 20f);
                baseKey = Component.translatable("text.oritech.augmenter_active", remainingSeconds);
            }
            
            panelData.text(baseKey);
            
        }
        
        
        for (var entry : shownAugments.entrySet()) {
            var augmentId = entry.getKey();
            var augmentState = entry.getValue();
            var augmentRecipe = (AugmentDataRecipe) this.menu.player.level().getRecipeManager().byKey(augmentId).get().value();
            
            var isResearched = this.menu.blockEntity.researchedAugments.contains(augmentId);
            var isResearching = this.menu.blockEntity.availableStations.values().stream().filter(Objects::nonNull).anyMatch(station -> station.selectedResearch.equals(augmentId));
            var isApplied = this.menu.blockEntity.hasPlayerAugment(augmentId, this.menu.player);
            
            var hasRequirements = true;
            var missingRequirements = new ArrayList<Component>();
            missingRequirements.add(Component.translatable(augmentKey(augmentId).formatted(ChatFormatting.BOLD)));
            missingRequirements.add(Component.translatable("oritech.text.missing_requirements_title"));
            for (var requirementId : augmentRecipe.getRequirements()) {
                if (!this.menu.blockEntity.researchedAugments.contains(requirementId)) {
                    hasRequirements = false;
                    missingRequirements.add(Component.translatable(augmentKey(requirementId)).withStyle(ChatFormatting.ITALIC, ChatFormatting.RED));
                } else {
                    missingRequirements.add(Component.translatable(augmentKey(requirementId)).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_GREEN));
                }
            }
            
            var hasResearchStation = false;
            var requiredStationBlock = BuiltInRegistries.BLOCK.get(augmentRecipe.getRequiredStation());
            
            for (var ownStation : this.menu.blockEntity.availableStations.values()) {
                if (ownStation == null) continue;
                if (ownStation.type.equals(requiredStationBlock)) {
                    hasResearchStation = true;
                    break;
                }
            }
            
            if (!hasResearchStation) {
                missingRequirements.add(Component.translatable("oritech.text.required_station", requiredStationBlock.getName()));
                hasRequirements = false;
            }
            
            var operation = PlayerAugments.AugmentApplicatorOperation.RESEARCH;
            var tooltipTitleText = Component.translatable(augmentKey(augmentId)).withStyle(ChatFormatting.BOLD);
            var tooltipOperation = "oritech.text.augment_op.research";
            var tooltipDesc = Component.translatable(augmentKey(augmentId) + ".desc").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
            
            var extraTooltips = new ArrayList<Component>();
            for (int i = 1; i < 8; i++) {
                var key = augmentKey(augmentId) + ".desc." + i;
                if (I18n.exists(key))
                    extraTooltips.add(Component.translatable(key).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
            
            if (isApplied) {
                operation = PlayerAugments.AugmentApplicatorOperation.REMOVE;
                tooltipOperation = "oritech.text.augment_op.remove";
            } else if (isResearched) {
                operation = PlayerAugments.AugmentApplicatorOperation.ADD;
                tooltipOperation = "oritech.text.augment_op.apply";
            } else if (isResearching) {
                operation = PlayerAugments.AugmentApplicatorOperation.NONE;
                tooltipOperation = "oritech.text.augment_op.pending";
            }
            
            tooltipTitleText = Component.translatable(tooltipOperation).append(tooltipTitleText);
            
            var lastOp = augmentState.openOp;
            if (operation != lastOp) {
                
                var collectedTooltip = new ArrayList<ClientTooltipComponent>();
                Stream.of(tooltipTitleText, tooltipDesc)
                  .map(elem -> ClientTooltipComponent.create(elem.getVisualOrderText()))
                  .forEach(collectedTooltip::add);
                
                extraTooltips.stream().map(elem -> ClientTooltipComponent.create(elem.getVisualOrderText())).forEach(collectedTooltip::add);
                
                var backgroundTexture = Oritech.id("textures/gui/augment/background_open.png");
                
                if (isApplied) {
                    backgroundTexture = Oritech.id("textures/gui/augment/background_installed.png");
                } else if (isResearched) {
                    backgroundTexture = Oritech.id("textures/gui/augment/background_completed.png");
                } else if (isResearching) {
                    backgroundTexture = Oritech.id("textures/gui/augment/background_pending.png");
                } else {
                    // collect requirements / cost
                    var inputs = augmentRecipe.getResearchCost();
                    var time = augmentRecipe.getTime() / 20;
                    
                    collectedTooltip.add(ClientTooltipComponent.create(Component.translatable("oritech.text.augment_research_time", time).getVisualOrderText()));
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
                blocker.mouseDown().subscribe((a, b, c) -> true);
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
    public void render(GuiGraphics vanillaContext, int mouseX, int mouseY, float delta) {
        
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
        
        var capacity = menu.blockEntity.getEnergyStorageForMultiblock(null).getCapacity();
        var amount = menu.blockEntity.getEnergyStorageForMultiblock(null).getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity, 0, (int) AugmentApplicationEntity.maxEnergyTransfer);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    private void addResearchPanels(FlowLayout parent, int width) {
        
        for (int i = 0; i < 3; i++) {
            var researchState = this.menu.blockEntity.availableStations.getOrDefault(i, null);
            if (researchState == null) {
                researchLabels.add(null);
                continue;
            }
            
            var panel = Containers.verticalFlow(Sizing.fixed(width), Sizing.fixed((int) (width * 0.4)));
            var title = Components.label(researchState.type.getName().withStyle(ChatFormatting.BOLD));
            title.horizontalSizing(Sizing.fill());
            title.horizontalTextAlignment(HorizontalAlignment.CENTER);
            var status = Components.label(Component.literal(" "));
            
            panel.child(title);
            panel.child(status.margins(Insets.of(4, 2, 0, 0)));
            
            researchLabels.add(status);
            
            parent.child(panel.surface(ORITECH_PANEL).padding(Insets.of(6)).margins(Insets.of(0, 10, 0, 0)).zIndex(-1));
        }
        
    }
    
    private void addAvailableAugments(FlowLayout parent) {
        
        var maxHeight = this.height * 0.75f;
        var leftOffset = 20;
        
        for (var augmentId : PlayerAugments.allAugments.keySet()) {
            var augmentRecipe = (AugmentDataRecipe) this.menu.player.level().getRecipeManager().byKey(augmentId).get().value();
            
            var position = new Vector2i(leftOffset + augmentRecipe.getUiX() * 4, (int) (augmentRecipe.getUiY() / 100f * maxHeight));
            
            var iconTexture = Oritech.id("textures/gui/" + augmentId.getPath() + ".png");
            var backgroundTexture = Oritech.id("textures/gui/augment/background_open.png");
            
            final var augmentOpId = augmentId;
            
            var icon = Components.texture(iconTexture, 0, 0, 24, 24, 24, 24);
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
            
            for (var dependencyId : augmentRecipe.getRequirements()) {
                var dependencyRecipe = (AugmentDataRecipe) this.menu.player.level().getRecipeManager().byKey(dependencyId).get().value();
                var dependencyPos = new Vector2i(leftOffset + dependencyRecipe.getUiX() * 4, (int) (dependencyRecipe.getUiY() / 100f * maxHeight));
                
                var depId = augmentId.getPath() + "_" + dependencyId.getPath();
                dependencyLines.put(depId, new Tuple<>(position, dependencyPos));
            }
            
            parent.child(highlight.zIndex(1));
            parent.child(background.zIndex(2));
            parent.child(icon.zIndex(3));
            
            highlighters.add(highlight);
            
            shownAugments.put(augmentId, new AugmentUiState(highlight, background, icon, null, PlayerAugments.AugmentApplicatorOperation.NEEDS_INIT, parent));
            
        }
        
    }
    
    private void onAugmentClick(ResourceLocation id, PlayerAugments.AugmentApplicatorOperation operation, boolean confirmed) {
        
        if (!confirmed) {
            showAugmentDialog(id, operation);
            return;
        }
        
        var operationId = operation.ordinal();
        NetworkManager.sendToServer(new PlayerAugments.AugmentInstallTriggerPacket(this.menu.blockPos, id, operationId));
    }
    
    private void onLoadAugmentsClick() {
        NetworkManager.sendToServer(new PlayerAugments.LoadPlayerAugmentsToMachinePacket(this.menu.blockPos));
        
        var loadedAugmentsCount = 0;
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
        
        var researchRecipe = (AugmentDataRecipe) this.menu.blockEntity.getLevel().getRecipeManager().byKey(id).get().value();
        
        var isCreative = this.menu.player.isCreative();
        var hasResources = true;
        var hasEnergy = true;
        
        var panel = Containers.verticalFlow(Sizing.fixed(310), Sizing.content(1));
        panel.padding(Insets.of(5));
        panel.surface(ORITECH_PANEL);
        panel.horizontalAlignment(HorizontalAlignment.CENTER);
        
        var descriptionPanel = Containers.verticalFlow(Sizing.fill(100), Sizing.content(3));
        descriptionPanel.surface(Surface.PANEL_INSET);
        descriptionPanel.padding(Insets.of(3, 0, 3, 3));
        descriptionPanel.margins(Insets.of(4));
        
        var overlay = Containers.overlay(panel);
        
        var titleLabel = Components.label(Component.translatable(augmentKey(id)).withStyle(ChatFormatting.BOLD, ChatFormatting.BLACK));
        titleLabel.margins(Insets.of(3, 1, 0, 0));
        
        descriptionPanel.child(Components.label(Component.translatable(augmentKey(id) + ".desc").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
        for (int i = 1; i < 8; i++) {
            var key = augmentKey(id) + ".desc." + i;
            if (I18n.exists(key))
                descriptionPanel.child(Components.label(Component.translatable(key).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
            
        }
        
        var requiredStationBlock = BuiltInRegistries.BLOCK.get(researchRecipe.getRequiredStation());
        var requiredStationLabel = Components.label(Component.translatable("oritech.text.required_station", requiredStationBlock.getName()));
        descriptionPanel.child(requiredStationLabel.margins(Insets.of(4, 2, 0, 0)));
        
        var hasRequiredStation = false;
        for (int i = 0; i < 3; i++) {
            var station = this.menu.blockEntity.availableStations.getOrDefault(i, null);
            if (station == null) continue;
            if (station.working) continue;
            if (!requiredStationBlock.equals(station.type)) continue;
            hasRequiredStation = true;
        }
        
        if (!operation.equals(PlayerAugments.AugmentApplicatorOperation.REMOVE)) {
            
            if (operation.equals(PlayerAugments.AugmentApplicatorOperation.RESEARCH)) {
                var rfCost = researchRecipe.getRfCost();
                var parsedCost = TooltipHelper.getEnergyText(rfCost);
                
                descriptionPanel.child(Components.label(Component.translatable("oritech.text.augment_research_time", researchRecipe.getTime() / 20).withStyle(ChatFormatting.WHITE)).margins(Insets.of(4, 0, 0, 0)));
                descriptionPanel.child(Components.label(Component.translatable("oritech.text.energy_cost", parsedCost).withStyle(ChatFormatting.WHITE)).margins(Insets.of(4, 0, 0, 0)));
                
                if (this.menu.blockEntity.getEnergyStorageForMultiblock(null).getAmount() < rfCost)
                    hasEnergy = false;
                
            }
            
            descriptionPanel.child(Components.label(Component.translatable("oritech.text.augment_resource_cost").withStyle(ChatFormatting.WHITE)).margins(Insets.of(4, 0, 0, 0)));
            
            var itemContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.content(1));
            var shownCost = researchRecipe.getResearchCost();
            if (operation.equals(PlayerAugments.AugmentApplicatorOperation.ADD)) {
                shownCost = researchRecipe.getApplyCost();
            }
            
            for (var wantedInput : shownCost) {
                var type = wantedInput.ingredient();
                var count = wantedInput.count();
                var matchingIngredients = this.menu.blockEntity.inventory.heldStacks.stream().filter(type).mapToInt(ItemStack::getCount).sum();
                var playerMatchingIngredients = this.menu.player.getInventory().items.stream().filter(type).mapToInt(ItemStack::getCount).sum();
                if (playerMatchingIngredients + matchingIngredients < count) {
                    hasResources = false;
                    break;
                }
            }
            
            for (var input : shownCost) {
                var shownItem = Arrays.stream(input.ingredient().getItems()).findFirst().get().getItem();
                var shownStack = new ItemStack(shownItem, input.count());
                
                var allMatchingItems = Arrays.stream(input.ingredient().getItems()).map(ItemStack::getHoverName).toList();
                var combinedList = new ArrayList<Component>();
                combinedList.add(Component.translatable("oritech.text.augment_ingredient_tip").withStyle(ChatFormatting.BOLD, ChatFormatting.GRAY));
                combinedList.addAll(allMatchingItems);
                
                var shown = Components.item(shownStack).showOverlay(true).setTooltipFromStack(false);
                
                if (allMatchingItems.size() > 1) {
                    shown.tooltip(combinedList);
                } else {
                    shown.setTooltipFromStack(true);
                }
                itemContainer.child(shown.margins(Insets.of(2)));
            }
            descriptionPanel.child(itemContainer);
        }
        
        var buttonPanel = Containers.horizontalFlow(Sizing.fill(100), Sizing.fixed(26));
        buttonPanel.margins(Insets.of(2, 0, 4, 4));
        buttonPanel.horizontalAlignment(HorizontalAlignment.RIGHT);
        
        var confirmKey = "text.oritech.begin_research";
        if (operation.equals(PlayerAugments.AugmentApplicatorOperation.ADD)) {
            confirmKey = "text.oritech.install";
        } else if (operation.equals(PlayerAugments.AugmentApplicatorOperation.REMOVE)) {
            confirmKey = "text.oritech.remove";
        } else if (operation.equals(PlayerAugments.AugmentApplicatorOperation.NONE)) {
            confirmKey = "text.oritech.noop";
        }
        
        var cancelButton = Components.button(Component.translatable("text.oritech.cancel").withColor(GRAY_TEXT_COLOR), component -> overlay.remove());
        cancelButton.textShadow(false);
        
        var confirmButton = Components.button(Component.translatable(confirmKey).withColor(GRAY_TEXT_COLOR), component -> {
            onAugmentClick(id, operation, true);
            overlay.remove();
        });
        confirmButton.textShadow(false);
        
        cancelButton.renderer(ORITECH_BUTTON);
        confirmButton.renderer(ORITECH_BUTTON);
        
        if ((!hasResources || !hasEnergy) && isCreative) {
            hasResources = true;
            hasEnergy = true;
            var text = Component.literal("[C] ").withStyle(ChatFormatting.DARK_PURPLE).append(Component.translatable(confirmKey));
            confirmButton.setMessage(text);
            confirmButton.tooltip(Component.translatable("text.oritech.augmenter_creative_tooltip"));
        }
        
        if (operation.equals(PlayerAugments.AugmentApplicatorOperation.NONE) || operation.equals(PlayerAugments.AugmentApplicatorOperation.RESEARCH) && (!hasRequiredStation || !hasResources || !hasEnergy) || operation.equals(PlayerAugments.AugmentApplicatorOperation.ADD) && !hasResources) {
            confirmButton.active(false);
        }
        
        buttonPanel.child(cancelButton.margins(Insets.of(2)));
        buttonPanel.child(confirmButton.margins(Insets.of(2, 2, 2, 0)));
        
        
        panel.child(titleLabel);
        panel.child(descriptionPanel);
        panel.child(buttonPanel);
        
        overlay.zIndex(100);
        root.child(overlay);
        
    }
    
    private void addEnergyBar(FlowLayout panel) {
        
        var insetSize = 1;
        var tooltipText = Component.translatable("tooltip.oritech.energy_indicator", 10, 50);
        
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
    
    private static final class AugmentUiState {
        private BoxComponent highlight;
        private TextureComponent background;
        private TextureComponent icon;
        private BoxComponent blocker;
        private PlayerAugments.AugmentApplicatorOperation openOp;
        private final FlowLayout parent;
        
        private AugmentUiState(BoxComponent highlight, TextureComponent background, TextureComponent icon, BoxComponent blocker, PlayerAugments.AugmentApplicatorOperation openOp, FlowLayout parent) {
            this.highlight = highlight;
            this.background = background;
            this.icon = icon;
            this.blocker = blocker;
            this.openOp = openOp;
            this.parent = parent;
        }
    }
    
    private static void drawLine(GuiGraphics context, Vector2i from, Vector2i to, int color) {
        
        if (from.distanceSquared(to) < 0.1) return;
        
        var matrices = context.pose();
        matrices.pushPose();
        
        var pos = matrices.last().pose();
        var normal = AugmentSelectionScreen.getNormalVector(from, to).normalize();
        var offset = normal.mul(1);
        var zIndex = 1.1f;
        
        var buffer = context.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(pos, from.x - offset.x, from.y - offset.y, zIndex).setColor(color);
        buffer.addVertex(pos, from.x + offset.x, from.y + offset.y, zIndex).setColor(color);
        buffer.addVertex(pos, to.x + offset.x, to.y + offset.y, zIndex).setColor(color);
        buffer.addVertex(pos, to.x - offset.x, to.y - offset.y, zIndex).setColor(color);
        context.flush();
        
        matrices.popPose();
    }

    public static String augmentKey(ResourceLocation id) {
        return "oritech.text." + id.getPath().replace('/', '.');
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
    
    private static class DraggableScrollContainer<C extends io.wispforest.owo.ui.core.Component> extends ScrollContainer<C> {
        
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
                drawLine(context, new Vector2i(dependency.getA()).add(offset), new Vector2i(dependency.getB()).add(offset), new Color(0.1f, 0.15f, 0.2f, 1f).argb());
            }
            
        }
        
        public double getScrollPosition() {
            return currentScrollPosition;
        }
        
    }
    
    public static class SizedIngredientTooltipComponent implements ClientTooltipComponent {
        
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
        public int getWidth(Font textRenderer) {
            return (size + spacing) * items.size();
        }
        
        @Override
        public void renderImage(Font textRenderer, int x, int y, GuiGraphics context) {
            context.push();
            // context.getMatrices().translate(0, spacing, 0);
            
            for (int i = 0; i < items.size(); i++) {
                var ingredient = items.get(i);
                if (ingredient.ingredient().getItems().length == 0) continue;
                var stack = Arrays.stream(ingredient.ingredient().getItems()).findFirst().get();
                stack = new ItemStack(stack.getItem(), ingredient.count());
                
                var itemX = x + (size + spacing) * i;
                var itemY = y + spacing;
                
                context.renderItem(stack, itemX, itemY);
                
                if (stack.getCount() > 1) {
                    context.pose().translate(0, 0, 200);
                    context.drawString(textRenderer, String.valueOf(stack.getCount()), itemX + 19 - 2 - textRenderer.width(String.valueOf(stack.getCount())), itemY + 6 + 3, 16777215, true);
                }
                
            }
            
            context.pop();
        }
    }
    
}
