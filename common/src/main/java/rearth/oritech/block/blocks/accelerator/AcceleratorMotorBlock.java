package rearth.oritech.block.blocks.accelerator;

import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.entity.accelerator.AcceleratorMotorBlockEntity;
import rearth.oritech.util.TooltipHelper;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AcceleratorMotorBlock extends AcceleratorPassthroughBlock implements EntityBlock {
    
    public AcceleratorMotorBlock(Properties settings) {
        super(settings);
    }
    
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return box(0, 0, 0, 16, 11, 16);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AcceleratorMotorBlockEntity(pos, state);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag options) {
        var showExtra = Screen.hasControlDown();
        if (showExtra) {
            tooltip.add(Component.translatable("tooltip.oritech.accelerator_motor").withStyle(ChatFormatting.GRAY));
        }
        
        TooltipHelper.addMachineTooltip(tooltip, this, this);
    }
}
