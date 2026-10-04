package rearth.oritech.spaceage.block.basic;

import net.minecraft.world.level.block.Block;

public class RocketEngineBlock extends Block {

    private final Type type;

    public RocketEngineBlock(Type type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        BASIC,
        ION
    }
}
