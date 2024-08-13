package rearth.oritech.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.SpriteUtilInvoker;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.entity.machines.generators.BasicGeneratorEntity;
import rearth.oritech.client.renderers.LaserArmModel;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.util.TooltipHelper;

import java.util.List;

public class BasicMachineScreen<S extends BasicMachineScreenHandler> extends BaseOwoHandledScreen<FlowLayout, S> {
    
    
    public static final Identifier BACKGROUND = Oritech.id("textures/gui/modular/gui_base.png");
    public static final Identifier ITEM_SLOT = Oritech.id("textures/gui/modular/itemslot.png");
    public static final Identifier GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    public FlowLayout root;
    private TextureComponent progress_indicator;
    protected TextureComponent energyIndicator;
    private ButtonComponent cycleInputButton;
    
    private final FluidDisplay genericDisplay;
    private final FluidDisplay steamDisplay;
    private final FluidDisplay waterDisplay;
    protected final LabelComponent steamProductionLabel;
    
    protected static final class FluidDisplay {
        private final BoxComponent fillOverlay;
        private float lastFill;
        private ColoredSpriteComponent background;
        private final TextureComponent foreGround;
        private final ScreenProvider.BarConfiguration config;
        private final SingleVariantStorage<FluidVariant> storage;
        
        private FluidDisplay(BoxComponent fillOverlay, float lastFill, ColoredSpriteComponent background, TextureComponent foreGround, ScreenProvider.BarConfiguration config, SingleVariantStorage<FluidVariant> storage) {
            this.fillOverlay = fillOverlay;
            this.lastFill = lastFill;
            this.background = background;
            this.foreGround = foreGround;
            this.config = config;
            this.storage = storage;
        }
    }
    
    public BasicMachineScreen(S handler, PlayerInventory inventory, Text title) {
        
        super(handler, inventory, title);
        
        if (handler.fluidProvider != null) {
            var container = handler.fluidProvider.getForDirectFluidAccess();
            var config = handler.screenData.getFluidConfiguration();
            genericDisplay = initFluidDisplay(container, config);
        } else {
            genericDisplay = null;
        }
        
        if (handler.steamStorage != null) {
            var config = handler.screenData.getEnergyConfiguration();
            var container = handler.waterStorage;
            waterDisplay = initFluidDisplay(container, config);
            
            var offset = config.width() + 8;
            
            var configSteam = new ScreenProvider.BarConfiguration(config.x() + offset, config.y(), config.width(), config.height());
            var containerSteam = handler.steamStorage;
            steamDisplay = initFluidDisplay(containerSteam, configSteam);
            // the label is then actually added to the screen in the upgradable screen extension
            steamProductionLabel = Components.label(Text.translatable("title.oritech.steam_production", 0.0F));
            steamProductionLabel.tooltip(Text.translatable("tooltip.oritech.steam_production"));
        } else {
            steamDisplay = null;
            waterDisplay = null;
            steamProductionLabel = null;
        }
        
    }
    
    protected static FluidDisplay initFluidDisplay(SingleVariantStorage<FluidVariant> container, ScreenProvider.BarConfiguration config) {
        var lastFill = 1 - ((float) container.getAmount() / container.getCapacity());
        ColoredSpriteComponent background = null;
        
        
        for (var it = container.nonEmptyIterator(); it.hasNext(); ) {
            var fluid = it.next();
            background = createFluidRenderer(fluid.getResource(), fluid.getAmount(), config);
            break;
        }
        
        if (background == null) {
            background = createFluidRenderer(FluidVariant.of(Fluids.EMPTY), 0L, config);
        }
        
        var fillOverlay = Components.box(Sizing.fixed(config.width()), Sizing.fixed((int) (config.height() * lastFill)));
        fillOverlay.color(new Color(77.6f / 255f, 77.6f / 255f, 77.6f / 255f));
        fillOverlay.fill(true);
        fillOverlay.positioning(Positioning.absolute(config.x(), config.y()));
        
        
        var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
        foreGround.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        foreGround.positioning(Positioning.absolute(config.x(), config.y()));
        
        return new FluidDisplay(fillOverlay, lastFill, background, foreGround, config, container);
    }
    
