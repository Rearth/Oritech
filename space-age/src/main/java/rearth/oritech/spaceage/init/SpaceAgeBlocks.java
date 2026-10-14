package rearth.oritech.spaceage.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.init.BlockContent.NoBlockItem;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.BlockPairController;
import rearth.oritech.spaceage.block.MissionControlBlock;
import rearth.oritech.spaceage.block.SurveyModuleBlock;
import rearth.oritech.spaceage.block.VacuumCrafterBlock;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlock;
import rearth.oritech.spaceage.block.basic.AsteroidBlock;
import rearth.oritech.spaceage.block.basic.GlobeBlock;
import rearth.oritech.spaceage.block.basic.PlanetBlock;
import rearth.oritech.spaceage.block.basic.RocketCouplingBlock;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.block.basic.RocketPadBlock;

public final class SpaceAgeBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OritechSpaceAge.MOD_ID);

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> EARTH = BLOCKS.registerBlock("earth", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> SUN = BLOCKS.registerBlock("sun", PlanetBlock::new,
            () -> machineProperties().noOcclusion().lightLevel(state -> 15));

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> MARS = BLOCKS.registerBlock("mars", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_EARTH = BLOCKS.registerBlock("mini_earth",
            GlobeBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_MARS = BLOCKS.registerBlock("mini_mars",
            GlobeBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_SUN = BLOCKS.registerBlock("mini_sun",
            GlobeBlock::new, () -> machineProperties().noOcclusion().lightLevel(state -> 15));

    public static final DeferredBlock<AsteroidBlock> ASTEROID_1 = BLOCKS.registerBlock("asteroid_1",
            properties -> new AsteroidBlock(properties, 1), () -> machineProperties().noOcclusion());

    public static final DeferredBlock<AsteroidBlock> ASTEROID_2 = BLOCKS.registerBlock("asteroid_2",
            properties -> new AsteroidBlock(properties, 2), () -> machineProperties().noOcclusion());

    public static final DeferredBlock<AsteroidBlock> ASTEROID_3 = BLOCKS.registerBlock("asteroid_3",
            properties -> new AsteroidBlock(properties, 3), () -> machineProperties().noOcclusion());

    public static final DeferredBlock<RocketAssemblerBlock> ROCKET_ASSEMBLER = BLOCKS.registerBlock("rocket_assembler", RocketAssemblerBlock::new,
            () -> machineProperties().noOcclusion());

    public static final DeferredBlock<RocketPadBlock> ROCKET_PAD = BLOCKS.registerBlock("rocket_pad", RocketPadBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<RocketCouplingBlock> ROCKET_COUPLING = BLOCKS.registerBlock("rocket_coupling", RocketCouplingBlock::new,
            () -> machineProperties().noOcclusion());

    public static final DeferredBlock<Block> ASTEROID_ANCHOR = BLOCKS.registerSimpleBlock("asteroid_anchor",
            () -> machineProperties().noOcclusion());

    public static final DeferredBlock<Block> NAVIGATION_COMPUTER = BLOCKS.registerSimpleBlock("navigation_computer", SpaceAgeBlocks::machineProperties);
    public static final DeferredBlock<Block> PARACHUTE = BLOCKS.registerSimpleBlock("parachute", SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<RocketEngineBlock> BASIC_BOOSTER_ROCKET = registerRocketEngine("basic_booster_rocket", RocketEngineBlock.Type.BASIC);
    public static final DeferredBlock<RocketEngineBlock> ION_BOOSTER_ROCKET = registerRocketEngine("ion_booster_rocket", RocketEngineBlock.Type.ION);

    public static final DeferredBlock<MissionControlBlock> MISSION_CONTROL =
            BLOCKS.registerBlock("mission_control", MissionControlBlock::new, SpaceAgeBlocks::machineProperties);
    public static final DeferredBlock<SurveyModuleBlock> SPACE_SCANNER =
            BLOCKS.registerBlock("space_scanner", SurveyModuleBlock::new, SpaceAgeBlocks::machineProperties);
    public static final DeferredBlock<SurveyModuleBlock> ANTENNA =
            BLOCKS.registerBlock("antenna", SurveyModuleBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<VacuumCrafterBlock> VACUUM_CRAFTER = BLOCKS.registerBlock(
            "vacuum_crafter", VacuumCrafterBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<BlockPairController> CARGO = BLOCKS.registerBlock(
            "cargo", BlockPairController::new, SpaceAgeBlocks::machineProperties);

    private SpaceAgeBlocks() {
    }

    private static DeferredBlock<RocketEngineBlock> registerRocketEngine(String name, RocketEngineBlock.Type type) {

        return BLOCKS.registerBlock(name, properties -> new RocketEngineBlock(type, properties), () -> machineProperties().noOcclusion());
    }

    private static BlockBehaviour.Properties machineProperties() {

        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F, 10.0F);
    }
}
