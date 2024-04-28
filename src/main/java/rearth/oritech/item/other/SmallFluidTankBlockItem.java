package rearth.oritech.item.other;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SmallFluidTankBlockItem extends BlockItem {
    public SmallFluidTankBlockItem(Block block, Settings settings) {
        super(block, settings);
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        if (!stack.hasNbt()) return;
        var nbt = stack.getNbt();
        
        var amount = nbt.getLong("amount");
        var amountTip = amount / FluidConstants.BUCKET + " B";
        tooltip.add(Text.literal("Content: " + amountTip).formatted(Formatting.DARK_AQUA));
    }
    
    @Override
    public Text getName(ItemStack stack) {
        
        if (!stack.hasNbt()) return super.getName(stack);
        
        var nbt = stack.getNbt();
        
        var variant = FluidVariant.fromNbt(nbt.getCompound("fluidVariant"));
        var titleFluid = FluidVariantAttributes.getName(variant).getString();
        
        return Text.of("Small " + titleFluid + " Tank");
    }
}
