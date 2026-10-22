package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlockEntity;
import rearth.oritech.spaceage.block.beam_aperture.BeamApertureBlockEntity;
import rearth.oritech.spaceage.block.mission_control.GroundStationBlockEntity;
import rearth.oritech.spaceage.block.robot_arm.RobotArmBlockEntity;
import rearth.oritech.spaceage.block.vacuum_crafter.VacuumCrafterBlockEntity;

import java.util.function.Supplier;

public final class SpaceAgeBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OritechSpaceAge.MOD_ID);

    public static final Supplier<BlockEntityType<RocketAssemblerBlockEntity>> ROCKET_ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("rocket_assembler", () -> new BlockEntityType<>(
                    RocketAssemblerBlockEntity::new,
                    SpaceAgeBlocks.ROCKET_ASSEMBLER.get()
            ));

    public static final Supplier<BlockEntityType<GroundStationBlockEntity>> GROUND_STATION =
            BLOCK_ENTITY_TYPES.register("ground_station", () -> new BlockEntityType<>(
                    GroundStationBlockEntity::new,
                    SpaceAgeBlocks.MISSION_CONTROL.get(), SpaceAgeBlocks.SPACE_SCANNER.get(), SpaceAgeBlocks.ANTENNA.get()));

    public static final Supplier<BlockEntityType<VacuumCrafterBlockEntity>> VACUUM_CRAFTER =
            BLOCK_ENTITY_TYPES.register("vacuum_crafter", () -> new BlockEntityType<>(
                    VacuumCrafterBlockEntity::new, SpaceAgeBlocks.VACUUM_CRAFTER.get(), SpaceAgeBlocks.CARGO.get()));

    public static final Supplier<BlockEntityType<BeamApertureBlockEntity>> BEAM_APERTURE =
            BLOCK_ENTITY_TYPES.register("beam_aperture", () -> new BlockEntityType<>(
                    BeamApertureBlockEntity::new, SpaceAgeBlocks.BEAM_APERTURE.get()));

    public static final Supplier<BlockEntityType<RobotArmBlockEntity>> ROBOT_ARM = BLOCK_ENTITY_TYPES.register("robot_arm", () -> new BlockEntityType<>(RobotArmBlockEntity::new, SpaceAgeBlocks.ROBOT_ARM.get()));

    private SpaceAgeBlockEntities() {
    }

}
