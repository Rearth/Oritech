package rearth.oritech.spaceage.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import rearth.oritech.spaceage.OritechSpaceAge;
import rearth.oritech.spaceage.block.entity.RocketAssemblerBlockEntity;

import java.util.function.Supplier;

public final class SpaceAgeBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, OritechSpaceAge.MOD_ID);

    public static final Supplier<BlockEntityType<RocketAssemblerBlockEntity>> ROCKET_ASSEMBLER =
            BLOCK_ENTITY_TYPES.register("rocket_assembler", () -> new BlockEntityType<>(
                    RocketAssemblerBlockEntity::new,
                    SpaceAgeBlocks.ROCKET_ASSEMBLER.get()
            ));

    private SpaceAgeBlockEntities() {
    }
}
