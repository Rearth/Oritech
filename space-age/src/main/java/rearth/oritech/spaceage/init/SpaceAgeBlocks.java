package rearth.oritech.spaceage.init;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.blocks.RocketAssemblerBlock;
import rearth.oritech.spaceage.block.blocks.RocketCouplingBlock;
import rearth.oritech.spaceage.block.blocks.RocketEngineBlock;
import rearth.oritech.spaceage.block.blocks.RocketPadBlock;

public final class SpaceAgeBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OritechSpaceAge.MOD_ID);

    public static final DeferredBlock<RocketAssemblerBlock> ROCKET_ASSEMBLER = BLOCKS.registerBlock(
            "rocket_assembler",
            RocketAssemblerBlock::new,
            () -> machineProperties().noOcclusion()
    );

    public static final DeferredBlock<RocketPadBlock> ROCKET_PAD = BLOCKS.registerBlock(
            "rocket_pad",
            RocketPadBlock::new,
            SpaceAgeBlocks::machineProperties
    );

    public static final DeferredBlock<RocketCouplingBlock> ROCKET_COUPLING = BLOCKS.registerBlock(
            "rocket_coupling",
            RocketCouplingBlock::new,
            SpaceAgeBlocks::machineProperties
    );

    public static final DeferredBlock<RocketEngineBlock> ROCKET_ENGINE_TIER_1 = registerRocketEngine(
            "rocket_engine_tier_1", RocketEngineBlock.Tier.TIER_1
    );
    public static final DeferredBlock<RocketEngineBlock> ROCKET_ENGINE_TIER_2 = registerRocketEngine(
            "rocket_engine_tier_2", RocketEngineBlock.Tier.TIER_2
    );
    public static final DeferredBlock<RocketEngineBlock> ROCKET_ENGINE_TIER_3 = registerRocketEngine(
            "rocket_engine_tier_3", RocketEngineBlock.Tier.TIER_3
    );

    private SpaceAgeBlocks() {
    }

    private static DeferredBlock<RocketEngineBlock> registerRocketEngine(String name, RocketEngineBlock.Tier tier) {
        return BLOCKS.registerBlock(name, properties -> new RocketEngineBlock(tier, properties), SpaceAgeBlocks::machineProperties);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F, 10.0F);
    }
}
