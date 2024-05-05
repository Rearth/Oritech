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
import rearth.oritech.client.renderers.LaserArmModel;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class BasicMachineScreen<S extends BasicMachineScreenHandler> extends BaseOwoHandledScreen<FlowLayout, S> {
    
    
    public static final Identifier BACKGROUND = new Identifier(Oritech.MOD_ID, "textures/gui/modular/gui_base.png");
    public static final Identifier ITEM_SLOT = new Identifier(Oritech.MOD_ID, "textures/gui/modular/itemslot.png");
    public static final Identifier GUI_COMPONENTS = new Identifier(Oritech.MOD_ID, "textures/gui/modular/machine_gui_components.png");
    public FlowLayout root;
    private TextureComponent progress_indicator;
    private TextureComponent energyIndicator;
    private ButtonComponent cycleInputButton;
    private BoxComponent fluidFillStatusOverlay;
    
    private float lastFluidFill = 1;    // to allow smooth interpolation
    private ColoredSpriteComponent fluidBackground;
    
    public BasicMachineScreen(S handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
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
        
        rootComponent.child(
          Containers.horizontalFlow(Sizing.fixed(176 + 250), Sizing.fixed(166 + 40))        // span entire inner area
            .child(Containers.horizontalFlow(Sizing.content(), Sizing.content())
                     .child(buildExtensionPanel())
                     .surface(Surface.PANEL)
                     .positioning(Positioning.absolute(176 + 117, 30)))
            .positioning(Positioning.relative(50, 50))
            .zIndex(-1)
        ).child(
          Components.texture(BACKGROUND, 0, 0, 176, 166, 176, 166)
        ).child(
          buildOverlay().positioning(Positioning.relative(50, 50))
        );
    }
    
    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        
        if (handler.screenData.showEnergy())
            updateEnergyBar();
        
        if (handler.screenData.showProgress())
            updateProgressBar();
        
        updateSettingsButtons();
        
        if (handler.fluidProvider != null)
            updateFluidBar();
    }
    
    private void updateProgressBar() {
        var config = handler.screenData.getIndicatorConfiguration();
        var progress = handler.screenData.getProgress();
        
        if (config.horizontal()) {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, (int) (config.width() * progress), config.height()));
        } else {
            progress_indicator.visibleArea(PositionedRectangle.of(0, 0, config.width(), (int) (config.height() * progress)));
        }
    }
    
    private void updateEnergyBar() {
        
        var capacity = handler.energyStorage.getCapacity();
        var amount = handler.energyStorage.getAmount();
        
        var fillAmount = (float) amount / capacity;
        var tooltipText = getEnergyTooltip(amount, capacity);
        
        energyIndicator.tooltip(tooltipText);
        energyIndicator.visibleArea(PositionedRectangle.of(0, 96 - ((int) (96 * (fillAmount))), 24, (int) (96 * fillAmount)));
    }
    
    public Text getEnergyTooltip(long amount, long max) {
        var percentage = (float) amount / max;
        var energyFill = String.format("%.1f", percentage * 100);
        var energyUsage = handler.screenData.getDisplayedEnergyUsage();
        var energyUsageText = String.format("%.1f", energyUsage);
        return Text.literal(amount + " / " + max + " RF\n" + energyFill + "% Charged\n\nMaximum Usage: " + energyUsageText + " RF/t");
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
        
        cycleInputButton = Components.button(Text.literal("Match Recipe"),
          button -> {
              NetworkContent.UI_CHANNEL.clientHandle().send(new NetworkContent.InventoryInputModeSelectorPacket(handler.blockPos));
          });
        cycleInputButton.horizontalSizing(Sizing.fixed(73));
        cycleInputButton.margins(Insets.of(3));
        
        container.child(Components.label(Text.literal("Options")).margins(Insets.of(3, 1, 1, 1)));
        
        if (handler.screenData.inputOptionsEnabled())
            container.child(cycleInputButton);
    }
    
    private FlowLayout buildOverlay() {
        
        var overlay = Containers.horizontalFlow(Sizing.fixed(176), Sizing.fixed(166));
        fillOverlay(overlay);
        
        return overlay;
    }
    
    public void fillOverlay(FlowLayout overlay) {
        
        addTitle(overlay);
        
        if (handler.fluidProvider != null) {
            addFluidBar(overlay);
            updateFluidBar();
        }
        
        for (var slot : handler.screenData.getGuiSlots()) {
            overlay.child(this.slotAsComponent(slot.index()).positioning(Positioning.absolute(slot.x(), slot.y())));
            overlay.child(getItemFrame(slot.x(), slot.y()));
        }
        
        if (handler.screenData.showEnergy()) {
            addEnergyBar(overlay);
            updateEnergyBar();
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
    
    protected void updateFluidBar() {
        
        var container = handler.fluidProvider.getForDirectFluidAccess();
        var data = handler.screenData.getFluidConfiguration();

        if (fluidBackground.getSprite() == null && !container.isResourceBlank() && container.amount > 0) {
            var parent = fluidBackground.parent();
            var targetIndex = parent.children().indexOf(fluidBackground);
            var newFluid = createFluidRenderer(container.getResource(), container.getAmount(), data);
            parent.removeChild(fluidBackground);
            ((FlowLayout) parent).child(targetIndex, newFluid);
            fluidBackground = newFluid;
        }

        var fill = 1 - ((float) container.getAmount() / container.getCapacity());

        var targetFill = LaserArmModel.lerp(lastFluidFill, fill, 0.15f);
        lastFluidFill = targetFill;

        fluidFillStatusOverlay.verticalSizing(Sizing.fixed((int) (data.height() * targetFill * 0.98f)));

        var tooltipText = List.of(Text.of(FluidVariantRendering.getTooltip(container.getResource()).get(0)), Text.of((container.getAmount() * 1000 / FluidConstants.BUCKET) + " mb"));
        fluidBackground.tooltip(tooltipText);
    }
    
    private void addProgressArrow(FlowLayout panel) {
        
        var config = handler.screenData.getIndicatorConfiguration();
        
        var empty = Components.texture(config.empty(), 0, 0, config.width(), config.height(), config.width(), config.height());
        progress_indicator = Components.texture(config.full(), 0, 0, config.width(), config.height(), config.width(), config.height());
        
        panel
          .child(empty.positioning(Positioning.absolute(config.x(), config.y())))
          .child(progress_indicator.positioning(Positioning.absolute(config.x(), config.y())));
    }
    
    // only supports single variants, for more complex variants override this
    protected void addFluidBar(FlowLayout panel) {
        
        var container = handler.fluidProvider.getForDirectFluidAccess();
        fluidBackground = null;
        var config = handler.screenData.getFluidConfiguration();
        lastFluidFill = 1 - ((float) container.getAmount() / container.getCapacity());
        
        
        for (var it = container.nonEmptyIterator(); it.hasNext(); ) {
            var fluid = it.next();
            fluidBackground = createFluidRenderer(fluid.getResource(), fluid.getAmount(), config);
            break;
        }
        
        if (fluidBackground == null) {
            fluidBackground = createFluidRenderer(FluidVariant.of(Fluids.EMPTY), 0L, config);
        }
        
        fluidFillStatusOverlay = Components.box(Sizing.fixed(config.width()), Sizing.fixed((int) (config.height() * lastFluidFill)));
        fluidFillStatusOverlay.color(new Color(77.6f / 255f, 77.6f / 255f, 77.6f / 255f));
        fluidFillStatusOverlay.fill(true);
        fluidFillStatusOverlay.positioning(Positioning.absolute(config.x(), config.y()));
        
        
        var foreGround = Components.texture(GUI_COMPONENTS, 48, 0, 14, 50, 98, 96);
        foreGround.sizing(Sizing.fixed(config.width()), Sizing.fixed(config.height()));
        foreGround.positioning(Positioning.absolute(config.x(), config.y()));
        
        panel.child(fluidBackground);
        panel.child(fluidFillStatusOverlay);
        panel.child(foreGround);
        
    }
    
    public static ColoredSpriteComponent createFluidRenderer(FluidVariant variant, long amount, ScreenProvider.BarConfiguration config) {
        var sprite = FluidVariantRendering.getSprite(variant);
        var spriteColor = FluidVariantRendering.getColor(variant);
        
        return getColoredSpriteComponent(variant, amount, config, sprite, spriteColor);
    }
    
    @NotNull
    private static ColoredSpriteComponent getColoredSpriteComponent(FluidVariant variant, long amount, ScreenProvider.BarConfiguration config, Sprite sprite, int spriteColor) {
        var tooltipText = List.of(Text.of(FluidVariantRendering.getTooltip(variant).get(0)), Text.of((amount * 1000 / FluidConstants.BUCKET) + " mb"));
        
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
        var tooltipText = Text.literal("10/50 RF");
        
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
        
        // direct copy of the method in drawContext, because I can't be called from here due to private access
        private void drawTexturedQuad(Identifier texture, MatrixStack matrices, int x1, int x2, int y1, int y2, int z, float u1, float u2, float v1, float v2, float red, float green, float blue, float alpha) {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(GameRenderer::getPositionColorTexProgram);
            RenderSystem.enableBlend();
            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
            bufferBuilder.vertex(matrix4f, x1, y1, z).color(red, green, blue, alpha).texture(u1, v1).next();
            bufferBuilder.vertex(matrix4f, x1, y2, z).color(red, green, blue, alpha).texture(u1, v2).next();
            bufferBuilder.vertex(matrix4f, x2, y2, z).color(red, green, blue, alpha).texture(u2, v2).next();
            bufferBuilder.vertex(matrix4f, x2, y1, z).color(red, green, blue, alpha).texture(u2, v1).next();
            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
            RenderSystem.disableBlend();
        }
        
    }
}
