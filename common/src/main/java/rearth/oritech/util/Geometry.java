package rearth.oritech.util;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.minecraft.core.Direction.*;


public class Geometry {
    
    public static final float DEG_TO_RAD = 0.017453292519943295769236907684886f;
    
    public static Vec3i offsetToWorldPosition(Direction facing, Vec3i offset, Vec3i ownPos) {
        var rotated = rotatePosition(offset, facing);
        return ownPos.offset(rotated);
    }
    
    public static Vec3 worldToOffsetPosition(Direction facing, Vec3 worldTarget, Vec3 ownPos) {
        Vec3 relativeWorld = worldTarget.subtract(ownPos);
        
        double relX = relativeWorld.x();
        double relY = relativeWorld.y();
        double relZ = relativeWorld.z();
        
        if (Objects.requireNonNull(facing) == NORTH) {
            return new Vec3(-relX, relY, -relZ);
        } else if (facing == SOUTH) {
            return new Vec3(relX, relY, relZ);
        } else if (facing == WEST) {
            return new Vec3(relZ, relY, -relX);
        } else if (facing == EAST) {
            return new Vec3(-relZ, relY, relX);
        } else if (facing == UP) {
            return new Vec3(relX, -relZ, relY);
        } else if (facing == DOWN) {
            return new Vec3(relX, relZ, -relY);
        }
        throw new IllegalArgumentException();
        
    }
    
    public static Vec3i rotatePosition(Vec3i relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new BlockPos(relativePos.getZ(), relativePos.getY(), relativePos.getX());
            case WEST -> new BlockPos(relativePos.getX(), relativePos.getY(), -relativePos.getZ());
            case SOUTH -> new BlockPos(-relativePos.getZ(), relativePos.getY(), -relativePos.getX());
            case EAST -> new BlockPos(-relativePos.getX(), relativePos.getY(), relativePos.getZ());
            case UP -> new BlockPos(-relativePos.getZ(), -relativePos.getX(), -relativePos.getY());
            case DOWN -> new BlockPos(relativePos.getZ(), relativePos.getX(), relativePos.getY());
        };
    }
    public static Vec3 rotatePosition(Vec3 relativePos, Direction facing) {
        return switch (facing) {
            case NORTH -> new Vec3(relativePos.z(), relativePos.y(), relativePos.x());
            case WEST -> new Vec3(relativePos.x(), relativePos.y(), -relativePos.z());
            case SOUTH -> new Vec3(-relativePos.z(), relativePos.y(), -relativePos.x());
            case EAST -> new Vec3(-relativePos.x(), relativePos.y(), relativePos.z());
            case UP -> new Vec3(-relativePos.z(), -relativePos.x(), -relativePos.y());
            case DOWN -> new Vec3(relativePos.z(), relativePos.x(), relativePos.y());
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
    
    public static Vec3i getUp(Direction facing) {
        return rotatePosition(new Vec3i(0, 1, 0), facing);
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

    public static VoxelShape rotateVoxelShape(VoxelShape shape, Direction facing, AttachFace face) {

        var minX = shape.min(Axis.X);
        var maxX = shape.max(Axis.X);
        var minY = shape.min(Axis.Y);
        var maxY = shape.max(Axis.Y);
        var minZ = shape.min(Axis.Z);
        var maxZ = shape.max(Axis.Z);

        if (facing == NORTH) {
            if (face == AttachFace.FLOOR) return shape;
            if (face == AttachFace.WALL) 
                return Shapes.box(1 - maxX, 1 - maxZ, 1 - maxY, 1 - minX, 1 - minZ, 1 - minY);
            if (face == AttachFace.CEILING)
                return Shapes.box(minX, 1 - maxY, 1 - maxZ, maxX, 1 - minY, 1 - minZ);
        }

        if (facing == SOUTH) {
            if (face == AttachFace.FLOOR)
                return Shapes.box(1 - maxX, minY, 1 - maxZ, 1 - minX, maxY, 1 - minZ);
            if (face == AttachFace.WALL)
                return Shapes.box(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
            if (face == AttachFace.CEILING)
                return Shapes.box(1 - maxX, 1 - maxY, minZ, 1 - minX, 1 - minY, maxZ);

        }

        if (facing == Direction.EAST) {
            if (face == AttachFace.FLOOR)
                return Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX);
            if (face == AttachFace.WALL)
                return Shapes.box(minY, 1 - maxZ, 1 - maxX, maxY, 1 - minZ, 1 - minX);
            if (face == AttachFace.CEILING)
                return Shapes.box(minZ, 1 - maxY, minX, maxZ, 1 - minY, maxX);
        }

        if (facing == Direction.WEST) {
            if (face == AttachFace.FLOOR)
                return Shapes.box(minZ, minY, 1 - maxX, maxZ, maxY, 1 - minX);
            if (face == AttachFace.WALL)
                return Shapes.box(1 - maxY, 1 - maxZ, minX, 1 - minY, 1 - minZ, maxX);
            if (face == AttachFace.CEILING)
                return Shapes.box(1 - maxZ, 1 - maxY, 1 - maxX, 1 - minZ, 1 - minY, 1 - minX);
        }

        if (facing == Direction.UP) {
            return Shapes.box(minX, 1 - maxZ, minY, maxX, 1 - minZ, maxY);
        }

        if (facing == Direction.DOWN) {
            return Shapes.box(minX, minZ, minY, maxX, maxZ, maxY);
        }
                
        return shape;
    }
}
