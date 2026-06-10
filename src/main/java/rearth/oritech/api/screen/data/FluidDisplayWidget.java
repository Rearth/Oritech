package rearth.oritech.api.screen.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class FluidDisplayWidget extends AbstractDataDisplayWidget {

    private static final Identifier GUI_COMPONENTS = Oritech.id("textures/gui/modular/machine_gui_components.png");
    private static final int FRAME_REGION_WIDTH = 14;
    private static final int FRAME_REGION_HEIGHT = 50;
    private static final int FLUID_BACKGROUND = ColorHelper.argb(0.06f, 0.18f, 0.24f);
    private static final int OVERLAY_COLOR = ColorHelper.argb(0.31f, 0.31f, 0.31f);
    private static final int BURST_DROPLET_COUNT = 9;
    private static final int MAX_ACTIVE_DROPLETS = 24;

    private final Supplier<FluidStack> fluidStackSupplier;
    private final List<FluidBurstDroplet> activeDroplets = new ArrayList<>();
    private final Random random = new Random();
    private int fluidColor = 0xFFFFFFFF;
    @Nullable
    private final BlockPos blockPos;
    private final int tankIndex;

    public FluidDisplayWidget(DisplayDataSource.FluidDataSource dataSource) {
        this(dataSource, null);
    }

    public FluidDisplayWidget(DisplayDataSource.FluidDataSource dataSource, @Nullable BlockPos blockPos) {
        super(dataSource);
        this.blockPos = blockPos;
        this.tankIndex = dataSource.getTankIndex();
        this.fluidStackSupplier = dataSource.getFluidSupplier();
        updateFluidRenderData();
    }

    @Override
    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (blockPos != null && (button == 0 || button == 1)) {
            var carried = Minecraft.getInstance().player.containerMenu.getCarried();
            if (!carried.isEmpty()) {
                var itemFluidStorage = carried.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(carried));
                if (itemFluidStorage != null) {
                    boolean extract = (button == 1);
                    if (canShowTransferBurst(itemFluidStorage, extract)) {
                        spawnTransferBurst(
                                extract,
                                resolveBurstColor(itemFluidStorage, extract),
                                mouseX,
                                mouseY
                        );
                    }
                    PacketDistributor.sendToServer(new OritechScreenHandler.FluidContainerInteractionPacket(blockPos, tankIndex, extract));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int cx = contentX();
        int cy = contentY();
        int cw = contentWidth();
        int ch = contentHeight();

        graphics.fill(cx, cy, cx + cw, cy + ch, FLUID_BACKGROUND);

        if (getFillRatio() > 0) {
            int filledHeight = (int) (ch * getFillRatio());
            int drawY = cy + (ch - filledHeight);
            graphics.fill(cx, drawY, cx + cw, cy + ch, fluidColor);
        }

        int overlayHeight = (int) (ch * (1f - getFillRatio()) * 0.98f);
        if (overlayHeight > 0) {
            graphics.fill(cx, cy, cx + cw, cy + overlayHeight, OVERLAY_COLOR);
        }

        var carried = Minecraft.getInstance().player.containerMenu.getCarried();
        if (isMouseOver(mouseX, mouseY) && !carried.isEmpty()) {
            var itemFluidStorage = carried.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forStack(carried));
            if (itemFluidStorage != null) {
                var color = ColorHelper.argb(1f, 0.75f, 0.75f, 0.25f);
                graphics.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, color);
            }
        }

        renderTransferBurst(graphics, cx, cy, cw, ch);

        graphics.blit(RenderPipelines.GUI_TEXTURED, GUI_COMPONENTS, cx, cy, 48, 0, cw, ch, FRAME_REGION_WIDTH, FRAME_REGION_HEIGHT, 98, 96);
    }

    private void updateFluidRenderData() {
        var stack = fluidStackSupplier.get();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        this.fluidColor = ColorHelper.makeOpaque(ColorHelper.getFluidTint(stack));
    }

    @Override
    public void tick() {
        super.tick();
        updateFluidRenderData();
        tickTransferBurst();
    }

    private boolean canShowTransferBurst(ResourceHandler<FluidResource> itemFluidStorage, boolean extract) {
        if (extract) {
            return getCurrentAmount() > 0;
        }

        if (itemFluidStorage.size() == 0) return false;
        var content = itemFluidStorage.getResource(0);
        return getCurrentAmount() < getCapacity()
                && !content.isEmpty()
                && itemFluidStorage.getAmountAsLong(0) > 0;
    }

    private int resolveBurstColor(ResourceHandler<FluidResource> itemFluidStorage, boolean extract) {
        if (extract) {
            return fluidColor;
        }

        if (itemFluidStorage.size() > 0) {
            var resource = itemFluidStorage.getResource(0);
            if (!resource.isEmpty()) {
                var stack = resource.toStack((int) itemFluidStorage.getAmountAsLong(0));
                return ColorHelper.makeOpaque(ColorHelper.getFluidTint(stack));
            }
        }

        return fluidColor;
    }

    private void spawnTransferBurst(boolean extract, int burstColor, double mouseX, double mouseY) {
        activeDroplets.clear();

        var width = contentWidth();
        var spawnY = mouseY - 14 * (extract ? 1 : 1.3);

        for (int index = 0; index < BURST_DROPLET_COUNT; index++) {
            var spawnX = width * 0.5 + (random.nextDouble() - 0.5) * Math.max(4, width * 0.45);
            var velocityX = (random.nextDouble() - 0.5) * 0.9;
            var velocityY = extract
                    ? -(0.9 + random.nextDouble() * 1.2)
                    : 0.5 + random.nextDouble() * 1.1;
            var size = 1.2 + random.nextDouble() * 1.8;
            int lifetime = 7 + random.nextInt(5);

            activeDroplets.add(new FluidBurstDroplet(
                    spawnX,
                    spawnY,
                    velocityX,
                    velocityY,
                    size,
                    lifetime,
                    burstColor
            ));
        }

        while (activeDroplets.size() > MAX_ACTIVE_DROPLETS) {
            activeDroplets.removeFirst();
        }
    }

    private void tickTransferBurst() {
        if (activeDroplets.isEmpty()) {
            return;
        }

        var width = contentWidth();
        var height = contentHeight();
        var iterator = activeDroplets.iterator();
        while (iterator.hasNext()) {
            var droplet = iterator.next();
            droplet.tick();
            if (droplet.isExpired(width, height)) {
                iterator.remove();
            }
        }
    }

    private void renderTransferBurst(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        if (activeDroplets.isEmpty()) {
            return;
        }

        for (var droplet : activeDroplets) {
            int drawX = x + Mth.floor(droplet.x);
            int drawY = y + Mth.floor(droplet.y);
            int drawSize = Math.max(1, Mth.ceil(droplet.size * droplet.alpha()));
            if (drawX + drawSize < x || drawX >= x + w || drawY + drawSize < y || drawY >= y + h) {
                continue;
            }

            int minX = Math.max(drawX, x);
            int minY = Math.max(drawY, y);
            int maxX = Math.min(drawX + drawSize, x + w);
            int maxY = Math.min(drawY + drawSize, y + h);
            graphics.fill(minX, minY, maxX, maxY, droplet.colorWithAlpha());
        }
    }

    private static final class FluidBurstDroplet {

        private final double size;
        private final int maxLifetime;
        private final int color;
        private double x;
        private double y;
        private double velocityX;
        private double velocityY;
        private int lifetime;

        private FluidBurstDroplet(double x, double y, double velocityX, double velocityY, double size, int maxLifetime, int color) {
            this.x = x;
            this.y = y;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.size = size;
            this.maxLifetime = maxLifetime;
            this.color = color;
        }

        private void tick() {
            x += velocityX;
            y += velocityY;
            velocityX *= 0.92;
            velocityY = velocityY * 0.9 + 0.08;
            lifetime++;
        }

        private boolean isExpired(int width, int height) {
            return lifetime >= maxLifetime || x < -4 || x > width + 4 || y < -4 || y > height + 4;
        }

        private float alpha() {
            return Mth.clamp(1f - (float) lifetime / maxLifetime, 0f, 1f);
        }

        private int colorWithAlpha() {
            int alpha = Mth.clamp((int) (((color >>> 24) & 0xFF) * alpha()), 0, 255);
            return (alpha << 24) | (color & 0x00FFFFFF);
        }
    }
}
