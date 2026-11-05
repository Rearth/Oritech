package rearth.oritech.spaceage.block.basic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RocketEngineBlock extends Block {

    private final Type type;

    public RocketEngineBlock(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    // Relative to the block center. Larger or directional engines can override these three values.
    public Vec3 exhaustOffset(BlockState state) {

        return new Vec3(0, -.5, 0);
    }

    public Vec3 exhaustDirection(BlockState state) {

        return new Vec3(0, -1, 0);
    }

    public float exhaustScale(BlockState state) {

        return 1;
    }

    public enum Type {
        BASIC,
        ION
    }
}
