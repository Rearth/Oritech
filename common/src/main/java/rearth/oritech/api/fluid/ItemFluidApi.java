package rearth.oritech.api.fluid;

import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.init.ComponentContent;
import rearth.oritech.util.StackContext;

import java.util.function.Supplier;

public interface ItemFluidApi {
    
    void registerForItem(Supplier<Item> itemSupplier);
    
    FluidApi.FluidStorage find(StackContext stack);
    
    default ComponentType<FluidStack> getFluidComponent() {
        return ComponentContent.STORED_FLUID.get();
    }
    
    static boolean tryFluidBlockItemInteraction(ItemStack stack, World world, BlockPos pos, PlayerEntity player, Hand hand) {
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
                stack.decrement(1);
                if (!player.getInventory().insertStack(updated)) {
                    player.dropItem(updated, true);
                }
            } else {
                player.setStackInHand(hand, updated);
            }
        });
        
        var candidate = FluidApi.ITEM.find(stackRef);
        if (candidate == null) {
            return false;
        }
        
        var fluidStorage = tankEntity.getFluidStorage(null);
        
        if (!world.isClient) {
            if (candidate.getContent().getFirst().isEmpty()) { // from tank to item
                var moved = FluidApi.transferFirst(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 8, false);
                if (moved == 0) {   // attempt last if first did not work
                    moved = FluidApi.transferLast(fluidStorage, candidate, FluidStackHooks.bucketAmount() * 8, false);
                }
                Oritech.LOGGER.debug("moved to item {} {}", moved, stackRef.getValue());
            } else {    // from item to tank
                var moved = FluidApi.transferFirst(candidate, fluidStorage, FluidStackHooks.bucketAmount() * 8, false);
                Oritech.LOGGER.debug("moved from item {} {}", moved, stackRef.getValue());
            }
        }
        
        return true;
    }
}
