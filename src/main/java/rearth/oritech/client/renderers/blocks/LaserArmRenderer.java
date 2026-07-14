package rearth.oritech.client.renderers.blocks;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.interaction.LaserArmBlockEntity;
import rearth.oritech.client.renderers.models.LaserArmModel;
import rearth.oritech.client.renderers.util.BeamRenderer;
import rearth.oritech.client.renderers.util.RenderHelpers;
import rearth.oritech.util.Geometry;

import java.util.HashMap;
import java.util.Objects;

public class LaserArmRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<LaserArmBlockEntity, R> {

    private Vec3 lastActivePlayerPos = Vec3.ZERO;
    private static final HashMap<Long, Vec3> drillOffsets = new HashMap<>();

    public static final DataTicket<org.joml.Vector2f> LASER_ANGLES = DataTicket.create("laser_angles", org.joml.Vector2f.class);

    public AABB getRenderBoundingBox(LaserArmBlockEntity blockEntity) {
        return AABB.INFINITE;
    }

    private Vec3 getOffsetByDrillId(long id, LaserArmBlockEntity laserEntity) {
        return drillOffsets.computeIfAbsent(id, s -> {
            var drillFacing = laserEntity.getLevel().getBlockState(laserEntity.getCurrentTarget()).getValue(BlockStateProperties.HORIZONTAL_FACING);
            return Geometry.rotatePosition(new Vec3(1, 1.4, 0), drillFacing);
        });
    }

