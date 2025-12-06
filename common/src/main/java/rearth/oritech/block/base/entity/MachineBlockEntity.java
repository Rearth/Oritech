package rearth.oritech.block.base.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.energy.containers.DynamicEnergyStorage;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.api.item.containers.DelegatingInventoryStorage;
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
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public abstract class MachineBlockEntity extends NetworkedBlockEntity
  implements ExtendedMenuProvider, GeoBlockEntity, EnergyApi.BlockProvider, ScreenProvider, ItemApi.BlockProvider, RedstoneAddonBlockEntity.RedstoneControllable, ColorableMachine {
    
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
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorableMachine.ColorVariant currentColor = getDefaultColor();
    
    // static data
    protected int energyPerTick;
    
    // own storages
    public final FilteringInventory inventory = new FilteringInventory(getInventorySize(), this::setChanged, getSlotAssignments());
    private final Map<Direction, ItemApi.InventoryStorage> sidedInventories = new HashMap<>(); // only for sided input mode
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate(), this::setChanged, this.canEnergyStorageChangeWhileGUIOpen());
    
    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state);
        this.energyPerTick = energyPerTick;
        
        if (level != null)
            lastWorkedAt = level.getGameTime();
    }
    
    @Override
    public void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
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
                lastWorkedAt = world.getGameTime();
                
                useEnergy();
                
                // increase progress
                progress++;
                
                if (checkCraftingFinished(activeRecipe)) {
                    craftItem(activeRecipe, getOutputView(), getInputView());
                    resetProgress();
                }
                
                setChanged();
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
                    inputStack.shrink(1);
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
            var outSlot = outInv.getItem(i);
            
            if (outSlot.isEmpty()) continue;
            
            if (!canAddToSlot(result, outSlot)) return false;
            
        }
        
        return true;
    }
    
    protected boolean canAddToSlot(ItemStack input, ItemStack slot) {
        if (slot.isEmpty()) return true;
        if (!slot.getItem().equals(input.getItem())) return false;  // type mismatch
        return slot.getCount() + input.getCount() <= slot.getMaxStackSize();  // count too high
    }
    
    protected Optional<RecipeHolder<OritechRecipe>> getRecipe() {
        
        // check if old recipe fits
        if (currentRecipe != null && currentRecipe != OritechRecipe.DUMMY) {
            if (currentRecipe.matches(getInputInventory(), level)) return Optional.of(new RecipeHolder<>(currentRecipe.getOriType().getIdentifier(), currentRecipe));
        }
        
        return level.getRecipeManager().getRecipeFor(getOwnRecipeType(), getInputInventory(), level);
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
    
    protected Container getOutputInventory() {
        return new SimpleContainer(getOutputView().toArray(ItemStack[]::new));
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        
        ContainerHelper.saveAllItems(nbt, inventory.heldStacks, false, registryLookup);
        nbt.putInt("oritech.machine_progress", progress);
        nbt.putLong("oritech.machine_energy", energyStorage.amount);
        nbt.putShort("oritech.machine_input_mode", (short) inventoryInputMode.ordinal());
        nbt.putBoolean("oritech.redstone", disabledViaRedstone);
        
        addColorToNbt(nbt);
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        ContainerHelper.loadAllItems(nbt, inventory.heldStacks, registryLookup);
        progress = nbt.getInt("oritech.machine_progress");
        energyStorage.amount = nbt.getLong("oritech.machine_energy");
        inventoryInputMode = InventoryInputMode.values()[nbt.getShort("oritech.machine_input_mode")];
        disabledViaRedstone = nbt.getBoolean("oritech.redstone");
        
        loadColorFromNbt(nbt);
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
        
        if (isActive(getBlockState())) {
            if (isActivelyWorking()) {
                return state.setAndContinue(WORKING);
            } else {
                return state.setAndContinue(IDLE);
            }
        }
        
        return state.setAndContinue(PACKAGED);
    }
    
    public boolean isActivelyWorking() {
        return level.getGameTime() - lastWorkedAt < 15;
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
    public void saveExtraData(FriendlyByteBuf buf) {
        this.sendUpdate(SyncType.GUI_OPEN);
        buf.writeBlockPos(worldPosition);
        
    }
    
    protected Direction getFacing() {
        return Objects.requireNonNull(level).getBlockState(getBlockPos()).getValue(BlockStateProperties.HORIZONTAL_FACING);
    }
    
    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
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
    
    // lower = better for both (speed and efficiency)
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
                inventoryInputMode = InventoryInputMode.SIDED;
                break;
            case SIDED:
                inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
                break;
        }
        
        setChanged();
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return inventoryInputMode;
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
    public Container getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public ItemApi.InventoryStorage getInventoryStorage(Direction direction) {
        if (inventoryInputMode.equals(InventoryInputMode.SIDED)) {
            return sidedInventories.computeIfAbsent(direction, this::getDirectedStorage);
        }
        return inventory;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.amount / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.heldStacks.size() <= slot) return 0;
        
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxStackSize()) * 15);
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
    
    // whether the energy storage should only send the current amount on network updates, or the full data
    public boolean canEnergyStorageChangeWhileGUIOpen() {
        return false;
    }
    
    @Override
    public ColorVariant getCurrentColor() {
        return currentColor;
    }
    
    @Override
    public void assignColor(ColorVariant color) {
        this.currentColor = color;
        
        if (this.level != null && !this.level.isClientSide()) {
            this.markDirty(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }
    
    public static void receiveCycleModePacket(InventoryInputModeSelectorPacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        if (player.level().getBlockEntity(packet.position()) instanceof MachineBlockEntity machineBlock)
            machineBlock.cycleInputMode();
    }
    
    public ItemApi.InventoryStorage getDirectedStorage(Direction direction) {
        
        var slots = getSlotAssignments();
        if (slots.inputCount() <= 1) return inventory;
        
        if (direction == null) return inventory;
        
        // input only, disable output
        if (direction.equals(Direction.UP)) {
            return new DelegatingInventoryStorage(inventory, () -> true) {
                @Override
                public int extract(ItemStack extracted, boolean simulate) {
                    return 0;
                }
                
                @Override
                public int extractFromSlot(ItemStack extracted, int slot, boolean simulate) {
                    return 0;
                }
                
                @Override
                public boolean supportsExtraction() {
                    return false;
                }
            };
        } else if (direction.equals(Direction.DOWN)) {
            return new DelegatingInventoryStorage(inventory, () -> true) {
                @Override
                public int insert(ItemStack inserted, boolean simulate) {
                    return 0;
                }
                
                @Override
                public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
                    return 0;
                }
                
                @Override
                public boolean supportsInsertion() {
                    return false;
                }
            };
        } else {
            // north = 0, east = 1, ...
            var horizontalOrdinal = 0;
            if (direction.equals(Direction.EAST)) horizontalOrdinal = 1;
            if (direction.equals(Direction.SOUTH)) horizontalOrdinal = 2;
            if (direction.equals(Direction.WEST)) horizontalOrdinal = 3;
            var inputSlotIndex = slots.inputStart() + horizontalOrdinal % slots.inputCount();
            
            return new DelegatingInventoryStorage(inventory, () -> true) {
                @Override
                public int insertToSlot(ItemStack inserted, int slot, boolean simulate) {
                    if (slot != inputSlotIndex) return 0;
                    return super.insertToSlot(inserted, slot, simulate);
                }
                
                @Override
                public int insert(ItemStack inserted, boolean simulate) {
                    return insertToSlot(inserted, inputSlotIndex, simulate);
                }
            };
        }
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
                    var content = this.getItem(i);
                    if (!content.isEmpty() && !content.getItem().equals(toInsert.getItem()))
                        continue;    // skip slots containing other items
                    if (content.getCount() < lowestSlotCount) {
                        lowestSlotCount = content.getCount();
                        lowestSlot = i;
                    }
                }
                
                for (var slot = 0; slot < getContainerSize() && remaining > 0; slot++) {
                    remaining -= customSlotInsert(toInsert.copyWithCount(slotCountTarget), (slot + lowestSlot) % getContainerSize(), simulate);
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
    
    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<InventoryInputModeSelectorPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("input_mode"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
