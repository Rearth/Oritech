package rearth.oritech.spaceage.client.robot_arm;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.PerBoneRender;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.client.renderers.blocks.MachineRenderer;
import rearth.oritech.util.ColorableMachine;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlockEntity;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlockEntity.Phase;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlockEntity.Visual;
import rearth.oritech.spaceage.block.robot_arm.RobotArmReach;
import rearth.oritech.spaceage.block.robot_arm.RobotProgram.Kind;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

public final class RobotArmRenderer<R extends BlockEntityRenderState & GeoRenderState> extends GeoBlockRenderer<RobotArmBlockEntity, R> {

    private static final BlockDisplayContext BLOCK_CONTEXT = BlockDisplayContext.create();
    private static final DataTicket<Frame> FRAME = DataTicket.create("robot_arm_frame", Frame.class);
    private static final Vector3fc REST = new Vector3f(RobotArmReach.WARP).add(0, 12, 0);
    // Cargo returns above the port before sinking into it.
    private static final Vector3fc DROP = new Vector3f(8, 47, -5);
    private static final float HALF_TURN = (float) Math.PI;
    // Placing spends the first fifth of the approach picking the block up from the warp port.
    private static final float PICKUP = .2f;
    private final Map<RobotArmBlockEntity, Motion> motions = new WeakHashMap<>();

    public RobotArmRenderer(BlockEntityRendererProvider.Context context) {

        super(context, new Model());
        withRenderLayer(new HeldLayer<>(this));
    }

    private static Vector3f destination(RobotArmBlockEntity arm, Visual visual, float yaw, float progress) {

        var placing = visual.kind() == Kind.PLACE;
        var target = modelPosition(arm, visual.target(), yaw);
        return switch (visual.phase()) {
            case APPROACH -> {
                if (placing && progress < PICKUP)
                    yield new Vector3f(REST).lerp(RobotArmReach.WARP, RobotArmIk.ease(progress / PICKUP));
                if (placing) yield RobotArmIk.travel(RobotArmReach.WARP, target, (progress - PICKUP) / (1 - PICKUP));
                yield RobotArmIk.travel(start(arm, visual, yaw), target, progress);
            }
            case WORK -> target;
            case RETRACT -> visual.returnToWarp() ? RobotArmIk.travel(target, DROP, progress / .8f) : target;
            case PREPARE -> start(arm, visual, yaw);
        };
    }

    /**
     * Placed blocks turn from the warp port's facing to the arm's facing; returning cargo turns upright.
     */
    private static @Nullable Quaternionf graspRotation(Visual visual, Motion motion, float yaw, float progress) {

        if (visual.kind() == Kind.PLACE) {
            var turn = switch (visual.phase()) {
                case WORK -> 1;
                case RETRACT -> 1 - RobotArmIk.ease(progress / .8f);
                default -> RobotArmIk.ease((progress - PICKUP) / (1 - PICKUP));
            };
            return new Quaternionf().rotationY(-yaw * turn).rotateX(HALF_TURN);
        }
        if (visual.phase() != Phase.RETRACT || !visual.returnToWarp()) return null;
        if (motion.phase != Phase.RETRACT && motion.pose != null)
            motion.cargoRotation.set(RobotArmIk.orientation(motion.pose)).rotateY(motion.spin);
        return new Quaternionf(motion.cargoRotation).slerp(new Quaternionf().rotationX(HALF_TURN), RobotArmIk.ease(progress / .8f));
    }

    private static boolean holdsCargo(Visual visual, float progress) {

        if (visual.held().isEmpty()) return false;
        return switch (visual.phase()) {
            case APPROACH -> visual.kind() == Kind.INTERACT || visual.kind() == Kind.PLACE && progress >= PICKUP;
            case WORK -> visual.kind() != Kind.DESTROY;
            case RETRACT -> true;
            case PREPARE -> false;
        };
    }

    private static Vector3f start(RobotArmBlockEntity arm, Visual visual, float yaw) {

        return visual.start().equals(arm.getBlockPos()) ? new Vector3f(REST) : modelPosition(arm, visual.start(), yaw);
    }

