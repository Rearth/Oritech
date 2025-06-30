package rearth.oritech.block.base.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.InOutInventoryStorage;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.ui.BasicMachineScreenHandler;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// next todo: update addon UI data handling, test ALL the things
// todo switch input mode button
// todo multiblock animation controller event sending
// todo geckolib part to handled animations?
public abstract class MachineBlockEntity extends NetworkedBlockEntity
  implements ExtendedMenuProvider, GeoBlockEntity, EnergyApi.BlockProvider, ScreenProvider, ItemApi.BlockProvider, RedstoneAddonBlockEntity.RedstoneControllable {
    
    // animations
    public static final RawAnimation PACKAGED = RawAnimation.begin().thenPlayAndHold("packaged");
    public static final RawAnimation SETUP = RawAnimation.begin().thenPlay("deploy");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation WORKING = RawAnimation.begin().thenPlay("working");
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // synced data
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK})
    public int progress;
    @SyncField({SyncType.GUI_TICK})
    protected OritechRecipe currentRecipe = OritechRecipe.DUMMY;
    @SyncField({SyncType.GUI_TICK})
    protected InventoryInputMode inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
    @SyncField({SyncType.GUI_TICK})
    protected boolean disabledViaRedstone = false;
    @SyncField({SyncType.TICK})
    public long lastWorkedAt;
    
    // static data
    protected int energyPerTick;
    
    // own storages
    public final FilteringInventory inventory = new FilteringInventory(getInventorySize(), this::markDirty, getSlotAssignments());
    @SyncField({SyncType.GUI_TICK})
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate(), this::markDirty);
    
    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state);
        this.energyPerTick = energyPerTick;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
        
        if (world != null)
            lastWorkedAt = world.getTime();
    }
    
    @Override
    public void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (!isActive(state) || disabledViaRedstone) return;
        
        // if a recipe is found, this means the input items are all available
        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty())
            currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given
        
        if (recipeCandidate.isPresent() && canOutputRecipe(recipeCandidate.get().value()) && canProceed(recipeCandidate.get().value())) {
            
            // reset when recipe was switched while running
            if (currentRecipe != recipeCandidate.get().value()) resetProgress();
            
            // this is separate so that progress is not reset when out of energy
            if (hasEnoughEnergy()) {
                var activeRecipe = recipeCandidate.get().value();
                currentRecipe = activeRecipe;
                lastWorkedAt = world.getTime();
                
                useEnergy();
                
                // increase progress
                progress++;
                
                if (checkCraftingFinished(activeRecipe)) {
                    craftItem(activeRecipe, getOutputView(), getInputView());
                    resetProgress();
                }
                
                markDirty();
            }
            
        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress > 0) resetProgress();
        }
    }
    
    // used to do additional checks, if the recipe match is not enough
    protected boolean canProceed(OritechRecipe value) {
        return true;
    }
    
    protected boolean hasEnoughEnergy() {
        return energyStorage.amount >= calculateEnergyUsage();
    }
    
    @SuppressWarnings("lossy-conversions")
    protected void useEnergy() {
        energyStorage.amount -= calculateEnergyUsage();
    }
    
    protected float calculateEnergyUsage() {
        return energyPerTick * getEfficiencyMultiplier() * (1 / getSpeedMultiplier());
    }
    
    public List<ItemStack> getCraftingResults(OritechRecipe activeRecipe) {
        return activeRecipe.getResults();
    }
    
    protected void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {
        
        var results = getCraftingResults(activeRecipe);
        var inputs = activeRecipe.getInputs();
        
        // create outputs
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var slot = outputInventory.get(i);
            
            var newCount = slot.getCount() + result.getCount();
            if (slot.isEmpty()) {
                outputInventory.set(i, result.copy());
            } else {
                slot.setCount(newCount);
            }
        }
        
        // remove inputs. Each input is 1 ingredient.
        var startOffset = 0;    // used so when multiple matching stacks are available, they're drained somewhat evenly
        for (var removedIng : inputs) {
            // try to find current ingredient
            for (int i = 0; i < inputInventory.size(); i++) {
                var inputStack = inputInventory.get((i + startOffset) % inputInventory.size());
                if (removedIng.test(inputStack)) {
                    inputStack.decrement(1);
                    startOffset++;
                    break;
                }
            }
            
            
        }
        
    }
    
    protected boolean checkCraftingFinished(OritechRecipe activeRecipe) {
        return progress >= activeRecipe.getTime() * getSpeedMultiplier();
    }
    
    protected void resetProgress() {
        progress = 0;
    }
    
    // check if output slots are valid, meaning: each slot is either empty, or of the same type and can add the target amount without overfilling
    public boolean canOutputRecipe(OritechRecipe recipe) {
        
        var outInv = getOutputInventory();
        
        if (outInv.isEmpty()) return true;
        
        List<ItemStack> results = recipe.getResults();
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var outSlot = outInv.getStack(i);
            
            if (outSlot.isEmpty()) continue;
            
            if (!canAddToSlot(result, outSlot)) return false;
            
        }
        
        return true;
    }
    
    protected boolean canAddToSlot(ItemStack input, ItemStack slot) {
        if (slot.isEmpty()) return true;
        if (!slot.getItem().equals(input.getItem())) return false;  // type mismatch
        return slot.getCount() + input.getCount() <= slot.getMaxCount();  // count too high
    }
    
    protected Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        return world.getRecipeManager().getFirstMatch(getOwnRecipeType(), getInputInventory(), world);
    }
    
    protected abstract OritechRecipeType getOwnRecipeType();
    
    public abstract InventorySlotAssignment getSlotAssignments();
    
    protected List<ItemStack> getInputView() {
        var slots = getSlotAssignments();
        return this.inventory.heldStacks.subList(slots.inputStart(), slots.inputStart() + slots.inputCount());
    }
    
    protected List<ItemStack> getOutputView() {
        var slots = getSlotAssignments();
        return this.inventory.heldStacks.subList(slots.outputStart(), slots.outputStart() + slots.outputCount());
    }
    
    protected RecipeInput getInputInventory() {
        return new SimpleCraftingInventory(getInputView().toArray(ItemStack[]::new));
    }
    
    protected Inventory getOutputInventory() {
        return new SimpleInventory(getOutputView().toArray(ItemStack[]::new));
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        
        Inventories.writeNbt(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putInt("oritech.machine_progress", progress);
        nbt.putLong("oritech.machine_energy", energyStorage.amount);
        nbt.putShort("oritech.machine_input_mode", (short) inventoryInputMode.ordinal());
        nbt.putBoolean("oritech.redstone", disabledViaRedstone);
    }
    
    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Inventories.readNbt(nbt, inventory.heldStacks, registryLookup);
        progress = nbt.getInt("oritech.machine_progress");
        energyStorage.amount = nbt.getLong("oritech.machine_energy");
        inventoryInputMode = InventoryInputMode.values()[nbt.getShort("oritech.machine_input_mode")];
        disabledViaRedstone = nbt.getBoolean("oritech.redstone");
    }
    
    private int findLowestMatchingSlot(ItemStack stack, List<ItemStack> inv, boolean allowEmpty) {
        
        var lowestMatchingIndex = -1;
        var lowestMatchingCount = 64;
        
        for (int i = 0; i < inv.size(); i++) {
            var invSlot = inv.get(i);
            
            // if a slot is empty, is it automatically the lowest
            if (invSlot.isEmpty() && allowEmpty) return i;
            
            if (invSlot.getItem().equals(stack.getItem()) && invSlot.getCount() < lowestMatchingCount) {
                lowestMatchingIndex = i;
                lowestMatchingCount = invSlot.getCount();
            }
        }
        
        return lowestMatchingIndex;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, this::onAnimationUpdate)
                          .triggerableAnim("setup", SETUP)
                          .setAnimationSpeedHandler(animatable -> (double) getAnimationSpeed())
                          .setSoundKeyframeHandler(new AutoPlayingSoundKeyframeHandler<>(this::getAnimationSpeed)));
    }
    
    public PlayState onAnimationUpdate(final AnimationState<MachineBlockEntity> state) {
        
        if (state.getController().isPlayingTriggeredAnimation()) return PlayState.CONTINUE;
        
        if (isActive(getCachedState())) {
            if (isActivelyWorking()) {
                return state.setAndContinue(WORKING);
            } else {
                return state.setAndContinue(IDLE);
            }
        }
        
        return state.setAndContinue(PACKAGED);
    }
    
    public boolean isActivelyWorking() {
        return world.getTime() - lastWorkedAt < 15;
    }
    
    public void playSetupAnimation() {
        triggerAnim("base_controller", "setup");
    }
    
    protected float getAnimationSpeed() {
        if (getRecipeDuration() < 0) return 1;
        var recipeTicks = getRecipeDuration() * getSpeedMultiplier();
        return (getAnimationDuration() / recipeTicks) * 0.99f;
    }
    
    public int getAnimationDuration() {
        return 60;  // 3s
    }
    
    protected int getRecipeDuration() {
        return getCurrentRecipe().getTime();
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public void saveExtraData(PacketByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(pos);
        
    }
    
    protected Direction getFacing() {
        return Objects.requireNonNull(world).getBlockState(getPos()).get(Properties.HORIZONTAL_FACING);
    }
    
    @Override
    public Text getDisplayName() {
        return Text.literal("");
    }
    
    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new BasicMachineScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public EnergyApi.EnergyStorage getEnergyStorage(Direction direction) {
        return energyStorage;
    }
    
    @Override
    public abstract List<GuiSlot> getGuiSlots();
    
    @Override
    public float getProgress() {
        return (float) progress / (currentRecipe.getTime() * getSpeedMultiplier());
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public DynamicEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public OritechRecipe getCurrentRecipe() {
        return currentRecipe;
    }
    
    public void setCurrentRecipe(OritechRecipe currentRecipe) {
        this.currentRecipe = currentRecipe;
    }
    
    // lower = better for both
    public float getSpeedMultiplier() {
        return 1;
    }
    
    public float getEfficiencyMultiplier() {
        return 1;
    }
    
    public void cycleInputMode() {
        switch (inventoryInputMode) {
            case FILL_LEFT_TO_RIGHT:
                inventoryInputMode = InventoryInputMode.FILL_EVENLY;
                break;
            case FILL_EVENLY:
                inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
                break;
        }
        
        markDirty();
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return inventoryInputMode;
    }
    
    public void setInventoryInputMode(InventoryInputMode inventoryInputMode) {
        this.inventoryInputMode = inventoryInputMode;
    }
    
    public abstract int getInventorySize();
    
    public boolean isActive(BlockState state) {
        return true;
    }
    
    public void setEnergyStored(long amount) {
        energyStorage.amount = amount;
    }
    
    @Override
    public float getDisplayedEnergyUsage() {
        return calculateEnergyUsage();
    }
    
    public long getDefaultCapacity() {
        return 5000;
    }
    
    public long getDefaultInsertRate() {
        return 1024;
    }
    
    @Override
    public float getDisplayedEnergyTransfer() {
        return energyStorage.maxInsert;
    }
    
    public long getDefaultExtractionRate() {
        return 0;
    }
    
    public int getEnergyPerTick() {
        return energyPerTick;
    }
    
    @Override
    public Inventory getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        return inventory;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot) return 0;
        
        var stack = inventory.getStack(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxCount()) * 15);
    }
    
    @Override
    public int getComparatorProgress() {
        if (currentRecipe.getTime() <= 0) return 0;
        return (int) ((progress / (float) currentRecipe.getTime() * getSpeedMultiplier()) * 15);
    }
    
    @Override
    public int getComparatorActiveState() {
        return isActivelyWorking() ? 15 : 0;
    }
    
    @Override
    public void onRedstoneEvent(boolean isPowered) {
        this.disabledViaRedstone = isPowered;
    }
    
    public class FilteringInventory extends InOutInventoryStorage {
        
        public FilteringInventory(int size, Runnable onUpdate, InventorySlotAssignment slotAssignment) {
            super(size, onUpdate, slotAssignment);
        }
        
        @Override
        public int insert(ItemStack toInsert, boolean simulate) {
            
            if (inventoryInputMode.equals(InventoryInputMode.FILL_EVENLY)) {
                var remaining = toInsert.getCount();
                var slotCountTarget = toInsert.getCount() / getSlotAssignments().inputCount();
                slotCountTarget = Math.clamp(slotCountTarget, 1, remaining);
                
                // start at slot with fewest items
                var lowestSlot = 0;
                var lowestSlotCount = Integer.MAX_VALUE;
                for (int i = getSlotAssignments().inputStart(); i < getSlotAssignments().inputStart() + getSlotAssignments().inputCount(); i++) {
                    var content = this.getStack(i);
                    if (!content.isEmpty() && !content.getItem().equals(toInsert.getItem())) continue;    // skip slots containing other items
                    if (content.getCount() < lowestSlotCount) {
                        lowestSlotCount = content.getCount();
                        lowestSlot = i;
                    }
                }
                
                for (var slot = 0; slot < size() && remaining > 0; slot++) {
                    remaining -= customSlotInsert(toInsert.copyWithCount(slotCountTarget), (slot + lowestSlot) % size(), simulate);
                }
                
                return toInsert.getCount() - remaining;
            }
            
            
            return super.insert(toInsert, simulate);
        }
        
        @Override
        public int insertToSlot(ItemStack addedStack, int slot, boolean simulate) {
            
            if (inventoryInputMode.equals(InventoryInputMode.FILL_EVENLY)) {
                return insert(addedStack, simulate);
            }
            
            return customSlotInsert(addedStack, slot, simulate);
        }
        
        private int customSlotInsert(ItemStack toInsert, int slot, boolean simulate) {
            return super.insertToSlot(toInsert, slot, simulate);
        }
    }
    
}
