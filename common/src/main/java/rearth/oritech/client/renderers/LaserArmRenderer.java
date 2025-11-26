package rearth.oritech.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
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

import static net.minecraft.client.renderer.RenderStateShard.VIEW_OFFSET_Z_LAYERING;
import static net.minecraft.core.Direction.*;
import static net.minecraft.core.Direction.DOWN;


public class LaserArmRenderer<T extends LaserArmBlockEntity & GeoAnimatable> extends GeoBlockRenderer<T> {
    
    public LaserArmRenderer(String modelPath) {
        super(new LaserArmModel<>(modelPath));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
    
    // Modified RenderLayer.LINES
    public static final RenderType.CompositeRenderType CUSTOM_LINES = RenderType.create("lines", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER).setLayeringState(VIEW_OFFSET_Z_LAYERING).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setOutputState(RenderStateShard.ITEM_ENTITY_TARGET).setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE).setCullState(RenderStateShard.NO_CULL).createCompositeState(false));
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
            targetPos = targetPos.add(moveX, 0.5, moveZ);
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
        
        var cross = forward.cross(new Vec3(0, 1, 0));
        
        matrices.pushPose();
        var lineConsumer = bufferSource.getBuffer(CUSTOM_LINES);
        
        // to prevent line from becoming too big when further away, as the size seems to be in screen space
        var camPos = Minecraft.getInstance().cameraEntity.position();
        var camDist = camPos.subtract(startPos).length();
        var widthMultiplier = 1f;
        if (camDist > 20)
            widthMultiplier = (float) (camDist / 20f);
        RenderSystem.lineWidth((float) (Math.sin((laserEntity.getLevel().getGameTime() + partialTick) * 0.3) * 2 + 7) / widthMultiplier);
        
        // startOffset = new Vec3d(0, 2, 0);
        // targetPosOffset = new Vec3d(0, 5, 0);
        
        lineConsumer.addVertex(matrices.last().pose(), (float) startOffset.x, (float) startOffset.y, (float) startOffset.z)
          .setColor(138, 242, 223, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal(0, 1, 0);
        lineConsumer.addVertex(matrices.last().pose(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .setColor(19, 91, 80, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal(1, 0, 0);
        
        // render a second one at right angle to first one
        lineConsumer.addVertex(matrices.last().pose(), (float) startOffset.x, (float) startOffset.y, (float) startOffset.z)
          .setColor(138, 242, 223, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal((float) cross.x, (float) cross.y, (float) cross.z);
        lineConsumer.addVertex(matrices.last().pose(), (float) targetPosOffset.x, (float) targetPosOffset.y, (float) targetPosOffset.z)
          .setColor(19, 91, 80, 255)
          .setLight(packedLight)
          .setOverlay(packedOverlay)
          .setNormal((float) cross.x, (float) cross.y, (float) cross.z);
        
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


