package rearth.oritech.api.fluid;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import rearth.oritech.Oritech;
import rearth.oritech.api.fluid.FluidApi.FluidStorage;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.StackContext;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface ItemFluidApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    FluidApi.FluidStorage find(StackContext stack);
    
    default DataComponentType<FluidStack> getFluidComponent() {
        return ComponentContent.STORED_FLUID.get();
    }
    
    static boolean tryFluidBlockItemInteraction(ItemStack stack, Level world, BlockPos pos, Player player, InteractionHand hand) {
        var blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof FluidApi.BlockProvider tankEntity)) {
            return false;
        }
        
        var usedStack = stack;
        if (stack.getCount() > 1) {
            usedStack = stack.copyWithCount(1);
        }
        
        var stackRef = new StackContext(usedStack, updated -> {
            if (stack.getCount() > 1) {
                stack.shrink(1);
                if (!player.getInventory().add(updated)) {
                    player.drop(updated, true);
                }
            } else {
                player.setItemInHand(hand, updated);
            }
        });
        
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null) {
            return false;
        }
        
        var fluidStorage = tankEntity.getFluidStorage(null);
        
        if (!world.isClientSide) {
            if (candidate.getContent().getFirst().isEmpty()) { // from tank to item
                var moved = FluidApi.transferLastIncludingInputs(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 8, false);
                Oritech.LOGGER.debug("moved to item {} {}", moved, stackRef.getValue());
            } else {    // from item to tank
                var moved = FluidApi.transferFirst(candidate, fluidStorage, FluidStackHooks.bucketAmount() * 8, false);
                Oritech.LOGGER.debug("moved from item {} {}", moved, stackRef.getValue());
            }
        }
        
        return true;
    }
}
