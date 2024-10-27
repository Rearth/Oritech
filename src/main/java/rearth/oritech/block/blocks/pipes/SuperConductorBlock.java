package rearth.oritech.block.blocks.pipes;

import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;
import team.reborn.energy.api.EnergyStorage;

import java.util.HashMap;
import java.util.List;

public class SuperConductorBlock extends GenericPipeBlock {
    
    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> SUPERCONDUCTOR_DATA = new HashMap<>();
    
    public SuperConductorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return EnergyStorage.SIDED;
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
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return SUPERCONDUCTOR_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
    
    @Override
    public boolean isCompatibleTarget(Block block) {
        return !block.equals(BlockContent.ENERGY_PIPE_CONNECTION);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        var text = Text.translatable("tooltip.oritech.energy_max_transfer").formatted(Formatting.GRAY)
            .append(Text.translatable("tooltip.oritech.energy_transfer_rate", Oritech.CONFIG.superConductorTransferRate()).formatted(Formatting.GOLD));
        tooltip.add(text);
        tooltip.add(Text.translatable("tooltip.oritech.superconductor").formatted(Formatting.GRAY));
        super.appendTooltip(stack, context, tooltip, options);
    }
}
