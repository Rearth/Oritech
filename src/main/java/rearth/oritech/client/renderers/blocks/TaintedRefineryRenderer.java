package rearth.oritech.client.renderers.blocks;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.client.renderers.blocks.SmallTankRenderer.FluidCube;
import rearth.oritech.client.renderers.models.MachineModel;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.ColorHelper;

import java.util.ArrayList;
import java.util.List;

public class TaintedRefineryRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<TaintedRefineryBlockEntity, R> {

    public static final DataTicket<TaintedRefineryFluidData> FLUID_DATA = DataTicket.create("tainted_refinery_fluids", TaintedRefineryFluidData.class);

    public TaintedRefineryRenderer(BlockEntityRendererProvider.Context context, String model) {
        super(context, new MachineModel<>(model));
    }

    public AABB getRenderBoundingBox(TaintedRefineryBlockEntity blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 6, 6, 6);
    }

    // extract phase: resolve all fluid cubes and ship them to the render state via the GeckoLib DataTicket
    @Override
    public void addRenderData(TaintedRefineryBlockEntity animatable, @Nullable Void relatedObject, R renderState, float partialTick) {
        MachineRenderer.addColorRenderData(animatable, renderState);

        var cubes = new ArrayList<FluidCube>();
        var capacity = animatable.ownStorage.getCapacity();

        var bigTank = animatable.ownStorage.getInStack();
        if (!bigTank.isEmpty()) {
            cubes.add(buildCube(new Vector3f(-24 / 16f, 3 / 16f, 11 / 16f), new Vector3f(12 / 16f, 24.5f / 16f, 28 / 16f), bigTank, capacity, null));
        }

        var smallTank = animatable.ownStorage.getOutStack();
        if (!smallTank.isEmpty()) {
            // bottom half of twisted tank
            cubes.add(buildCube(new Vector3f(-23.5f / 16f, 7 / 16f, -5 / 16f), new Vector3f(7 / 16f, 8 / 16f, 10 / 16f), smallTank, capacity, Axis.ZN.rotationDegrees(10)));
            // top half
            cubes.add(buildCube(new Vector3f(-18.25f / 16f, 19 / 16f, -5 / 16f), new Vector3f(7 / 16f, 8 / 16f, 10 / 16f), smallTank, capacity, Axis.ZP.rotationDegrees(7.5f)));
        }

        if (!cubes.isEmpty()) renderState.addGeckolibData(FLUID_DATA, new TaintedRefineryFluidData(cubes));
    }

    // submit phase: draw the previously resolved fluid cubes through the submit pipeline
    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {

        var data = renderPassInfo.getGeckolibData(FLUID_DATA);
        if (data == null) return;

        SmallTankRenderer.submitFluidCubes(renderTasks, renderPassInfo.poseStack(), data.cubes(), renderPassInfo.packedLight(), OverlayTexture.NO_OVERLAY);
    }

    private static FluidCube buildCube(Vector3f min, Vector3f size, FluidStack drawnStack, long tankCapacity, @Nullable Quaternionf rotation) {
        var fluid = drawnStack.getFluid();
        var fill = drawnStack.getAmount() / (float) tankCapacity;

        var sprite = RenderHelpers.getFluidSprite(fluid);
        var color = ColorHelper.makeOpaque(ColorHelper.getFluidTint(drawnStack));

        return new FluidCube(min, size, fill, sprite, color, rotation);
    }

    public record TaintedRefineryFluidData(List<FluidCube> cubes) {
    }
}
