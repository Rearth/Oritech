package rearth.oritech.block.blocks.addons;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import rearth.oritech.util.ColorableMachine.ColorVariant;
import rearth.oritech.util.PaintableBlock;

public class PaintableMachineAddonBlock extends MachineAddonBlock implements PaintableBlock {
    public PaintableMachineAddonBlock(Properties properties, AddonSettings addonSettings) {
        super(properties, addonSettings);
        registerDefaultState(defaultBlockState().setValue(COLOR, ColorVariant.ORANGE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }
}
