package rearth.oritech.block.blocks.pipes.energy;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedDataType;
import rearth.oritech.block.blocks.pipes.GenericPipeDuctBlock;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;

public class SuperConductorDuctBlock extends GenericPipeDuctBlock {
    public SuperConductorDuctBlock(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getConnectionBlock() {
        return getNormalBlock();
    }

    @Override
    public BlockState getNormalBlock() {
        return BlockContent.SUPERCONDUCTOR_DUCT.get().defaultBlockState();
    }

    @Override
    public SavedDataType<GenericPipeInterfaceEntity.PipeNetworkData> getNetworkDataType() {
        return GenericPipeInterfaceEntity.PipeNetworkData.SUPERCONDUCTOR_TYPE;
    }

    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof SuperConductorDuctBlock || block instanceof SuperConductorBlock || block instanceof SuperConductorConnectionBlock;
    }

    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(Level level) {
        return SuperConductorBlock.SUPERCONDUCTOR_DATA.computeIfAbsent(level.dimension().identifier(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
