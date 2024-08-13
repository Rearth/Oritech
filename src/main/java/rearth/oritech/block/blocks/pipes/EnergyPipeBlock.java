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
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.pipes.GenericPipeInterfaceEntity;
import rearth.oritech.init.BlockContent;
import team.reborn.energy.api.EnergyStorage;

import java.util.HashMap;
import java.util.List;

public class EnergyPipeBlock extends GenericPipeBlock {
    
    public static HashMap<Identifier, GenericPipeInterfaceEntity.PipeNetworkData> ENERGY_PIPE_DATA = new HashMap<>();
    
    public EnergyPipeBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public BlockApiLookup<?, Direction> getSidesLookup() {
        return EnergyStorage.SIDED;
    }
    
    @Override
    public BlockState getConnectionBlock() {
        return BlockContent.ENERGY_PIPE_CONNECTION.getDefaultState();
    }
    
    @Override
    public BlockState getNormalBlock() {
        return BlockContent.ENERGY_PIPE.getDefaultState();
    }
    
    @Override
    public String getPipeTypeName() {
        return "energy";
    }
    
    @Override
    public boolean connectToBlockType(Block block) {
        return block instanceof EnergyPipeBlock || block instanceof EnergyPipeConnectionBlock;
    }
    
    @Override
    public GenericPipeInterfaceEntity.PipeNetworkData getNetworkData(World world) {
        return ENERGY_PIPE_DATA.computeIfAbsent(world.getRegistryKey().getValue(), data -> new GenericPipeInterfaceEntity.PipeNetworkData());
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        var text = Text.translatable("tooltip.oritech.energy_max_transfer", Oritech.CONFIG.energyPipeTransferRate());
        tooltip.add(text);
        super.appendTooltip(stack, context, tooltip, options);
    }
}
