package rearth.oritech.spaceage.init;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlock;
import rearth.oritech.spaceage.block.basic.RocketCouplingBlock;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.block.basic.RocketPadBlock;

// all models are placeholders for now
public final class SpaceAgeBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OritechSpaceAge.MOD_ID);

    public static final DeferredBlock<RocketAssemblerBlock> ROCKET_ASSEMBLER = BLOCKS.registerBlock("rocket_assembler", RocketAssemblerBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<RocketPadBlock> ROCKET_PAD = BLOCKS.registerBlock("rocket_pad", RocketPadBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<RocketCouplingBlock> ROCKET_COUPLING = BLOCKS.registerBlock("rocket_coupling", RocketCouplingBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<RocketEngineBlock> BASIC_BOOSTER_ROCKET = registerRocketEngine("basic_booster_rocket", RocketEngineBlock.Type.BASIC);
    public static final DeferredBlock<RocketEngineBlock> ION_BOOSTER_ROCKET = registerRocketEngine("ion_booster_rocket", RocketEngineBlock.Type.ION);

    private SpaceAgeBlocks() {
    }

    private static DeferredBlock<RocketEngineBlock> registerRocketEngine(String name, RocketEngineBlock.Type type) {
        return BLOCKS.registerBlock(name, properties -> new RocketEngineBlock(type, properties), () -> machineProperties().noOcclusion());
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F, 10.0F);
    }
}
