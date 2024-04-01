package rearth.oritech.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public class Geometry {
    
    public static final float DEG_TO_RAD = 0.017453292519943295769236907684886f;
    
    public static Vec3i offsetToWorldPosition(Direction facing, Vec3i offset, Vec3i ownPos) {
        var rotated = rotatePosition(offset, facing);
        return ownPos.add(rotated);
    }
    public static Vec3i rotatePosition(Vec3i relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(relativePos.getZ(), relativePos.getY(), relativePos.getX());
            case WEST -> new BlockPos(relativePos.getX(), relativePos.getY(), -relativePos.getZ());
            case SOUTH -> new BlockPos(-relativePos.getZ(), relativePos.getY(), -relativePos.getX());
            case EAST -> new BlockPos(-relativePos.getX(), relativePos.getY(), relativePos.getZ());
            case UP -> new BlockPos(relativePos.getZ(), -relativePos.getX(), -relativePos.getY());
            case DOWN -> new BlockPos(relativePos.getZ(), relativePos.getX(), relativePos.getY());
            default -> relativePos;
        };
    }
    
    
    
    public static Vec3i getForward(Direction facing) {
        return rotatePosition(BlockDirection.FORWARD.pos, facing);
    }
    
    public static Vec3i getBackward(Direction facing) {
        return rotatePosition(BlockDirection.BACKWARD.pos, facing);
    }
    
    public static Vec3i getRight(Direction facing) {
        return rotatePosition(BlockDirection.RIGHT.pos, facing);
    }
    
    public static Vec3i getLeft(Direction facing) {
        return rotatePosition(BlockDirection.LEFT.pos, facing);
    }
    
    public enum BlockDirection {
        FORWARD(new BlockPos(-1, 0, 0)),
        BACKWARD(new BlockPos(1, 0, 0)),
        LEFT(new BlockPos(0, 0, 1)),
        RIGHT(new BlockPos(0, 0, -1));
        
        public final BlockPos pos;
        
        BlockDirection(BlockPos blockPos) {
            pos = blockPos;
        }
    }    
}
