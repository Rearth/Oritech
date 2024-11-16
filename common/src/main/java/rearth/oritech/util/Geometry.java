package rearth.oritech.util;

import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.*;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import static net.minecraft.util.math.Direction.*;

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
    public static Vec3d rotatePosition(Vec3d relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3d(relativePos.getZ(), relativePos.getY(), relativePos.getX());
            case WEST -> new Vec3d(relativePos.getX(), relativePos.getY(), -relativePos.getZ());
            case SOUTH -> new Vec3d(-relativePos.getZ(), relativePos.getY(), -relativePos.getX());
            case EAST -> new Vec3d(-relativePos.getX(), relativePos.getY(), relativePos.getZ());
            case UP -> new Vec3d(relativePos.getZ(), -relativePos.getX(), -relativePos.getY());
            case DOWN -> new Vec3d(relativePos.getZ(), relativePos.getX(), relativePos.getY());
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
    
    public static Direction fromVector(Vec3i vector) {
        
        var x = vector.getX();
        var y = vector.getY();
        var z = vector.getZ();
        
        if (x == 0) {
            if (y == 0) {
                if (z > 0) {
                    return SOUTH;
                }
                
                if (z < 0) {
                    return NORTH;
                }
            } else if (z == 0) {
                if (y > 0) {
                    return UP;
                }
                
                return DOWN;
            }
        } else if (y == 0 && z == 0) {
            if (x > 0) {
                return EAST;
            }
            
            return WEST;
        }
        
        return null;
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

    public static VoxelShape rotateVoxelShape(VoxelShape shape, Direction facing, BlockFace face) {

        var minX = shape.getMin(Axis.X);
        var maxX = shape.getMax(Axis.X);
        var minY = shape.getMin(Axis.Y);
        var maxY = shape.getMax(Axis.Y);
        var minZ = shape.getMin(Axis.Z);
        var maxZ = shape.getMax(Axis.Z);

        if (facing == NORTH) {
            if (face == BlockFace.FLOOR) return shape;
            if (face == BlockFace.WALL) 
                return VoxelShapes.cuboid(1 - maxX, 1 - maxZ, 1 - maxY, 1 - minX, 1 - minZ, 1 - minY);
            if (face == BlockFace.CEILING)
                return VoxelShapes.cuboid(minX, 1 - maxY, 1 - maxZ, maxX, 1 - minY, 1 - minZ);
        }

        if (facing == SOUTH) {
            if (face == BlockFace.FLOOR)
                return VoxelShapes.cuboid(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
            if (face == BlockFace.WALL)
                return VoxelShapes.cuboid(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
            if (face == BlockFace.CEILING)
                return VoxelShapes.cuboid(1 - maxX, 1 - maxY, minZ, 1 - minX, 1 - minY, maxZ);

        }

        if (facing == Direction.EAST) {
            if (face == BlockFace.FLOOR)
                return VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
            if (face == BlockFace.WALL)
                return VoxelShapes.cuboid(minY, 1 - maxZ, 1 - maxX, maxY, 1 - minZ, 1 - minX);
            if (face == BlockFace.CEILING)
                return VoxelShapes.cuboid(minZ, 1 - maxY, minX, maxZ, 1 - minY, maxX);
        }

        if (facing == Direction.WEST) {
            if (face == BlockFace.FLOOR)
                return VoxelShapes.cuboid(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
            if (face == BlockFace.WALL)
                return VoxelShapes.cuboid(1 - maxY, 1 - maxZ, minX, 1 - minY, 1 - minZ, maxX);
            if (face == BlockFace.CEILING)
                return VoxelShapes.cuboid(1 - maxZ, 1 - maxY, 1 - maxX, 1 - minZ, 1 - minY, 1 - minX);
        }

        if (facing == Direction.UP) {
            return VoxelShapes.cuboid(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
        }

        if (facing == Direction.DOWN) {
            return VoxelShapes.cuboid(minX, minZ, minY, maxX, maxZ, maxY);
        }
                
        return shape;
    }
}
