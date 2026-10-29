package rearth.oritech.spaceage.init;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.init.BlockContent.NoAutoDrop;
import rearth.oritech.init.BlockContent.NoBlockItem;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlock;
import rearth.oritech.spaceage.block.basic.AsteroidBlock;
import rearth.oritech.spaceage.block.basic.GalleryLightBlock;
import rearth.oritech.spaceage.block.basic.GlobeBlock;
import rearth.oritech.spaceage.block.basic.PlanetBlock;
import rearth.oritech.spaceage.block.basic.RocketCouplingBlock;
import rearth.oritech.spaceage.block.basic.RocketEngineBlock;
import rearth.oritech.spaceage.block.basic.RocketPadBlock;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureBlock;
import rearth.oritech.spaceage.block.cubesat.CubeSatBlock;
import rearth.oritech.spaceage.block.launch_ramp.LaunchRampBlock;
import rearth.oritech.spaceage.block.mission_control.MissionControlBlock;
import rearth.oritech.spaceage.block.mission_control.SurveyModuleBlock;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlock;
import rearth.oritech.spaceage.block.vacuum_crafter.BlockPairController;
import rearth.oritech.spaceage.block.vacuum_crafter.VacuumCrafterBlock;

public final class SpaceAgeBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OritechSpaceAge.MOD_ID);

    public static final DeferredBlock<RobotArmBlock> ROBOT_ARM = BLOCKS.registerBlock("robot_arm", RobotArmBlock::new, () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> EARTH = BLOCKS.registerBlock("earth", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> SUN = BLOCKS.registerBlock("sun", PlanetBlock::new,
            () -> machineProperties().noOcclusion().lightLevel(state -> 15));

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> MARS = BLOCKS.registerBlock("mars", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> LUNA = BLOCKS.registerBlock("luna", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> PHOBOS = BLOCKS.registerBlock("phobos", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> CERES = BLOCKS.registerBlock("ceres", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> VESTA = BLOCKS.registerBlock("vesta", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> NYX = BLOCKS.registerBlock("nyx", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_COAL_ORE = BLOCKS.registerSimpleBlock("superdense_coal_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_IRON_ORE = BLOCKS.registerSimpleBlock("superdense_iron_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_COPPER_ORE = BLOCKS.registerSimpleBlock("superdense_copper_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_GOLD_ORE = BLOCKS.registerSimpleBlock("superdense_gold_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_REDSTONE_ORE = BLOCKS.registerSimpleBlock("superdense_redstone_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_LAPIS_ORE = BLOCKS.registerSimpleBlock("superdense_lapis_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_DIAMOND_ORE = BLOCKS.registerSimpleBlock("superdense_diamond_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_EMERALD_ORE = BLOCKS.registerSimpleBlock("superdense_emerald_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_QUARTZ_ORE = BLOCKS.registerSimpleBlock("superdense_quartz_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_ANCIENT_DEBRIS_ORE = BLOCKS.registerSimpleBlock("superdense_ancient_debris_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_NICKEL_ORE = BLOCKS.registerSimpleBlock("superdense_nickel_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_PLATINUM_ORE = BLOCKS.registerSimpleBlock("superdense_platinum_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoAutoDrop
    public static final DeferredBlock<Block> SUPERDENSE_URANIUM_ORE = BLOCKS.registerSimpleBlock("superdense_uranium_ore",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).strength(12.0F, 30.0F).requiresCorrectToolForDrops());

    @NoBlockItem
    public static final DeferredBlock<PlanetBlock> ECHO = BLOCKS.registerBlock("echo", PlanetBlock::new,
            () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_EARTH = BLOCKS.registerBlock("mini_earth",
            GlobeBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_MARS = BLOCKS.registerBlock("mini_mars",
            GlobeBlock::new, () -> machineProperties().noOcclusion());

    public static final DeferredBlock<GlobeBlock> MINI_SUN = BLOCKS.registerBlock("mini_sun",
            GlobeBlock::new, () -> machineProperties().noOcclusion().lightLevel(state -> 15));

    public static final DeferredBlock<GalleryLightBlock> GALLERY_LIGHT = BLOCKS.registerBlock("gallery_light",
            GalleryLightBlock::new, () -> machineProperties().noOcclusion().lightLevel(state -> 15));

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
            BLOCKS.registerBlock("space_scanner", properties -> new SurveyModuleBlock(properties, SurveyModuleBlock.SCANNER_SHAPE), () -> machineProperties().noOcclusion());
    public static final DeferredBlock<SurveyModuleBlock> ANTENNA =
            BLOCKS.registerBlock("antenna", properties -> new SurveyModuleBlock(properties, SurveyModuleBlock.ANTENNA_SHAPE), () -> machineProperties().noOcclusion());

    public static final DeferredBlock<VacuumCrafterBlock> VACUUM_CRAFTER = BLOCKS.registerBlock(
            "vacuum_crafter", VacuumCrafterBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<TransparentBlock> CRYSTAL_GLASS = BLOCKS.registerBlock(
            "crystal_glass", TransparentBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1.5F, 6.0F).noOcclusion());

    public static final DeferredBlock<BeamApertureBlock> BEAM_APERTURE = BLOCKS.registerBlock(
            "beam_aperture", BeamApertureBlock::new, SpaceAgeBlocks::machineProperties);

    public static final DeferredBlock<BlockPairController> CARGO = BLOCKS.registerBlock(
            "cargo", BlockPairController::new, SpaceAgeBlocks::machineProperties);

    // Drops itself with its contents through a custom loot table.
    @NoAutoDrop
    public static final DeferredBlock<CubeSatBlock> CUBE_SAT = BLOCKS.registerBlock(
            "cube_sat", CubeSatBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(1.5F, 6.0F).noOcclusion());

    public static final DeferredBlock<LaunchRampBlock> LAUNCH_RAMP = BLOCKS.registerBlock(
            "launch_ramp", LaunchRampBlock::new, SpaceAgeBlocks::machineProperties);

    private SpaceAgeBlocks() {
    }

    private static DeferredBlock<RocketEngineBlock> registerRocketEngine(String name, RocketEngineBlock.Type type) {

        return BLOCKS.registerBlock(name, properties -> new RocketEngineBlock(type, properties), () -> machineProperties().noOcclusion());
    }

    private static BlockBehaviour.Properties machineProperties() {

        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F, 10.0F);
    }
}