    public static Component getItemFrame(int x, int y) {
        return Components.texture(ITEM_SLOT, 0, 0, 18, 18, 18, 18).sizing(Sizing.fixed(18)).positioning(Positioning.absolute(x - 1, y - 1));
    }
    
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }
    
    @Override
    protected void build(FlowLayout rootComponent) {
        this.root = rootComponent;
        
        rootComponent
          .surface(Surface.VANILLA_TRANSLUCENT)
          .horizontalAlignment(HorizontalAlignment.CENTER)
          .verticalAlignment(VerticalAlignment.CENTER);
        
        if (showExtensionPanel()) {
            rootComponent.child(
              Containers.horizontalFlow(Sizing.fixed(176 + 250), Sizing.fixed(166 + 40))        // span entire inner area
                .child(Containers.horizontalFlow(Sizing.content(), Sizing.content())
                         .child(buildExtensionPanel())
                         .surface(Surface.PANEL)
                         .positioning(Positioning.absolute(176 + 117, 30)))
                .positioning(Positioning.relative(50, 50))
                .zIndex(-1)
            );
        }
        
        rootComponent.child(
          Components.texture(BACKGROUND, 0, 0, 176, 166, 176, 166)
        ).child(
          buildOverlay().positioning(Positioning.relative(50, 50))
        );
    }
    
    public boolean showExtensionPanel() {
        return true;
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        if (handler.screenData.showEnergy()) {
            if (handler.steamStorage != null) {
                updateFluidDisplay(waterDisplay);
                updateFluidDisplay(steamDisplay);
            } else {
                updateEnergyBar();
            }
        }
        
        if (handler.screenData.showProgress())
            updateProgressBar();
        
        if (showExtensionPanel())
            updateSettingsButtons();
        
        if (handler.fluidProvider != null)
            updateFluidDisplay(genericDisplay);
        
        if (steamProductionLabel != null) {
            var productionRate = handler.screenData.getDisplayedEnergyUsage() * Oritech.CONFIG.generators.rfToSteamRation();
            productionRate = Math.min(this.waterDisplay.storage.amount, productionRate);
            steamProductionLabel.text(Text.translatable("title.oritech.steam_production", productionRate));
        }
    }
    
    private void updateProgressBar() {
        var config = handler.screenData.getIndicatorConfiguration();
        var progress = handler.screenData.getProgress();
        
        
        if (handler.blockEntity instanceof MachineBlockEntity machineEntity && (machineEntity.getCurrentRecipe().getTime() > 0 || machineEntity.progress > 0)) {
            
            var progressTicks = machineEntity.progress;
            var recipeDurationTicks = machineEntity.getCurrentRecipe().getTime();
            var effectiveDurationTicks = (int) (recipeDurationTicks * machineEntity.getSpeedMultiplier());
            
            if (machineEntity instanceof UpgradableGeneratorBlockEntity generatorBlock) {
                if (recipeDurationTicks <= 0)
                    recipeDurationTicks = (int) (generatorBlock.currentMaxBurnTime / generatorBlock.getSpeedMultiplier() * generatorBlock.getEfficiencyMultiplier());
                effectiveDurationTicks = generatorBlock.currentMaxBurnTime;
            }
            
            if (machineEntity instanceof BasicGeneratorEntity generatorEntity)
                recipeDurationTicks = generatorEntity.currentMaxBurnTime;
            
            
            progress_indicator.tooltip(Text.translatable("tooltip.oritech.progress_indicator", progressTicks, effectiveDurationTicks, recipeDurationTicks));
        }
        
        
        if (config.horizontal()) {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, (int) (config.width() * progress), config.height()));
        } else {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, config.width(), (int) (config.height() * progress)));
        }
    }
    
    protected void updateEnergyBar() {
        
        var capacity = handler.energyStorage.getCapacity();
        var amount = handler.energyStorage.getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    public Text getEnergyTooltip(long amount, long max) {
        var percentage = (float) amount / max;
        var energyUsage = handler.screenData.getDisplayedEnergyUsage();
        var storedAmount = TooltipHelper.getEnergyText(amount);
        var maxAmount = TooltipHelper.getEnergyText(max);
        return Text.translatable("tooltip.oritech.energy_usage", storedAmount, maxAmount, percentage, energyUsage);
    }
    
    public void updateSettingsButtons() {
        
        var activeMode = handler.screenData.getInventoryInputMode();
        var modeName = activeMode.name().toLowerCase();
        
        cycleInputButton.setMessage(Text.translatable("button.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
        cycleInputButton.tooltip(Text.translatable("tooltip.%s.input_mode_%s".formatted(Oritech.MOD_ID, modeName)));
    }
    
    private Component buildExtensionPanel() {
        
        var container = Containers.verticalFlow(Sizing.content(), Sizing.content());
        container.surface(Surface.PANEL_INSET);
        container.horizontalAlignment(HorizontalAlignment.CENTER);
        
        container.padding(Insets.of(1, 4, 1, 1));
        container.margins(Insets.of(7));
        
        addExtensionComponents(container);
        updateSettingsButtons();
        
        return container;
    }
    
    public void addExtensionComponents(FlowLayout container) {
        
        cycleInputButton = Components.button(Text.translatable("button.oritech.input_mode_fill_matching_recipe"),
          button -> {
              NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.InventoryInputModeSelectorPacket(handler.blockPos));
          });
        cycleInputButton.horizontalSizing(Sizing.fixed(73));
        cycleInputButton.margins(Insets.of(3));
        
        container.child(Components.label(Text.translatable("title.oritech.details")).margins(Insets.of(3, 1, 1, 1)));
        
        if (handler.screenData.inputOptionsEnabled())
            container.child(cycleInputButton);
        
        for (var label : handler.screenData.getExtraExtensionLabels()) {
            container.child(Components.label(label.getLeft()).tooltip(label.getRight()).margins(Insets.of(3)));
        }
        
    }
    
    private FlowLayout buildOverlay() {
        
        var overlay = Containers.verticalFlow(Sizing.fixed(176), Sizing.fixed(166));
        fillOverlay(overlay);
        
        return overlay;
    }
    
    public void fillOverlay(FlowLayout overlay) {
        
        addTitle(overlay);
        
        if (handler.fluidProvider != null) {
            addFluidDisplay(overlay, genericDisplay);
            updateFluidDisplay(genericDisplay);
        }
        
        for (var slot : handler.screenData.getGuiSlots()) {
            overlay.child(this.slotAsComponent(slot.index()).positioning(Positioning.absolute(slot.x(), slot.y())));
            overlay.child(getItemFrame(slot.x(), slot.y()));
        }
        
        if (handler.screenData.showEnergy()) {
            if (handler.steamStorage != null) {
                addFluidDisplay(overlay, steamDisplay);
                updateFluidDisplay(steamDisplay);
                addFluidDisplay(overlay, waterDisplay);
                updateFluidDisplay(waterDisplay);
            } else {
                addEnergyBar(overlay);
                updateEnergyBar();
            }
        }
        
        if (handler.screenData.showProgress()) {
            addProgressArrow(overlay);
            updateProgressBar();
        }
    }
    
    private void addTitle(FlowLayout overlay) {
        var blockTitle = handler.machineBlock.getBlock().getName();
        var label = Components.label(blockTitle);
        label.color(new Color(64 / 255f, 64 / 255f, 64 / 255f));
        label.sizing(Sizing.fixed(176), Sizing.content(2));
        label.horizontalTextAlignment(HorizontalAlignment.CENTER);
        label.zIndex(1);
        overlay.child(label.positioning(Positioning.relative(54, 2)));
    }
    
    private void addProgressArrow(FlowLayout panel) {
        
        var config = handler.screenData.getIndicatorConfiguration();
        
        var empty = Components.texture(config.empty(), 0, 0, config.width(), config.height(), config.width(), config.height());
        progress_indicator = Components.texture(config.full(), 0, 0, config.width(), config.height(), config.width(), config.height());
        
        panel
          .child(empty.positioning(Positioning.absolute(config.x(), config.y())))
          .child(progress_indicator.positioning(Positioning.absolute(config.x(), config.y())));
    }
    
    protected void addFluidDisplay(FlowLayout panel, FluidDisplay display) {
        panel.child(display.background);
        panel.child(display.fillOverlay);
        panel.child(display.foreGround);
    }
    
    protected void updateFluidDisplay(FluidDisplay display) {
        
        var background = display.background;
        var container = display.storage;
        var config = display.config;
        
        if (background.getSprite() == null && !container.isResourceBlank() && container.amount > 0) {
            var parent = background.parent();
            var targetIndex = parent.children().indexOf(background);
            var newFluid = createFluidRenderer(container.getResource(), container.getAmount(), config);
            parent.removeChild(background);
            ((FlowLayout) parent).child(targetIndex, newFluid);
            background = newFluid;
            display.background = background;
        }
        
        var fill = 1 - ((float) container.getAmount() / container.getCapacity());
        
        var targetFill = LaserArmModel.lerp(display.lastFill, fill, 0.15f);
        display.lastFill = targetFill;
        
        display.fillOverlay.verticalSizing(Sizing.fixed((int) (config.height() * targetFill * 0.98f)));
        
        var tooltipText = Text.translatable("tooltip.oritech.fluid_content", container.getAmount() * 1000 / FluidConstants.BUCKET, container.getResource().toString());
        background.tooltip(tooltipText);
    }
    
    public static ColoredSpriteComponent createFluidRenderer(FluidVariant variant, long amount, ScreenProvider.BarConfiguration config) {
        var sprite = FluidVariantRendering.getSprite(variant);
        var spriteColor = FluidVariantRendering.getColor(variant);
        
        return getColoredSpriteComponent(variant, amount, config, sprite, spriteColor);
    }
    
    @NotNull
    private static ColoredSpriteComponent getColoredSpriteComponent(FluidVariant variant, long amount, ScreenProvider.BarConfiguration config, Sprite sprite, int spriteColor) {
        var tooltipText = Text.translatable("tooltip.oritech.fluid_content", amount * 1000 / FluidConstants.BUCKET, variant.toString());
        
        var result = new ColoredSpriteComponent(sprite);
        result.widthMultiplier = config.width() / 60f;
        result.color = Color.ofArgb(spriteColor);
        result.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        result.positioning(Positioning.absolute(config.x(), config.y()));
        result.tooltip(tooltipText);
        return result;
    }
    
    private void addEnergyBar(FlowLayout panel) {
        
        var config = handler.screenData.getEnergyConfiguration();
        var insetSize = 1;
        var tooltipText = Text.translatable("tooltip.oritech.energy_indicator", 10, 50);
        
        var frame = Containers.horizontalFlow(Sizing.fixed(config.width() + insetSize * 2), Sizing.fixed(config.height() + insetSize * 2));
        frame.surface(Surface.PANEL_INSET);
        frame.padding(Insets.of(insetSize));
        frame.positioning(Positioning.absolute(config.x() - insetSize, config.y() - insetSize));
        panel.child(frame);
        
        var indicator_background = Components.texture(GUI_COMPONENTS, 24, 0, 24, 96, 98, 96);
        indicator_background.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        
        energyIndicator = Components.texture(GUI_COMPONENTS, 0, 0, 24, (96), 98, 96);
        energyIndicator.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        energyIndicator.positioning(Positioning.absolute(0, 0));
        energyIndicator.tooltip(tooltipText);
        
        frame
          .child(indicator_background)
          .child(energyIndicator);
    }
    
    public static class ColoredSpriteComponent extends SpriteComponent {
        
        public Color color;
        public float widthMultiplier = 1f;
        
        protected ColoredSpriteComponent(Sprite sprite) {
            super(sprite);
        }
        
        public Sprite getSprite() {
            return sprite;
        }
        
        @Override
        public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
            if (sprite == null) return;
            SpriteUtilInvoker.markSpriteActive(this.sprite);
            drawSprite(this.x, this.y, 0, this.width, this.height, this.sprite, this.color.red(), this.color.green(), this.color.blue(), this.color.alpha(), context.getMatrices());
        }
        
        // these 2 methods are copies from drawContext, width slight modifications
        public void drawSprite(int x, int y, int z, int width, int height, Sprite sprite, float red, float green, float blue, float alpha, MatrixStack matrices) {
            
            var uvWidth = sprite.getMaxU() - sprite.getMinU();
            var newMax = sprite.getMinU() + uvWidth * widthMultiplier;
            
            this.drawTexturedQuad(sprite.getAtlasId(), matrices, x, x + width, y, y + height, z, sprite.getMinU(), newMax, sprite.getMinV(), sprite.getMaxV(), red, green, blue, alpha);
        }
        
        // direct copy of the method in drawContext, because it can't be called from here due to private access
        private void drawTexturedQuad(Identifier texture, MatrixStack matrices, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.enableBlend();
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(matrix4f, (float) x1, (float) y1, (float) z).texture(u1, v1).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix4f, (float) x1, (float) y2, (float) z).texture(u1, v2).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix4f, (float) x2, (float) y2, (float) z).texture(u2, v2).color(red, green, blue, alpha);
            bufferBuilder.vertex(matrix4f, (float) x2, (float) y1, (float) z).texture(u2, v1).color(red, green, blue, alpha);
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
        }
        
    }
}
