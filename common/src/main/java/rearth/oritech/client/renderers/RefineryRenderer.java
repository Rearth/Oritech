package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import io.wispforest.owo.ui.core.Color;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.block.entity.processing.RefineryBlockEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RefineryRenderer<T extends RefineryBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    
    private final Map<Long, VisualTankHeights> tankHeights = new HashMap<>();
    
    public RefineryRenderer(String model) {
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
        // consumer = buffer;
        
        var lastHeight = tankHeights.computeIfAbsent(animatable.getBlockPos().asLong(), key -> new VisualTankHeights());
        
        var inputStack = animatable.ownStorage.getInStack();
        if (!inputStack.isEmpty()) {
            // render in stack
            renderFluidCube(new Vec3(-24 / 16f, 3 / 16f, 11 / 16f), new Vector3f(12 / 16f, 25 / 16f, 28 / 16f), inputStack, animatable.ownStorage.getCapacity(), consumer, poseStack, packedLight, packedOverlay, -1, lastHeight);
        }
        
        var moduleCount = animatable.getModuleCount();
        for (int i = 0; i <= moduleCount; i++) {
            var outputStorage = animatable.getOutputStorage(i);
            var renderedStack = outputStorage.getStack();
            if (renderedStack.isEmpty()) continue;
            // render storage
            
            var tankPosition = getTankCoordinates(i);
            renderFluidCube(tankPosition.getA(), tankPosition.getB(), renderedStack, outputStorage.getCapacity(), consumer, poseStack, packedLight, packedOverlay, i, lastHeight);
        }
        
    }
    
    private static Tuple<Vec3, Vector3f> getTankCoordinates(int i) {
        return switch (i) {
            case 0 -> new Tuple<>(new Vec3(-22 / 16f, 9 / 16f, -5 / 16f), new Vector3f(7 / 16f, 15 / 16f, 10 / 16f));
            case 1 ->
              new Tuple<>(new Vec3(-21 / 16f, 0 / 16f + 2, -5 / 16f), new Vector3f(26 / 16f, 14 / 16f, 26 / 16f));
            case 2 ->
              new Tuple<>(new Vec3(-21 / 16f, 0 / 16f + 3, -5 / 16f), new Vector3f(26 / 16f, 14 / 16f, 26 / 16f));
            default -> throw new IllegalStateException("Tried to access invalid tank for renderer: " + i);
        };
    }
    
    private static void renderFluidCube(Vec3 min, Vector3f size, FluidStack drawnStack, Long tankCapacity, VertexConsumer consumer, PoseStack matrices, int light, int overlay, int index, VisualTankHeights lastHeight) {
        var fluid = drawnStack.getFluid();
        var fill = drawnStack.getAmount() / (float) tankCapacity;
        
        var lastFill = index == -1 ? lastHeight.input : lastHeight.outputs[index];
        var newFill = Mth.lerp(0.003f, lastFill, fill);
        if (index == -1) {
            lastHeight.input = newFill;
        } else {
            lastHeight.outputs[index] = newFill;
        }
        
        var sprite = FluidStackHooks.getStillTexture(fluid);
        var spriteColor = FluidStackHooks.getColor(fluid);
        
        var parsedColor = Color.ofArgb(spriteColor);
        var opaqueColor = new Color(parsedColor.red(), parsedColor.green(), parsedColor.blue(), 1f);
        spriteColor = opaqueColor.argb();
        
        matrices.pushPose();
        matrices.translate(min.x + 0.01f, min.y + 0.01f, min.z + 0.01f);
        matrices.scale(size.x - 0.02f, size.y * newFill - 0.03f, size.z - 0.02f);
        
        var entry = matrices.last();
        var modelMatrix = entry.pose();
        
        // Draw the cube using quads
        for (var direction : Direction.values()) {
            if (direction.equals(Direction.DOWN)) continue; // skip bottom, as it's never visible
            SmallTankRenderer.drawQuad(direction, consumer, modelMatrix, entry, sprite, spriteColor, light, overlay);
        }
        
        matrices.popPose();
    }
    
    private static class VisualTankHeights {
        private float input = 0;
        private final float[] outputs = new float[3];
    }
}
