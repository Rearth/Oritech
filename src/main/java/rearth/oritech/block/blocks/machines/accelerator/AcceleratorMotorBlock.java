package rearth.oritech.block.blocks.machines.accelerator;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.machines.accelerator.AcceleratorMotorBlockEntity;

import java.util.List;

public class AcceleratorMotorBlock extends AcceleratorPassthroughBlock implements BlockEntityProvider {
    
    public AcceleratorMotorBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return Block.createCuboidShape(0, 0, 0, 16, 7, 16);
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorMotorBlockEntity(pos, state);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        var showExtra = Screen.hasControlDown();
        if (!showExtra) {
            tooltip.add(Text.translatable("tooltip.oritech.item_extra_info").formatted(Formatting.GRAY).formatted(Formatting.ITALIC));
        } else {
            tooltip.add(Text.translatable("tooltip.oritech.accelerator_motor").formatted(Formatting.GRAY));
        }
    }
}