    private Vec3 getIdleTarget(LaserArmBlockEntity entity) {

        var offsetA = new Vec3(0, Math.pow(Math.sin(entity.getLevel().getGameTime() / 40f), 3), 0);
        var offsetB = new Vec3(Math.pow(Math.sin(entity.getLevel().getGameTime() / 40f + 1.3f), 3), 0, 0);

        if (entity.getLevel().getRandom().nextFloat() > 0.9f) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                lastActivePlayerPos = player.getEyePosition();
            }
        }

        if (lastActivePlayerPos.equals(Vec3.ZERO))
            return Vec3.ZERO;

        return lastActivePlayerPos.add(offsetA).add(offsetB);
    }

    public static float determinant(org.joml.Vector2f a, org.joml.Vector2f b) {
        return a.x * b.y - a.y * b.x;
    }

    public static final Identifier BEAM_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final double BEAM_START_DISTANCE = 1.65;

    public static final int GLOW_COLOR_START = 0x998AF2DF;
    public static final int GLOW_COLOR_END = 0x99135B50;

    public static final int CORE_COLOR_START = BeamRenderer.color(200, 220, 255, 100);
    public static final int CORE_COLOR_END = BeamRenderer.color(180, 230, 255, 100);

    private static final HashMap<Long, Vec3> cachedOffsets = new HashMap<>();

    public static final DataTicket<LaserBeamData> BEAM_DATA = DataTicket.create("laser_beam_data", LaserBeamData.class);

    public LaserArmRenderer(BlockEntityRendererProvider.Context context, String modelPath) {
        super(context, new LaserArmModel<>(modelPath));
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void addRenderData(LaserArmBlockEntity laserEntity, @Nullable Void relatedObject, R renderState, float partialTick) {
        super.addRenderData(laserEntity, relatedObject, renderState, partialTick);

        // calculate custom angles first!
        Vec3 target;
        var isIdle = false;
        if (laserEntity.getCurrentTarget() == null || laserEntity.getCurrentTarget().closerThan(BlockPos.ZERO, 0.1f)) {
            target = getIdleTarget(laserEntity);
            isIdle = true;
        } else {
            target = laserEntity.getVisualTarget();
        }

        if (target != null && !target.equals(Vec3.ZERO)) {
            var targetBlock = laserEntity.getLevel().getBlockState(laserEntity.getCurrentTarget() != null ? laserEntity.getCurrentTarget() : BlockPos.ZERO).getBlock();
            var startPos = laserEntity.laserHead;

            if (laserEntity.isTargetingAtomicForge(targetBlock)) { // adjust so the beam end faces one of the corner pillars
                var moveX = 0.5;
                var moveZ = 0.5;
                if (startPos.x < target.x) moveX = -0.5;
                if (startPos.z < target.z) moveZ = -0.5;
                target = target.add(moveX, 0.2, moveZ);
            } else if (!isIdle && laserEntity.isTargetingDeepdrill(targetBlock)) {
                var drillId = laserEntity.getCurrentTarget().asLong();
                var offset = getOffsetByDrillId(drillId, laserEntity);
                target = target.add(offset);
            }

            var ownPos = laserEntity.laserHead;
            var facing = laserEntity.getBlockState().getValue(BlockStateProperties.FACING);
            var offset = Geometry.worldToOffsetPosition(facing, target, ownPos);

            // thanks to: https://math.stackexchange.com/questions/878785/how-to-find-an-angle-in-range0-360-between-2-vectors
            var offsetY = new org.joml.Vector2f((float) offset.x(), (float) offset.y());
            var forwardY = new org.joml.Vector2f(0, -1);
            if (facing == Direction.NORTH)
                forwardY = new org.joml.Vector2f(0, 1);
            if (facing == Direction.WEST)
                forwardY = new org.joml.Vector2f(1, 0);
            if (facing == Direction.EAST)
                forwardY = new org.joml.Vector2f(-1, 0);
            var angleY = -offsetY.angle(forwardY);

            // to create a 2d vector in a plane based on normal angleY
            var lengthY = offsetY.length();
            var heightDiff = offset.z();

            var offsetX = new org.joml.Vector2f(lengthY, (float) heightDiff);
            var forwardX = new org.joml.Vector2f(0, 1);
            var detX = determinant(offsetX, forwardX);
            var dotX = offsetX.dot(forwardX);
            var angleX = Math.atan2(detX, dotX);

            angleX -= 47.5 * Geometry.DEG_TO_RAD; //to offset for parent bone rotations

            laserEntity.lastLaserRotX = LaserArmModel.lerp(laserEntity.lastLaserRotX, (float) angleX, 0.06f);
            laserEntity.lastLaserRotY = LaserArmModel.lerp(laserEntity.lastLaserRotY, angleY, 0.06f);
            renderState.addGeckolibData(LASER_ANGLES, new org.joml.Vector2f(laserEntity.lastLaserRotX, laserEntity.lastLaserRotY));
        }

        if (laserEntity.getCurrentTarget() == null || !laserEntity.isFiring()) return;

        var startPos = laserEntity.laserHead;

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

        var forward = targetPos.subtract(startPos).normalize();
        if (!laserEntity.isTargetingEnergyContainer() && !laserEntity.isTargetingBuddingAmethyst() && laserEntity.getLevel().getRandom().nextFloat() > 0.7) {
            var level = laserEntity.getLevel();
            var p = targetPos.add(0.5, 0, 0.5).subtract(forward.scale(0.6));
            level.addParticle(ParticleTypes.SMALL_FLAME, p.x + (level.getRandom().nextDouble() - 0.5) * 0.8, p.y + (level.getRandom().nextDouble() - 0.5) * 0.6, p.z + (level.getRandom().nextDouble() - 0.5) * 0.8, 0, 0, 0);
        }

        float thickness = (float) (0.03f + Math.sin((laserEntity.getLevel().getGameTime() + partialTick) * 0.3) * 0.015f);
        var facing = laserEntity.getBlockState().getValue(BlockStateProperties.FACING);
        var beamStart = Vec3.atCenterOf(laserEntity.getBlockPos())
                .add(Vec3.atLowerCornerOf(facing.getUnitVec3i()).scale(1.15));
        var blockOrigin = Vec3.atLowerCornerOf(laserEntity.getBlockPos());
        var localStart = beamStart.subtract(blockOrigin);
        var deltaVec = targetPos.subtract(beamStart);

        // postRenderPass runs after GeckoLib has popped the model-facing transform. Store the
        // beam in block-local world axes so wall and ceiling mounted lasers still point correctly.
        renderState.addGeckolibData(BEAM_DATA, new LaserBeamData(localStart, deltaVec, thickness));
    }

    @Override
    public void postRenderPass(RenderPassInfo<R> renderPassInfo, SubmitNodeCollector renderTasks) {
        super.postRenderPass(renderPassInfo, renderTasks);

        var data = renderPassInfo.getGeckolibData(BEAM_DATA);
        if (data == null) return;

        renderTasks.submitCustomGeometry(renderPassInfo.poseStack(), RenderTypes.eyes(BEAM_TEXTURE), (pose, consumer) -> {
            // glowing core
            BeamRenderer.renderStraightBeam(
                    pose,
                    consumer,
                    data.startOffset,
                    data.deltaVec,
                    data.thickness * 0.2f,
                    RenderHelpers.FULL_BRIGHT,
                    CORE_COLOR_START,
                    CORE_COLOR_END
            );

            // outer
            BeamRenderer.renderStraightBeam(
                    pose,
                    consumer,
                    data.startOffset,
                    data.deltaVec,
                    data.thickness,
                    RenderHelpers.FULL_BRIGHT,
                    GLOW_COLOR_START,
                    GLOW_COLOR_END
            );
        });
    }

    public static Vec3 idToOffset(BlockPos source, float range, Level level, BlockPos targetPos) {

        var drillFacing = level.getBlockState(targetPos).getValue(BlockStateProperties.HORIZONTAL_FACING);
        var drillCenter = Geometry.rotatePosition(new Vec3(1, 1.4, 0), drillFacing);

        var random = RandomSource.create(source.asLong());
        return new Vec3((random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range, (random.nextFloat() * 2 - 1) * range).add(drillCenter);
    }

    @Override
    protected void tryRotateByBlockstate(RenderPassInfo<R> renderPassInfo, PoseStack poseStack) {
        // Since tryRotateByBlockstate handles facing in the same way as standard rotateBlock, we override it
        final Direction facing = renderPassInfo.getOrDefaultGeckolibData(DIRECTION_FACING, Direction.NORTH);
        if (Objects.requireNonNull(facing) == Direction.DOWN) {
            poseStack.translate(0, 1, 0);
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        } else if (facing == Direction.WEST) {
            poseStack.translate(0.5, 0.5, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        } else if (facing == Direction.EAST) {
            poseStack.translate(-0.5, 0.5, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(270));
        } else if (facing == Direction.SOUTH) {
            poseStack.translate(0, 0.5, -0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else if (facing == Direction.NORTH) {
            poseStack.translate(0, 0.5, 0.5);
            poseStack.mulPose(Axis.XN.rotationDegrees(90));
        }
    }

    public static Vec3 lerp(Vec3 a, Vec3 b, float f) {
        return new Vec3(lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f));
    }

    public static double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> renderPassInfo, BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots);

        var angles = renderPassInfo.getGeckolibData(LASER_ANGLES);
        if (angles == null) return;

        var targetAngleX = angles.x();
        var targetAngleY = angles.y();

        boneSnapshots.ifPresent("pivotX", snapshot -> snapshot.setRotX(targetAngleX));
        boneSnapshots.ifPresent("pivotY", snapshot -> snapshot.setRotY(targetAngleY));
    }

    public static class LaserBeamData {
        public final Vec3 startOffset;
        public final Vec3 deltaVec;
        public final float thickness;

        public LaserBeamData(Vec3 startOffset, Vec3 deltaVec, float thickness) {
            this.startOffset = startOffset;
            this.deltaVec = deltaVec;
            this.thickness = thickness;
        }
    }
}
