package rearth.oritech.client.renderers.blocks;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import rearth.oritech.client.renderers.blocks.SmallTankRenderer.FluidCube;
import rearth.oritech.client.renderers.models.MachineModel;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefineryRenderer<T extends RefineryBlockEntity & GeoAnimatable, R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<T, R> {

    public static final DataTicket<RefineryFluidData> FLUID_DATA = DataTicket.create("refinery_fluids", RefineryFluidData.class);

    // smoothed fill heights, keyed by block position (per-block-entity animation state)
    private final Map<Long, VisualTankHeights> tankHeights = new HashMap<>();

    public RefineryRenderer(BlockEntityRendererProvider.Context context, String model) {
        super(context, new MachineModel<>(model));
    }

    // this overrides a method from IBlockEntityRendererExtension on NF. Since this extension mixin is not available
    // in common, we just declare the method without the override annotation
    public AABB getRenderBoundingBox(T blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 6, 6, 6);
    }

    // extract phase: resolve all fluid cubes and ship them to the render state via the GeckoLib DataTicket
    @Override
    public void addRenderData(T animatable, @Nullable Void relatedObject, R renderState, float partialTick) {

        // add machine color to state (can be empty, meaning default orange)
        MachineRenderer.addColorRenderData(animatable, renderState);

        var lastHeight = tankHeights.computeIfAbsent(animatable.getBlockPos().asLong(), key -> new VisualTankHeights());
        var cubes = new ArrayList<FluidCube>();

        var inputStack = animatable.ownStorage.getInStack();
        if (!inputStack.isEmpty()) {
            cubes.add(buildCube(new Vector3f(-24 / 16f, 3 / 16f, 11 / 16f), new Vector3f(12 / 16f, 25 / 16f, 28 / 16f), inputStack, animatable.ownStorage.getCapacity(), -1, lastHeight));
        }

        var moduleCount = animatable.getModuleCount();
        for (int i = 0; i <= moduleCount; i++) {
            var renderedStack = animatable.getOutputFluid(i);
            if (renderedStack.isEmpty()) continue;

            var tankPosition = getTankCoordinates(i);
            cubes.add(buildCube(tankPosition.getA(), tankPosition.getB(), renderedStack, animatable.getOutputCapacity(i), i, lastHeight));
        }

        if (!cubes.isEmpty()) renderState.addGeckolibData(FLUID_DATA, new RefineryFluidData(cubes));
    }

    // submit phase: draw the previously resolved fluid cubes through the submit pipeline
    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {

        var data = renderPassInfo.getGeckolibData(FLUID_DATA);
        if (data == null) return;

        var poseStack = renderPassInfo.poseStack();
        poseStack.pushPose();
        poseStack.last().set(renderPassInfo.getModelRenderMatrixPose());
        SmallTankRenderer.submitFluidCubes(renderTasks, poseStack, data.cubes(), renderPassInfo.packedLight(), OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private FluidCube buildCube(Vector3f min, Vector3f size, FluidStack drawnStack, long tankCapacity, int index, VisualTankHeights lastHeight) {
        var fluid = drawnStack.getFluid();
        var fill = drawnStack.getAmount() / (float) tankCapacity;

        // smooth the fill change to avoid visual snapping
        var lastFill = index == -1 ? lastHeight.input : lastHeight.outputs[index];
        var newFill = Mth.lerp(0.003f, lastFill, fill);
        if (index == -1) {
            lastHeight.input = newFill;
        } else {
            lastHeight.outputs[index] = newFill;
        }

        var sprite = RenderHelpers.getFluidSprite(fluid);
        var color = ColorHelper.makeOpaque(ColorHelper.getFluidTint(drawnStack));

        return new FluidCube(min, size, newFill, sprite, color, null);
    }

    private static Tuple<Vector3f, Vector3f> getTankCoordinates(int i) {
        return switch (i) {
            case 0 ->
                    new Tuple<>(new Vector3f(-22 / 16f, 9 / 16f, -5 / 16f), new Vector3f(7 / 16f, 15 / 16f, 10 / 16f));
            case 1 ->
                    new Tuple<>(new Vector3f(-21 / 16f, 0 / 16f + 2, -5 / 16f), new Vector3f(26 / 16f, 14 / 16f, 26 / 16f));
            case 2 ->
                    new Tuple<>(new Vector3f(-21 / 16f, 0 / 16f + 3, -5 / 16f), new Vector3f(26 / 16f, 14 / 16f, 26 / 16f));
            default -> throw new IllegalStateException("Tried to access invalid tank for renderer: " + i);
        };
    }

    public record RefineryFluidData(List<FluidCube> cubes) {
    }

    private static class VisualTankHeights {
        private float input = 0;
        private final float[] outputs = new float[3];
    }
}
