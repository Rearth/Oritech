package rearth.oritech.spaceage.robot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.robot.ActionCardItem;
import rearth.oritech.spaceage.robot.RobotArmBlockEntity;

public final class RobotWorkArea {

    private static final ContextKey<Vec3> CENTER = new ContextKey<>(OritechSpaceAge.id("robot_work_area"));

    private RobotWorkArea() {}

    public static void extract(ExtractLevelRenderStateEvent event) {

        var player = Minecraft.getInstance().player;
        if (player == null || !(player.getOffhandItem().getItem() instanceof ActionCardItem)) return;
        var program = player.getOffhandItem().get(SpaceAgeComponents.ROBOT_PROGRAM.get());
        if (program == null || program.binding().isEmpty()) return;
        var binding = program.binding().get();
        if (binding.dimension() != event.getLevel().dimension()) return;
        event.getRenderState().setRenderData(CENTER, binding.pos().above().getCenter().subtract(event.getCamera().position()));
    }

    public static void submit(SubmitCustomGeometryEvent event) {

        var center = event.getLevelRenderState().getRenderData(CENTER);
        if (center == null) return;
        event.getSubmitNodeCollector().submitCustomGeometry(event.getPoseStack(), RenderTypes.lines(), (pose, consumer) -> {
            for (int plane = 0; plane < 3; plane++) {
                for (int segment = 0; segment < 96; segment++) {
                    var a = point(plane, segment * Math.PI / 48).add(center.toVector3f());
                    var b = point(plane, (segment + 1) * Math.PI / 48).add(center.toVector3f());
                    var normal = new Vector3f(b).sub(a).normalize();
                    consumer.addVertex(pose, a.x, a.y, a.z).setColor(0xAA55DDFF).setNormal(pose, normal).setLineWidth(2);
                    consumer.addVertex(pose, b.x, b.y, b.z).setColor(0xAA55DDFF).setNormal(pose, normal).setLineWidth(2);
                }
            }
        });
    }

    private static Vector3f point(int plane, double angle) {

        var a = (float) Math.cos(angle) * RobotArmBlockEntity.RANGE;
        var b = (float) Math.sin(angle) * RobotArmBlockEntity.RANGE;
        return switch (plane) {
            case 0 -> new Vector3f(a, b, 0);
            case 1 -> new Vector3f(a, 0, b);
            default -> new Vector3f(0, a, b);
        };
    }
}
