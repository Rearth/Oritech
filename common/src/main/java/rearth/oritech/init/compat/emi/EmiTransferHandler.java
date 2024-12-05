package rearth.oritech.init.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.client.ui.UpgradableMachineScreenHandler;

import java.util.ArrayList;
import java.util.List;

public class EmiTransferHandler implements StandardRecipeHandler<UpgradableMachineScreenHandler> {
    
    private final Identifier categoryId;
    
    public EmiTransferHandler(Identifier categoryId) {
        this.categoryId = categoryId;
    }
    
    @Override
    public List<Slot> getInputSources(UpgradableMachineScreenHandler handler) {
        return handler.slots;
    }
    
    @Override
    public List<Slot> getCraftingSlots(UpgradableMachineScreenHandler handler) {
        
        if (!(handler.blockEntity instanceof MachineBlockEntity machine)) return List.of();
        
        var res = new ArrayList<Slot>();
        
        for (int i = handler.getMachineInvStartSlot(ItemStack.EMPTY); i < handler.getMachineInvStartSlot(ItemStack.EMPTY) + machine.getSlots().inputCount(); i++) {
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
}
