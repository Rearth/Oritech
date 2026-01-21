package rearth.oritech.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.util.Geometry;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.HashMap;
import java.util.Objects;

import static net.minecraft.core.Direction.*;


public class LaserArmRenderer<T extends LaserArmBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    
    public LaserArmRenderer(String modelPath) {
        super(new LaserArmModel<>(modelPath));
    }
    
    public static final ResourceLocation BEAM_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    private static final Vec3 BEAM_START_OFFSET = new Vec3(0, 1.65, 0);
    
    public static final int GLOW_COLOR_START = 0x998AF2DF;
    public static final int GLOW_COLOR_END   = 0x99135B50;
    
    public static final int CORE_COLOR_START = BeamRenderer.color(200, 220, 255, 100);
    public static final int CORE_COLOR_END   = BeamRenderer.color(180, 230, 255, 100);
    
    private static final HashMap<Long, Vec3> cachedOffsets = new HashMap<>();
    
    @Override
    public int getViewDistance() {
        return 128;
    }
    
    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }
    
    @Override
    public void postRender(PoseStack matrices, T laserEntity, BakedGeoModel model, MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.postRender(matrices, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        
        if (laserEntity.getCurrentTarget() == null || !laserEntity.isFiring()) return;
        
        var facing = laserEntity.getBlockState().getValue(BlockStateProperties.FACING);
        var startPos = laserEntity.laserHead;
        var startOffset = new Vec3(0, 1.65f, 0);
        
        var targetPos = laserEntity.getVisualTarget();
        var targetBlock = laserEntity.getLevel().getBlockState(laserEntity.getCurrentTarget()).getBlock();
        if (laserEntity.isTargetingAtomicForge(targetBlock)) { // adjust so the beam end faces one of the corner pillars
            var moveX = 0.5;
            var moveZ = 0.5;
            if (startPos.x < targetPos.x) moveX = -0.5;
            if (startPos.z < targetPos.z) moveZ = -0.5;
            targetPos = targetPos.add(moveX, 0.2, moveZ);
        } else if (laserEntity.isTargetingDeepdrill(targetBlock)) {
            var offset = cachedOffsets.computeIfAbsent(laserEntity.getBlockPos().asLong(), id -> idToOffset(BlockPos.of(id), 0.5f, laserEntity.getLevel(), laserEntity.getCurrentTarget()));
            targetPos = targetPos.add(offset);
        }
        
        if (laserEntity.lastRenderPosition == null) laserEntity.lastRenderPosition = targetPos;
        targetPos = lerp(laserEntity.lastRenderPosition, targetPos, 0.06f);
        laserEntity.lastRenderPosition = targetPos;
        
        var targetPosOffset = worldToOffsetPosition(facing, targetPos, startPos).add(startOffset);
        
        var forward = targetPos.subtract(startPos).normalize();
        if (!laserEntity.isTargetingEnergyContainer() && !laserEntity.isTargetingBuddingAmethyst() && laserEntity.getLevel().random.nextFloat() > 0.7)
            ParticleContent.LASER_BEAM_EFFECT.spawn(laserEntity.getLevel(), targetPos.add(0.5, 0, 0.5).subtract(forward.scale(0.6)));
        
        
        matrices.pushPose();
        var beamConsumer = bufferSource.getBuffer(RenderType.eyes(BEAM_TEXTURE));
        
        float thickness = (float) (0.03f + Math.sin((laserEntity.getLevel().getGameTime() + partialTick) * 0.3) * 0.015f);
        
        var deltaVec = targetPosOffset.subtract(startOffset);
        
        // glowing core
        BeamRenderer.renderStraightBeam(
          matrices,
          beamConsumer,
          BEAM_START_OFFSET,
          deltaVec,
          thickness * 0.2f,
          LightTexture.FULL_BRIGHT,
          CORE_COLOR_START,
          CORE_COLOR_END
        );
        
        // outer
        BeamRenderer.renderStraightBeam(
          matrices,
          beamConsumer,
          BEAM_START_OFFSET,
          deltaVec,
          thickness,
          LightTexture.FULL_BRIGHT,
          GLOW_COLOR_START,
          GLOW_COLOR_END
        );
        
        matrices.popPose();
    }
    
    public static Vec3 idToOffset(BlockPos source, float range, Level world, BlockPos targetPos) {
        
        var drillFacing = world.getBlockState(targetPos).getValue(BlockStateProperties.HORIZONTAL_FACING);
        var drillCenter = Geometry.rotatePosition(new Vec3(1, 1.4, 0), drillFacing);
        
        var random = RandomSource.create(source.asLong());
        return new Vec3((random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range).add(drillCenter);
    }
    
    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        if (Objects.requireNonNull(facing) == Direction.DOWN) {
            poseStack.translate(0,  1, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        } else if (facing == Direction.WEST) {
            poseStack.translate(0.5,  0.5, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        } else if (facing == Direction.EAST) {
            poseStack.translate(-0.5,  0.5, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(270));
        } else if (facing == Direction.SOUTH) {
            poseStack.translate(0,  0.5, -0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else if (facing == Direction.NORTH) {
            poseStack.translate(0,  0.5, 0.5);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
        }
    }
    
    public static Vec3 lerp(Vec3 a, Vec3 b, float f) {
        return new Vec3(lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f));
    }
    
    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }
    
    private static Vec3 worldToOffsetPosition(Direction facing, Vec3 worldTarget, Vec3 ownPos) {
        Vec3 relativeWorld = worldTarget.subtract(ownPos);
        
        double relX = relativeWorld.x();
        double relY = relativeWorld.y();
        double relZ = relativeWorld.z();
        
        if (Objects.requireNonNull(facing) == NORTH) {
            return new Vec3(relX, -relZ, relY);
        } else if (facing == SOUTH) {
            return new Vec3(relX, relZ, -relY);
        } else if (facing == WEST) {
            return new Vec3(relY, -relX, relZ);
        } else if (facing == EAST) {
            return new Vec3(-relY, relX, relZ);
        } else if (facing == UP) {
            return new Vec3(relX, relY, relZ);
        } else if (facing == DOWN) {
            return new Vec3(relX, -relY, -relZ);
        }
        throw new IllegalArgumentException();
        
    }
}


