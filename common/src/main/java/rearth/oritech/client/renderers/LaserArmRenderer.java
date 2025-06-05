package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.HashMap;
import java.util.Objects;

import static net.minecraft.client.render.RenderPhase.VIEW_OFFSET_Z_LAYERING;
import static net.minecraft.util.math.Direction.*;
import static net.minecraft.util.math.Direction.DOWN;

public class LaserArmRenderer<T extends LaserArmBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    public LaserArmRenderer(String modelPath) {
        super(new LaserArmModel<>(modelPath));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    
    // Modified RenderLayer.LINES
    public static final RenderLayer.MultiPhase CUSTOM_LINES = RenderLayer.of("lines", VertexFormats.LINES, VertexFormat.DrawMode.LINES, 1536, RenderLayer.MultiPhaseParameters.builder().program(RenderPhase.LINES_PROGRAM).layering(VIEW_OFFSET_Z_LAYERING).transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY).target(RenderPhase.ITEM_ENTITY_TARGET).writeMaskState(RenderPhase.ALL_MASK).cull(RenderPhase.DISABLE_CULLING).build(false));
    private static final HashMap<LaserArmBlockEntity, Vec3d> cachedOffsets = new HashMap<>();
    
    
    @Override
    public int getRenderDistance() {
        return 128;
    }
    
    @Override
    public boolean rendersOutsideBoundingBox(T blockEntity) {
        return true;
    }
    
    @Override
    public void postRender(MatrixStack matrices, T laserEntity, BakedGeoModel model, VertexConsumerProvider bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(matrices, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        if (laserEntity.getCurrentTarget() == null || !laserEntity.isFiring()) return;
        
        var facing = laserEntity.getCachedState().get(Properties.FACING);
        var startPos = laserEntity.laserHead;
        var startOffset = new Vec3d(0, 1.65f, 0);
        
        var targetPos = laserEntity.getVisualTarget();
        var targetBlock = laserEntity.getWorld().getBlockState(laserEntity.getCurrentTarget()).getBlock();
        if (laserEntity.isTargetingAtomicForge(targetBlock)) { // adjust so the beam end faces one of the corner pillars
            var moveX = 0.5;
            var moveZ = 0.5;
            if (startPos.x < targetPos.x) moveX = -0.5;
            if (startPos.z < targetPos.z) moveZ = -0.5;
            targetPos = targetPos.add(moveX, 0.5, moveZ);
        } else if (laserEntity.isTargetingDeepdrill(targetBlock)) {
            var offset = cachedOffsets.computeIfAbsent(laserEntity, id -> idToOffset(id.getPos(), 0.5f, laserEntity.getWorld(), laserEntity.getCurrentTarget()));
            targetPos = targetPos.add(offset);
        }
        
        if (laserEntity.lastRenderPosition == null) laserEntity.lastRenderPosition = targetPos;
        targetPos = lerp(laserEntity.lastRenderPosition, targetPos, 0.06f);
        laserEntity.lastRenderPosition = targetPos;
        
        var targetPosOffset = worldToOffsetPosition(facing, targetPos, startPos).add(startOffset);
        
        var forward = targetPos.subtract(startPos).normalize();
        if (!laserEntity.isTargetingEnergyContainer() && !laserEntity.isTargetingBuddingAmethyst() && laserEntity.getWorld().random.nextFloat() > 0.7)
            ParticleContent.LASER_BEAM_EFFECT.spawn(laserEntity.getWorld(), targetPos.add(0.5, 0, 0.5).subtract(forward.multiply(0.6)));
        
        var cross = forward.crossProduct(new Vec3d(0, 1, 0));
        
        matrices.push();
        var lineConsumer = bufferSource.getBuffer(CUSTOM_LINES);
        
        // to prevent line from becoming too big when further away, as the size seems to be in screen space
        var camPos = MinecraftClient.getInstance().cameraEntity.getPos();
        var camDist = camPos.subtract(startPos).length();
        var widthMultiplier = 1f;
        if (camDist > 20)
            widthMultiplier = (float) (camDist / 20f);
        RenderSystem.lineWidth((float) (Math.sin((laserEntity.getWorld().getTime() + partialTick) * 0.3) * 2 + 7) / widthMultiplier);
        
        // startOffset = new Vec3d(0, 2, 0);
        // targetPosOffset = new Vec3d(0, 5, 0);
        
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) startOffset.x, (float) startOffset.y, (float) startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(0, 1, 0);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) startOffset.x, (float) startOffset.y, (float) startOffset.z)
          .color(138, 242, 223, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        lineConsumer.vertex(matrices.peek().getPositionMatrix(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .color(19, 91, 80, 255)
          .light(packedLight)
          .overlay(packedOverlay)
          .normal((float) cross.x, (float) cross.y, (float) cross.z);
        
        matrices.pop();
    }
    
    public static Vec3d idToOffset(BlockPos source, float range, World world, BlockPos targetPos) {
        
        var drillFacing = world.getBlockState(targetPos).get(Properties.HORIZONTAL_FACING);
        var drillCenter = Geometry.rotatePosition(new Vec3d(1, 1.4, 0), drillFacing);
        
        var random = Random.create(source.asLong());
        return new Vec3d((random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range).add(drillCenter);
    }
    
    @Override
    protected void rotateBlock(Direction facing, MatrixStack poseStack) {
        if (Objects.requireNonNull(facing) == Direction.DOWN) {
            poseStack.translate(0,  1, 0);
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        } else if (facing == Direction.WEST) {
            poseStack.translate(0.5,  0.5, 0);
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
        } else if (facing == Direction.EAST) {
            poseStack.translate(-0.5,  0.5, 0);
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(270));
        } else if (facing == Direction.SOUTH) {
            poseStack.translate(0,  0.5, -0.5);
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        } else if (facing == Direction.NORTH) {
            poseStack.translate(0,  0.5, 0.5);
            poseStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90));
        }
    }
    
    public static Vec3d lerp(Vec3d a, Vec3d b, float f) {
        return new Vec3d(lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f));
    }
    
    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
    
    private static Vec3d worldToOffsetPosition(Direction facing, Vec3d worldTarget, Vec3d ownPos) {
        Vec3d relativeWorld = worldTarget.subtract(ownPos);
        
        double relX = relativeWorld.getX();
        double relY = relativeWorld.getY();
        double relZ = relativeWorld.getZ();
        
        if (Objects.requireNonNull(facing) == NORTH) {
            return new Vec3d(relX, -relZ, relY);
        } else if (facing == SOUTH) {
            return new Vec3d(relX, relZ, -relY);
        } else if (facing == WEST) {
            return new Vec3d(relY, -relX, relZ);
        } else if (facing == EAST) {
            return new Vec3d(-relY, relX, relZ);
        } else if (facing == UP) {
            return new Vec3d(relX, relY, relZ);
        } else if (facing == DOWN) {
            return new Vec3d(relX, -relY, -relZ);
        }
        throw new IllegalArgumentException();
        
    }
}


