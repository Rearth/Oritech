package rearth.oritech.util;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;

public class AnimatedBlockRotation {

    // Shared by rendering and collision: these two models are upright in Blockbench, unlike normal machines.
    public static Matrix4f getModelRotation(Direction facing, boolean hangarDoor, boolean rotated) {
        var transform = new Matrix4f();
        var quarterTurn = (float) (Math.PI / 2);

        if (hangarDoor && rotated && facing.getAxis().isVertical()) {
            transform.translate(0, 0, -1).translate(0.5f, 0, 0.5f)
                    .rotateY(quarterTurn).translate(-0.5f, 0, -0.5f);
        }

        switch (facing) {
            case DOWN -> transform.translate(0, 1, 0).rotateX(2 * quarterTurn);
            case WEST -> {
                transform.translate(0.5f, 0.5f, 0);
                if (hangarDoor) transform.rotateX(-quarterTurn);
                transform.rotateZ(quarterTurn);
            }
            case EAST -> {
                transform.translate(-0.5f, 0.5f, 0);
                if (hangarDoor) transform.rotateX(-quarterTurn);
                transform.rotateZ(3 * quarterTurn);
            }
            case SOUTH -> transform.translate(0, 0.5f, -0.5f).rotateX(quarterTurn);
            case NORTH -> transform.translate(0, 0.5f, 0.5f).rotateX(-quarterTurn);
            default -> { }
        }

        if (hangarDoor && rotated && facing.getAxis().isHorizontal()) transform.rotateY(quarterTurn);
        return transform;
    }
}