    private static Vector3f modelPosition(RobotArmBlockEntity arm, BlockPos pos, float yaw) {

        var origin = arm.getBlockPos();
        var delta = pos.getCenter().subtract(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
        return new Vector3f((float) delta.x * 16, (float) delta.y * 16, (float) delta.z * 16).rotateY(-yaw);
    }

    @Override
    public void addRenderData(RobotArmBlockEntity arm, Void related, R state, float partialTick) {

        super.addRenderData(arm, related, state, partialTick);
        state.addGeckolibData(MachineRenderer.TEXTURE_OVERRIDE_TICKET, arm.getCurrentColor());
        // Render states are reused. A packaged/idle arm must not retain the previous IK frame.
        state.addGeckolibData(FRAME, null);
        if (!arm.isAssembled(arm.getBlockState()) || !arm.visual.active()) {
            motions.remove(arm);
            return;
        }
        var controller = arm.getAnimatableInstanceCache().getManagerForId(getInstanceId(arm, related)).getAnimationControllers().get("machine");
        // Authored deployment must finish before procedural motion takes over the joints.
        var animation = controller.getCurrentRawAnimation();
        if (RobotArmBlockEntity.PACKAGED.equals(animation) || RobotArmBlockEntity.SETUP.equals(animation)) {
            motions.remove(arm);
            return;
        }
        var visual = arm.visual;
        var motion = motions.computeIfAbsent(arm, ignored -> new Motion());
        var seconds = motion.advance(visual, arm.getLevel().getGameTime() + partialTick);
        var progress = Math.clamp(motion.elapsed / visual.duration(), 0, 1);
        var yaw = RobotArmReach.facingYaw(arm.getFacingForMultiblock());

        var rotation = graspRotation(visual, motion, yaw, progress);
        // The wrist, not the cargo pivot, supplies the required endpoint orientation.
        motion.target.lerp(destination(arm, visual, yaw, progress), 1 - (float) Math.exp(-seconds * 24));
        var pose = RobotArmIk.interpolate(motion.pose, motion.ik.solve(motion.target, rotation), seconds);
        motion.pose = pose;
        motion.phase = visual.phase();

        var closure = visual.phase() == Phase.PREPARE ? 0 : switch (visual.kind()) {
            case DESTROY -> 7;
            case INTERACT -> 8;
            case PLACE -> 0;
        };
        motion.closure += (closure - motion.closure) * (1 - (float) Math.exp(-seconds * 12));
        if (visual.kind() == Kind.DESTROY && visual.phase() == Phase.WORK && visual.active())
            motion.spin += seconds * 16;

        var spin = rotation != null ? 0 : motion.spin;
        Held held = null;
        if (holdsCargo(visual, progress)) {
            // Returning cargo sinks into the warp port at the very end of the retraction.
            var sink = visual.phase() == Phase.RETRACT && visual.returnToWarp() ? -25 * RobotArmIk.ease((progress - .8f) / .2f) : 0;
            var offset = sink == 0 ? new Vector3f() : RobotArmIk.orientation(pose).rotateY(spin).invert().transform(new Vector3f(0, sink / 16, 0));
            held = Held.of(arm, visual, offset);
        }
        state.addGeckolibData(FRAME, new Frame(pose, motion.closure, spin, held));
    }

    @Override
    public void applyAnimationControllers(RenderPassInfo<R> pass, BoneSnapshots bones) {

        // Authored clips start from the original model, including bones they don't keyframe.
        for (var name : pass.model().boneLookup().get().keySet()) {
            bones.ifPresent(name, bone -> bone.setScale(1, 1, 1).setRotation(0, 0, 0).setTranslation(0, 0, 0));
        }
        super.applyAnimationControllers(pass, bones);
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<R> pass, BoneSnapshots bones) {

        var frame = pass.getGeckolibData(FRAME);
        if (frame == null) return;
        var angles = frame.pose.angles();
        for (int i = 0; i < RobotArmIk.BONES.length; i++) {
            var angle = angles[i];
            var axis = RobotArmIk.AXES[i];
            bones.ifPresent(RobotArmIk.BONES[i], bone -> bone.setRotation(axis == 'x' ? angle : 0, axis == 'y' ? angle : 0, axis == 'z' ? angle : 0));
        }
        // Each piston carries a third of the extension; the housings stretch to stay connected.
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
        var wrist = frame.pose.wrist();
        if (wrist != null) bones.ifPresent("rotX4", bone -> bone.setRotation(wrist.x, wrist.y, wrist.z));
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
            if (frame == null || frame.held == null || !pass.willRender()) return;
            var held = frame.held;
            pass.model().getBone("heldBlockPivot").ifPresent(bone -> consumer.accept(bone, (info, ignored, tasks) -> {
                var stack = info.poseStack();
                stack.pushPose();
                stack.translate(held.offset.x, held.offset.y, held.offset.z);
                if (held.block != null) {
                    // The fingers point down while the carried block stays upright.
                    stack.mulPose(Axis.XP.rotation(HALF_TURN));
                    stack.translate(-.5, -.5, -.5);
                    held.block.submit(stack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                } else {
                    // Hand-held model transforms put the handle between the fingers
                    // and the working end along the gripper's local Y direction.
                    stack.mulPose(Axis.YP.rotation(HALF_TURN));
                    held.item.submit(stack, tasks, info.packedLight(), OverlayTexture.NO_OVERLAY, 0);
                }
                stack.popPose();
            }));
        }
    }

    private static final class Model extends GeoModel<RobotArmBlockEntity> {

        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return OritechSpaceAge.id("robot_arm");
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {

            var color = state.getOrDefaultGeckolibData(MachineRenderer.TEXTURE_OVERRIDE_TICKET, ColorableMachine.ColorVariant.ORANGE);
            return ColorableMachine.getTextureForColor(OritechSpaceAge.id("textures/models/robot_arm.png"), color);
        }

        @Override
        public Identifier getAnimationResource(RobotArmBlockEntity arm) {
            return OritechSpaceAge.id("robot_arm");
        }
    }

    /**
     * Client-side smoothing state per arm, advanced on the render clock.
     */
    private static final class Motion {

        final RobotArmIk ik = new RobotArmIk();
        final Vector3f target = new Vector3f(REST);
        final Quaternionf cargoRotation = new Quaternionf();
        BlockPos actionTarget = BlockPos.ZERO;
        Phase phase = Phase.PREPARE;
        RobotArmIk.Pose pose = ik.solve(target, null);
        double lastTime = -1;
        float elapsed;
        float closure;
        float spin;

        /**
         * Follows the server's phase progress and returns the frame time in seconds.
         */
        float advance(Visual visual, double time) {

            var seconds = lastTime < 0 ? .05f : (float) Math.clamp((time - lastTime) / 20, 0, .1);
            lastTime = time;
            if (phase != visual.phase() || !actionTarget.equals(visual.target())) {
                elapsed = visual.elapsed();
                actionTarget = visual.target();
            } else if (visual.active()) {
                elapsed += seconds * 20;
                // Advance on the render clock; server tick timestamps can lead the client clock.
                elapsed += (visual.elapsed() - elapsed) * (1 - (float) Math.exp(-seconds * 3));
            }
            return seconds;
        }
    }

    private record Held(Vector3f offset, @Nullable BlockModelRenderState block, @Nullable ItemStackRenderState item) {

        static Held of(RobotArmBlockEntity arm, Visual visual, Vector3f offset) {

            var minecraft = Minecraft.getInstance();
            if (visual.held().getItem() instanceof BlockItem blockItem) {
                var block = new BlockModelRenderState();
                minecraft.getBlockModelResolver().update(block, blockItem.getBlock().defaultBlockState(), BLOCK_CONTEXT);
                return new Held(offset, block, null);
            }
            var item = new ItemStackRenderState();
            minecraft.getItemModelResolver().updateForTopItem(item, visual.held(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, arm.getLevel(), null, 0);
            return new Held(offset, null, item);
        }
    }

    private record Frame(RobotArmIk.Pose pose, float closure, float spin, @Nullable Held held) {
    }
}
