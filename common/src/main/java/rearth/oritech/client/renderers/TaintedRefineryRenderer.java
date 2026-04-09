package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.block.entity.processing.TaintedRefineryBlockEntity;
import rearth.oritech.util.ColorHelper;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class TaintedRefineryRenderer<T extends TaintedRefineryBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    
    public TaintedRefineryRenderer(String model) {
        super(new MachineModel<>(model));
    }
    
    // this overrides a method from IBlockEntityRendererExtension on NF. Since this extension mixin is not available in common, we just declare the methode without\
    // the override annotation
    public AABB getRenderBoundingBox(T blockEntity) {
        return AABB.ofSize(blockEntity.getBlockPos().getCenter(), 6, 6, 6);
    }
    
    @Override
    public void postRender(PoseStack poseStack, T animatable, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        var consumer = bufferSource.getBuffer(RenderType.translucent());
        
        var bigTank = animatable.ownStorage.getInStack();
        if (!bigTank.isEmpty()) {
            // render in stack
            renderFluidCube(new Vector3f(-24 / 16f, 3 / 16f, 11 / 16f), new Vector3f(12 / 16f, 24.5f / 16f, 28 / 16f), bigTank, animatable.ownStorage.getCapacity(), consumer, poseStack, packedLight, packedOverlay);
        }
        
        var smallTank = animatable.ownStorage.getOutStack();
        if (!smallTank.isEmpty()) {
            // bottom half of twisted tank
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZN.rotationDegrees(10));
            renderFluidCube(new Vector3f(-23.5f / 16f, 7 / 16f, -5 / 16f), new Vector3f(7 / 16f, 8 / 16f, 10 / 16f), smallTank, animatable.ownStorage.getCapacity(), consumer, poseStack, packedLight, packedOverlay);
            poseStack.popPose();
            
            // top half
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(7.5f));
            renderFluidCube(new Vector3f(-18.25f / 16f, 19 / 16f, -5 / 16f), new Vector3f(7 / 16f, 8 / 16f, 10 / 16f), smallTank, animatable.ownStorage.getCapacity(), consumer, poseStack, packedLight, packedOverlay);
            poseStack.popPose();
        }
        
    }
    
    private static void renderFluidCube(Vector3f min, Vector3f size, FluidStack drawnStack, Long tankCapacity, VertexConsumer consumer, PoseStack matrices, int light, int overlay) {
        var fluid = drawnStack.getFluid();
        var fill = drawnStack.getAmount() / (float) tankCapacity;
        
        var sprite = FluidStackHooks.getStillTexture(fluid);
        var spriteColor = ColorHelper.makeOpaque(FluidStackHooks.getColor(fluid));
        
        matrices.pushPose();
        matrices.translate(min.x + 0.01f, min.y + 0.01f, min.z + 0.01f);
        matrices.scale(size.x - 0.02f, size.y * fill - 0.03f, size.z - 0.02f);
        
        var entry = matrices.last();
        var modelMatrix = entry.pose();
        
        // draw cube from quads
        for (var direction : Direction.values()) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            SmallTankRenderer.drawQuad(direction, consumer, modelMatrix, entry, sprite, spriteColor, light, overlay);
        }
        
        matrices.popPose();
    }
}
