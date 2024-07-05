package rearth.oritech.block.blocks.machines.generators;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.entity.machines.generators.SteamEngineEntity;

import java.util.List;
import java.util.Objects;

public class SteamEngineBlock extends MultiblockMachine {
    public SteamEngineBlock(Settings settings) {
        super(settings);
    }
    
    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return Objects.requireNonNull(super.getPlacementState(ctx)).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return SteamEngineEntity.class;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        
        if (Screen.hasControlDown()) {
            tooltip.add(Text.translatable("tooltip.oritech.steam_engine").formatted(Formatting.GRAY));
        }
        
        super.appendTooltip(stack, context, tooltip, options);
    }
}
