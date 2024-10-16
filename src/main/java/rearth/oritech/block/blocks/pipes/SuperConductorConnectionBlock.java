package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.pipes.EnergyPipeInterfaceEntity;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;
import team.reborn.energy.api.EnergyStorage;

import static rearth.oritech.block.blocks.pipes.SuperConductorBlock.SUPERCONDUCTOR_DATA;

public class SuperConductorConnectionBlock extends GenericPipeConnectionBlock {
    
    public SuperConductorConnectionBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return EnergyStorage.SIDED;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyPipeInterfaceEntity(pos, state);
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.SUPERCONDUCTOR_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.SUPERCONDUCTOR.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "superconductor";
    }
    
    @Override
    public boolean connectToOwnBlockType(Block block) {
        return block instanceof SuperConductorBlock || block instanceof SuperConductorConnectionBlock;
    }
    
    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.ENERGY_PIPE_CONNECTION);
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return SUPERCONDUCTOR_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
}
