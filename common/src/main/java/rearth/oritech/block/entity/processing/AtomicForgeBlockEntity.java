package rearth.oritech.block.entity.processing;

import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.block.base.entity.MultiblockMachineEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.InventorySlotAssignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

public class AtomicForgeBlockEntity extends MultiblockMachineEntity {
    
    public AtomicForgeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ATOMIC_FORGE_ENTITY, pos, state, Oritech.CONFIG.processingMachines.atomicForgeData.energyPerTick());
    }
    
    @Override
    protected boolean canProceed(OritechRecipe value) {     // recipe times are 1 tick, but energy storage needs to be full (sized according to recipe)
        return hasEnoughEnergy() && super.canProceed(value);
    }
    
    @Override
    protected boolean hasEnoughEnergy() {
        return energyStorage.getCapacity() > 10 && energyStorage.getAmount() >= energyStorage.getCapacity();
    }
    
    @Override
    protected boolean checkCraftingFinished(OritechRecipe activeRecipe) {
        return progress > 0;
    }
    
    @Override
    protected void useEnergy() {
        energyStorage.amount = 0;
    }
    
    @Override
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        var result = super.getRecipe();
        
        // also adjust energy storage when getting recipe
        if (result.isPresent()) {
            energyStorage.setCapacity((long) Oritech.CONFIG.processingMachines.atomicForgeData.energyPerTick() * result.get().value().getTime());
        } else {
            energyStorage.setCapacity(1);
            energyStorage.setAmount(1);
        }
        
        return result;
        
    }
    
    @Override
    public void updateEnergyContainer() { } // energy storage is updated by this class (based on the recipe amount), not the usual methods
    
    @Override
    public boolean canEnergyStorageChangeWhileGUIOpen() {
        return true;
    }
    
    @Override
    public BarConfiguration getEnergyConfiguration() {
        return new BarConfiguration(7, 7, 18, 71);
    }
    
    @Override
    public float getProgress() {
        return (float) energyStorage.getAmount() / energyStorage.getCapacity();
    }
    
    @Override
    public long getDefaultCapacity() {
        return Oritech.CONFIG.processingMachines.atomicForgeData.energyCapacity();
    }
    
    @Override
    public long getDefaultInsertRate() {
        return Oritech.CONFIG.processingMachines.atomicForgeData.maxEnergyInsertion();
    }
    
    @Override
    protected OritechRecipeType getOwnRecipeType() {
        return RecipeContent.ATOMIC_FORGE;
    }
    
    @Override
    public InventorySlotAssignment getSlotAssignments() {
        return new InventorySlotAssignment(0, 3, 3, 1);
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38),
          new GuiSlot(1, 83, 21),
          new GuiSlot(2, 83, 54),
          new GuiSlot(3, 117, 36, true));
    }
    
    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.ATOMIC_FORGE_SCREEN;
    }
    
    @Override
    public int getInventorySize() {
        return 4;
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction) {
        return null;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(1, 0, 1),
          new Vec3i(1, 0, 0),
          new Vec3i(1, 0, -1),
          new Vec3i(0, 0, 1),
          new Vec3i(0, 0, -1),
          new Vec3i(-1, 0, 1),
          new Vec3i(-1, 0, 0),
          new Vec3i(-1, 0, -1)
        );
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return new ArrayList<>();
    }
    
    @Override
    public void saveExtraData(FriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
    }
}
