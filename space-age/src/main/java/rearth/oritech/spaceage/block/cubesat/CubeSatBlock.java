package rearth.oritech.spaceage.block.cubesat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.init.SpaceAgeComponents;
import rearth.oritech.spaceage.simulation.rocket.CubeSatData;

/**
 * A rocket compacted by the addon splicer. It can be built into another rocket or shot up by a particle launch ramp.
 */
public class CubeSatBlock extends Block implements EntityBlock {

    public CubeSatBlock(Properties properties) {

        super(properties);
    }

    public static ItemStack createStack(CubeSatData data) {

        var stack = new ItemStack(SpaceAgeBlocks.CUBE_SAT.get());
        stack.set(SpaceAgeComponents.CUBE_SAT.get(), data);
        if (!data.name().isBlank()) stack.set(DataComponents.CUSTOM_NAME, Component.literal(data.name()));
        return stack;
    }

    /**
     * The cube stored in a stack, named after the stack's current (anvil) name.
     */
    public static @Nullable CubeSatData data(ItemStack stack) {

        var data = stack.get(SpaceAgeComponents.CUBE_SAT.get());
        if (data == null) return null;
        var name = stack.getCustomName();
        return data.withName(name == null ? "" : name.getString());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return new CubeSatBlockEntity(pos, state);
    }
}
