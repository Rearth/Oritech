package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.registry.EmiRecipeFiller;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.ui.BasicMachineScreenHandler;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EmiTransferHandler<S extends BasicMachineScreenHandler> implements StandardRecipeHandler<S> {
    
    private final ResourceLocation categoryId;
    
    public EmiTransferHandler(ResourceLocation categoryId) {
        this.categoryId = categoryId;
    }
    
    @Override
    public List<Slot> getInputSources(S handler) {
        return handler.slots;
    }
    
    @Override
    public List<Slot> getCraftingSlots(S handler) {
        
        if (!(handler.blockEntity instanceof MachineBlockEntity machine)) return List.of();
        
        var res = new ArrayList<Slot>();
        
        for (int i = handler.getMachineInvStartSlot(ItemStack.EMPTY); i < handler.getMachineInvStartSlot(ItemStack.EMPTY) + machine.getSlotAssignments().inputCount(); i++) {
            res.add(handler.slots.get(i));
        }
        
        return res;
        
    }
    
    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        
        if (!(recipe instanceof OritechEMIRecipe oriRecipe)) return false;
        
        var id = oriRecipe.getCategory().getId();
        return id.equals(categoryId);
    }
    
    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<S> context) {
        
        var stacks = EmiRecipeFiller.getStacks(this, recipe, context.getScreen(), context.getAmount());
        if (stacks != null) {
            return EmiRecipeFiller.clientFill(this, recipe, context.getScreen(), stacks, context.getDestination());
        }
        return false;
    }
    
    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<S> context) {
        
        var handler = context.getScreenHandler();
        if (getCraftingSlots(handler).stream().anyMatch(slot -> slot.hasItem() && !slot.getItem().isEmpty())) // check if a non-empty slot is present
            return false;
        
        return StandardRecipeHandler.super.canCraft(recipe, context);
    }
}
