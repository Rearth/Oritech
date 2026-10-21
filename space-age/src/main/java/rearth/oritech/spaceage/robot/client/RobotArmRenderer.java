package rearth.oritech.spaceage.robot.client;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.robot.RobotArmBlockEntity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public final class RobotArmRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<RobotArmBlockEntity, R> {

    private static final BlockDisplayContext BLOCK_CONTEXT = BlockDisplayContext.create();
    private static final DataTicket<Frame> FRAME = DataTicket.create("robot_arm_frame", Frame.class);
    private static final Vector3f WARP = new Vector3f(8, 22, -5);
    private static final Vector3f DROP = new Vector3f(8, 47, -5);
    private final Map<RobotArmBlockEntity, Motion> motions = new WeakHashMap<>();

    public RobotArmRenderer(BlockEntityRendererProvider.Context context) {

        super(context, new Model());
        withRenderLayer(new HeldLayer<>(this));
    }

    @Override
    public void addRenderData(RobotArmBlockEntity arm, Void related, R state, float partialTick) {

        super.addRenderData(arm, related, state, partialTick);
        var visual = arm.visual;
        var time = arm.getLevel().getGameTime() + partialTick;
        var motion = motions.computeIfAbsent(arm, ignored -> new Motion());
        var seconds = motion.lastTime < 0 ? .05f : (float) Math.clamp((time - motion.lastTime) / 20, 0, .1);
        motion.lastTime = time;
        if (motion.phase != visual.phase() || !motion.actionTarget.equals(visual.target())) {
            motion.elapsed = visual.elapsed();
            motion.actionTarget = visual.target();
        } else if (visual.active()) {
            motion.elapsed += seconds * 20;
            // Advance on the render clock; server tick timestamps can lead the client clock.
            motion.elapsed += (visual.elapsed() - motion.elapsed) * (1 - (float) Math.exp(-seconds * 3));
        }
        var progress = Math.clamp(motion.elapsed / visual.duration(), 0, 1);
        var target = modelPosition(arm, visual.target());
        var start = visual.start().equals(arm.getBlockPos()) ? new Vector3f(DROP) : modelPosition(arm, visual.start());
        var placing = visual.kind() == 0;
        var pickup = new Vector3f(DROP).lerp(WARP, RobotArmIk.ease(progress / .2f));
        var destination = switch (visual.phase()) {
            case 1 -> placing && progress < .2f ? pickup : RobotArmIk.travel(placing ? WARP : start, target, placing ? (progress - .2f) / .8f : progress);
            case 2 -> target;
            case 3 -> visual.returnToWarp() ? RobotArmIk.travel(target, DROP, progress / .8f) : target;
            default -> start;
        };
        var rotation = placing ? new Quaternionf().rotationY(-facingYaw(arm) * RobotArmIk.ease((progress - .2f) / .8f)).rotateX((float) Math.PI) : null;
        if (placing && visual.phase() == 2) rotation.rotationY(-facingYaw(arm)).rotateX((float) Math.PI);
        if (placing && visual.phase() == 3) rotation.rotationY(-facingYaw(arm) * (1 - RobotArmIk.ease(progress / .8f))).rotateX((float) Math.PI);
        if (!placing && visual.phase() == 3 && visual.returnToWarp()) {
            if (motion.phase != 3 && motion.pose != null) motion.cargoRotation.set(RobotArmIk.orientation(motion.pose)).rotateY(motion.spin);
            rotation = new Quaternionf(motion.cargoRotation).slerp(new Quaternionf().rotationX((float) Math.PI), RobotArmIk.ease(progress / .8f));
        }
        // The wrist, not the cargo pivot, supplies the required endpoint orientation.
        motion.target.lerp(destination, 1 - (float) Math.exp(-seconds * 24));
        var pose = RobotArmIk.interpolate(motion.pose, motion.ik.solve(motion.target, rotation), seconds);
        motion.pose = pose;
        motion.phase = visual.phase();
        var closure = visual.phase() == 0 ? 0 : visual.kind() == 1 ? 7 : visual.kind() == 2 ? 8 : 0;
        motion.closure += (closure - motion.closure) * (1 - (float) Math.exp(-seconds * 12));
        if (visual.kind() == 1 && visual.phase() == 2 && visual.active()) motion.spin += seconds * 16;
        var item = new ItemStackRenderState();
        var block = new BlockModelRenderState();
        var visible = !visual.held().isEmpty() && (visual.kind() == 0 && (visual.phase() == 1 && progress >= .2f || visual.phase() == 2)
                || visual.kind() == 2 && (visual.phase() == 1 || visual.phase() == 2) || visual.phase() == 3);
        if (visible) {
            var minecraft = Minecraft.getInstance();
            if (visual.held().getItem() instanceof BlockItem blockItem) {
                minecraft.getBlockModelResolver().update(block, blockItem.getBlock().defaultBlockState(), BLOCK_CONTEXT);
            } else {
                minecraft.getItemModelResolver().updateForTopItem(item, visual.held(), ItemDisplayContext.NONE, arm.getLevel(), null, 0);
            }
        }
        var orientation = RobotArmIk.orientation(pose).rotateY(rotation != null ? 0 : motion.spin);
        var sink = visual.phase() == 3 && visual.returnToWarp() ? -25 * RobotArmIk.ease((progress - .8f) / .2f) : 0;
        var offset = orientation.invert().transform(new Vector3f(0, sink / 16, 0));
        state.addGeckolibData(FRAME, new Frame(pose, motion.closure, rotation != null ? 0 : motion.spin, offset, block, item));
    }

    private static float facingYaw(RobotArmBlockEntity arm) {

        return switch (arm.getFacingForMultiblock()) {
            case SOUTH -> (float) Math.PI;
            case WEST -> (float) Math.PI / 2;
            case EAST -> (float) -Math.PI / 2;
            default -> 0;
        };
    }

    private static Vector3f modelPosition(RobotArmBlockEntity arm, BlockPos pos) {

        var delta = pos.getCenter().subtract(arm.getBlockPos().getX() + .5, arm.getBlockPos().getY(), arm.getBlockPos().getZ() + .5);
        return new Vector3f((float) delta.x * 16, (float) delta.y * 16, (float) delta.z * 16).rotateY(-facingYaw(arm));
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {

        var frame = pass.getGeckolibData(FRAME);
        if (frame == null) return;
        for (int i = 0; i < RobotArmIk.BONES.length; i++) {
            var angle = frame.pose.angles()[i];
            var axis = RobotArmIk.AXES[i];
            bones.ifPresent(RobotArmIk.BONES[i], bone -> bone.setRotation(axis == 'x' ? angle : 0, axis == 'y' ? angle : 0, axis == 'z' ? angle : 0));
        }
        var extension = frame.pose.extension() / 3;
        bones.ifPresent("extendedY", bone -> bone.setTranslateY(extension));
        bones.ifPresent("piston", bone -> bone.setScaleY(1 + extension / 7));
        bones.ifPresent("extendedY2", bone -> bone.setTranslateY(extension));
        bones.ifPresent("piston2", bone -> bone.setScaleY(1 + extension / 6));
        bones.ifPresent("extendedY3", bone -> bone.setTranslateY(extension));
        bones.ifPresent("piston3", bone -> bone.setScaleY(1 + extension / 8));
        bones.ifPresent("left", bone -> bone.setTranslateZ(frame.closure));
        bones.ifPresent("right", bone -> bone.setTranslateZ(-frame.closure));
        bones.ifPresent("rotY5", bone -> bone.setRotY(frame.spin));
        if (frame.pose.wrist() != null) {
            var wrist = frame.pose.wrist();
            bones.ifPresent("rotX4", bone -> bone.setRotation(wrist.x, wrist.y, wrist.z));
        }
    }

    @Override
    public AABB getRenderBoundingBox(RobotArmBlockEntity arm) {

        return RobotArmBlockEntity.workBounds(arm.getBlockPos()).inflate(2);
    }

    private static final class HeldLayer<R extends BlockEntityRenderState & GeoRenderState> extends GeoRenderLayer<RobotArmBlockEntity, Void, R> {

        HeldLayer(RobotArmRenderer<R> renderer) {

            super(renderer);
        }

        @Override
        public void addPerBoneRender(RenderPassInfo<R> pass, BiConsumer<GeoBone, PerBoneRender<R>> consumer) {

            var frame = pass.getGeckolibData(FRAME);
            if (frame == null || !pass.willRender()) return;
            pass.model().getBone("heldBlockPivot").ifPresent(bone -> consumer.accept(bone, (info, ignored, tasks) -> {
                var stack = info.poseStack();
                stack.pushPose();
                stack.translate(frame.offset.x, frame.offset.y, frame.offset.z);
                // Fixed mounting orientation: the fingers point down, the carried block stays upright.
                stack.mulPose(Axis.XP.rotation((float) Math.PI));
                if (!frame.block.isEmpty()) {
                    stack.translate(-.5, -.5, -.5);
                    frame.block.submit(stack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                } else {
                    frame.item.submit(stack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                }
                stack.popPose();
            }));
        }
    }

    private static final class Model extends GeoModel<RobotArmBlockEntity> {

        @Override
        public Identifier getModelResource(GeoRenderState state) { return OritechSpaceAge.id("robot_arm"); }

        @Override
        public Identifier getTextureResource(GeoRenderState state) { return OritechSpaceAge.id("textures/models/robot_arm.png"); }

        @Override
        public Identifier getAnimationResource(RobotArmBlockEntity arm) { return OritechSpaceAge.id("robot_arm"); }
    }

    private static final class Motion {

        final RobotArmIk ik = new RobotArmIk();
        final Vector3f target = new Vector3f(DROP);
        BlockPos actionTarget = BlockPos.ZERO;
        float elapsed;
        final Quaternionf cargoRotation = new Quaternionf();
        RobotArmIk.Pose pose;
        int phase;
        double lastTime = -1;
        float closure;
        float spin;
    }

    private record Frame(RobotArmIk.Pose pose, float closure, float spin, Vector3f offset, BlockModelRenderState block, ItemStackRenderState item) {}
}
