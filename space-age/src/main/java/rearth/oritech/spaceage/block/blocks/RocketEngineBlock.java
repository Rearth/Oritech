package rearth.oritech.spaceage.block.blocks;

import net.minecraft.world.level.block.Block;

public class RocketEngineBlock extends Block {

    private final Tier tier;

    public RocketEngineBlock(Tier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    public enum Tier {
        TIER_1,
        TIER_2,
        TIER_3
    }
}
