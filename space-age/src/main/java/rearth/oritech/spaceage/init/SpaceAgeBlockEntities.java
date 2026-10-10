package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.GroundStationBlockEntity;
import rearth.oritech.spaceage.block.VacuumCrafterBlockEntity;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerBlockEntity;

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

    private SpaceAgeBlockEntities() {
    }

}
