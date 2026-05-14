package rearth.oritech.block.base.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.item.InOutInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.block.entity.addons.RedstoneAddonBlockEntity;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.util.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class MachineBlockEntity extends NetworkedBlockEntity
  implements GeoBlockEntity, ScreenProvider, MenuProvider, RedstoneAddonBlockEntity.RedstoneControllable, ColorableMachine, EnergyProvider, ItemProvider {
    
    // animations
    public static final RawAnimation PACKAGED = RawAnimation.begin().thenPlayAndHold("packaged");
    public static final RawAnimation SETUP = RawAnimation.begin().thenPlay("deploy");
    public static final RawAnimation IDLE = RawAnimation.begin().thenPlayAndHold("idle");
    public static final RawAnimation WORKING = RawAnimation.begin().thenPlay("working");
    
    protected final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);
    
    // synced data
    @SyncField({SyncType.GUI_TICK, SyncType.SPARSE_TICK})
    public final ProgressStorage progress = new ProgressStorage();
    @SyncField({SyncType.GUI_TICK})
    protected OritechRecipe currentRecipe = OritechRecipe.EMPTY;
    @SyncField({SyncType.GUI_TICK})
    protected InventoryInputMode inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
    @SyncField({SyncType.GUI_TICK})
    protected boolean disabledViaRedstone = false;
    @SyncField({SyncType.TICK})
    public long lastWorkedAt;   // used for animation sync
    
    @SyncField({SyncType.SPARSE_TICK, SyncType.INITIAL})
    public ColorVariant currentColor = getDefaultColor();
    
    // static data
    protected int energyPerTick;
    
    private long lastChangedAt = 0;  // used to check if anything happened in the last tick, to avoid unneeded calculations and updates
    
    // cache for sided inventory access
    private final Map<Direction, ResourceHandler<ItemResource>> sidedInventories = new HashMap<>(); // only for sided input mode
    
    // own storages
    public final MachineInventoryStorage inventory = new MachineInventoryStorage(getInventorySize(), this::setChanged, getSlotAssignments());
    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(getDefaultCapacity(), getDefaultInsertRate(), getDefaultExtractionRate(), 0, this::setChanged, this.canEnergyStorageChangeWhileGUIOpen());
    
    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state);
        this.energyPerTick = energyPerTick;
        
        if (level != null) {
            lastWorkedAt = level.getGameTime();
            lastChangedAt = level.getGameTime();
        }
    }
    
    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        
        if (!isAssembled(state) || disabledViaRedstone) return;
        
        var lastRecipe = currentRecipe;
        currentRecipe = findActiveRecipe();
        if (currentRecipe.isEmpty()) {
            resetProgress();
            return;
        }
        
        if (lastRecipe != currentRecipe || !canOutputRecipe(currentRecipe)) resetProgress();
        
        workTick();
    }
    
    // main work is done here. At this point, we have a valid recipe (items+fluid verified), and could output the results.
    // we work with one big transaction. Everything is taken directly, and if a part doesnt work we just don't commit the transaction
    protected void workTick() {
        try (var transaction = Transaction.openRoot()) {
            
            var energyNeeded = (int) calculateEnergyUsage();
            var energyTaken = energyStorage.internalExtract(energyNeeded, transaction);
            
            // abort if not enough energy
            if (energyTaken != energyNeeded) return;
            
            progress.increment(transaction);
            
            if (checkCraftingFinished(currentRecipe)) {
                var crafted = finishCrafting(transaction);
                
                if (!crafted) {
                    Oritech.LOGGER.warn("crafting results failed! This should never happen. At: {}", worldPosition.toShortString());
                    return;
                }
                
                progress.reset(transaction);
            }
            
            transaction.commit();
            setChanged();
            
        }
    }
    
    protected void onProgressed() {}
    
    // performance optimized recipe lookup. Verifies that both item and fluid inputs match the recipe.
    protected OritechRecipe findActiveRecipe() {
        
        if (!(level instanceof ServerLevel serverLevel)) return currentRecipe;
        
        var recentlyChanged = (level.getGameTime() - lastChangedAt) <= 1;
        
        // if no active recipe and nothing changed recently, dont do anything
        if (currentRecipe.isEmpty() && !recentlyChanged) return currentRecipe;
        
        // at this point: Either machine inputs were changed, or we have a current recipe
        
        var recipeInput = getRecipeInput();
        if (recipeInput.isEmpty()) return OritechRecipe.EMPTY;
        
        // existing recipe matches (if non-empty)
        if (!currentRecipe.isEmpty() && currentRecipe.matches(recipeInput, level)) return currentRecipe;
        
        // return a potential match, or empty
        var recipeCandidate = serverLevel.recipeAccess().getRecipeFor(getOwnRecipeType(), recipeInput, level);
        return recipeCandidate.map(RecipeHolder::value).orElse(OritechRecipe.EMPTY);
    }
    
    protected OritechRecipeInput getRecipeInput() {
        return new OritechRecipeInput(getInputView(), FluidStack.EMPTY);
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
        lastChangedAt = level.getGameTime();
    }
    
    protected float calculateEnergyUsage() {
        return energyPerTick * getEfficiencyMultiplier() * (1 / getSpeedMultiplier());
    }
    
    public List<ItemStackTemplate> getCraftingResults(OritechRecipe activeRecipe) {
        return activeRecipe.itemResults();
    }
    
    // uses the transaction and tries to do all steps. If input is missing or output doesn't match, return false.
    protected boolean finishCrafting(Transaction transaction) {
        
        var results = getCraftingResults(currentRecipe);
        var recipeIngredients = currentRecipe.itemInputs();
        var outputInv = inventory.getOutputContainer();
        var inputInv = inventory.getInputContainer();
        
        // create outputs
        for (var result : results) {
            var added = outputInv.insert(ItemResource.of(result), result.count(), transaction);
            if (added != result.count()) return false;
        }
        
        // remove inputs. Each input is 1 ingredient.
        var startOffset = 0;    // used so when multiple matching itemStacks are available, they're drained somewhat evenly
        for (var removedIng : recipeIngredients) {
            // try to find current ingredient
            
            var found = false;
            
            for (int i = 0; i < inputInv.size(); i++) {
                var inputResource = inputInv.getResource((i + startOffset) % inputInv.size());
                if (removedIng.test(inputResource.toStack())) {
                    var taken = inputInv.extract(i, inputResource, 1, transaction);
                    if (taken != 1) return false;
                    startOffset++;
                    found = true;
                    break;
                }
            }
            
            if (!found) return false;
        }
        
        return true;
        
    }
    
    protected boolean checkCraftingFinished(OritechRecipe activeRecipe) {
        return progress.get() >= activeRecipe.time() * getSpeedMultiplier();
    }
    
    protected void resetProgress() {
        progress.set(0);
    }
    
    // check if output slots are valid, meaning: each slot is either empty, or of the same type and can add the target amount without overfilling
    public boolean canOutputRecipe(OritechRecipe recipe) {
        
        var outInv = getOutputCopy();
        
        if (outInv.isEmpty()) return true;
        
        var results = recipe.itemResults();
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var outSlot = outInv.getItem(i);
            
            if (outSlot.isEmpty()) continue;
            
            if (!canAddToSlot(result.create(), outSlot)) return false;
        }
        
        return true;
    }
    
    protected boolean canAddToSlot(ItemStack input, ItemStack slot) {
        if (slot.isEmpty()) return true;
        if (!slot.getItem().equals(input.getItem())) return false;  // type mismatch
        return slot.getCount() + input.getCount() <= slot.getMaxStackSize();  // count too high
    }
    
    protected abstract RecipeType<OritechRecipe> getOwnRecipeType();
    
    public abstract ContainerSlotAssignment getSlotAssignments();
    
    protected List<ItemStack> getInputView() {
        var slots = getSlotAssignments();
        return this.inventory.getStacks().subList(slots.inputStart(), slots.inputStart() + slots.inputCount());
    }
    
    protected List<ItemStack> getOutputView() {
        var slots = getSlotAssignments();
        return this.inventory.getStacks().subList(slots.outputStart(), slots.outputStart() + slots.outputCount());
    }
    
    protected Container getOutputCopy() {
        return new SimpleContainer(getOutputView().toArray(ItemStack[]::new));
    }
    
    // new:
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        
        ContainerHelper.saveAllItems(output, inventory.getStacks());
        serializeColor(output);
        
        progress.serialize(output);
        energyStorage.serialize(output);
        output.putShort("input_mode", (short) inventoryInputMode.ordinal());
        output.putBoolean("redstone", disabledViaRedstone);
    }
    
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        
        ContainerHelper.loadAllItems(input, inventory.getStacks());
        deserializeColor(input);
        
        progress.deserialize(input);
        energyStorage.deserialize(input);
        
        inventoryInputMode = InventoryInputMode.values()[input.getShortOr("input_mode", (short) 0)];
        disabledViaRedstone = input.getBooleanOr("redstone", false);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("machine", this::onAnimationUpdate)
                          .triggerableAnim("setup", SETUP)
                          .setSoundKeyframeHandler(new MachineSoundHandler<>(this::getAnimationSpeed)));
    }
    
    public PlayState onAnimationUpdate(AnimationTest<MachineBlockEntity> state) {
        
        state.setControllerSpeed(getAnimationSpeed());
        
        if (state.controller().isPlayingTriggeredAnimation()) return PlayState.CONTINUE;
        
        if (isAssembled(getBlockState())) {
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
    
    public int getRecipeDuration() {
        return getCurrentRecipe().time();
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }
    
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        MenuProvider.super.writeClientSideData(menu, buffer);
        this.sendUpdate(SyncType.GUI_OPEN);
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
        return new OritechScreenHandler(syncId, playerInventory, this);
    }
    
    @Override
    public abstract List<GuiSlot> getGuiSlots();
    
    @Override
    public float getProgress() {
        return (float) progress.get() / (getRecipeDuration() * getSpeedMultiplier());
    }
    
    public DynamicEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public OritechRecipe getCurrentRecipe() {
        return currentRecipe;
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
    
    public boolean isAssembled(BlockState state) {
        return true;
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
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }
    
    @Override
    public int getComparatorEnergyAmount() {
        return (int) ((energyStorage.energy / (float) energyStorage.capacity) * 15);
    }
    
    @Override
    public int getComparatorSlotAmount(int slot) {
        if (inventory.getStacks().size() <= slot) return 0;
        
        var stack = inventory.getStacks().get(slot);
        if (stack.isEmpty()) return 0;
        
        return (int) ((stack.getCount() / (float) stack.getMaxStackSize()) * 15);
    }
    
    @Override
    public int getComparatorProgress() {
        if (currentRecipe.time() <= 0) return 0;
        return (int) ((progress.get() / (float) currentRecipe.time() * getSpeedMultiplier()) * 15);
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
            this.setChanged(false);
            this.sendUpdate(SyncType.SPARSE_TICK);
        }
    }
    
    public static void receiveCycleModePacket(InventoryInputModeSelectorPacket packet, IPayloadContext context) {
        if (context.player().level().getBlockEntity(packet.position()) instanceof MachineBlockEntity machineBlock)
            machineBlock.cycleInputMode();
    }
    
    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }
    
    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        if (inventoryInputMode.equals(InventoryInputMode.SIDED)) {
            return sidedInventories.computeIfAbsent(direction, this::getDirectedStorage);
        }
        return inventory;
    }
    
    // needed for sided inventory mode
    public ResourceHandler<ItemResource> getDirectedStorage(Direction direction) {
        
        var slots = getSlotAssignments();
        if (slots.inputCount() <= 1) return inventory;
        
        if (direction == null) return inventory;
        
        if (direction.equals(Direction.UP)) {
            // input only, disable output
            return new DelegatingResourceHandler<>(inventory) {
                @Override
                public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
                    return 0;
                }
                
                @Override
                public int extract(ItemResource resource, int amount, TransactionContext transaction) {
                    return 0;
                }
            };
            
        } else if (direction.equals(Direction.DOWN)) {
            // output only, disable input
            return new DelegatingResourceHandler<>(inventory) {
                @Override
                public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                    return 0;
                }
                
                @Override
                public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                    return 0;
                }
            };
        } else {
            // north = 0, east = 1, ...
            var horizontalOrdinal = 0;
            if (direction.equals(Direction.EAST)) horizontalOrdinal = 1;
            if (direction.equals(Direction.SOUTH)) horizontalOrdinal = 2;
            if (direction.equals(Direction.WEST)) horizontalOrdinal = 3;
            var inputSlotIndex = slots.inputStart() + horizontalOrdinal % slots.inputCount();
            
            return new DelegatingResourceHandler<>(inventory) {
                @Override
                public int insert(ItemResource resource, int amount, TransactionContext transaction) {
                    return insert(inputSlotIndex, resource, amount, transaction);
                }
                
                @Override
                public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
                    if (index != inputSlotIndex) return 0;
                    return super.insert(index, resource, amount, transaction);
                }
            };
        }
    }
    
    // basic in out storage, but with option for sided / fill even input mode
    public class MachineInventoryStorage extends InOutInventoryStorage {
        
        public MachineInventoryStorage(int size, Runnable onUpdate, ContainerSlotAssignment slotAssignment) {
            super(size, onUpdate, slotAssignment);
        }
        
        // fill evenly tries to insert it in the best matching slots (fills the lowest ones evenly)
        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            
            if (inventoryInputMode.equals(InventoryInputMode.FILL_EVENLY)) {
                var remaining = amount;
                var amountPerSlot = amount / getSlotAssignments().inputCount();
                amountPerSlot = Math.clamp(amountPerSlot, 1, remaining);
                
                // start at slot with fewest items
                var lowestSlot = 0;
                var lowestSlotCount = Integer.MAX_VALUE;
                for (int i = getSlotAssignments().inputStart(); i < getSlotAssignments().inputStart() + getSlotAssignments().inputCount(); i++) {
                    var content = stacks.get(i);
                    if (!content.isEmpty() && !resource.is(content.getItem()))
                        continue;    // skip slots containing other items
                    if (content.getCount() < lowestSlotCount) {
                        lowestSlotCount = content.getCount();
                        lowestSlot = i;
                    }
                }
                
                // actually fill slots, starting with most empty one
                for (var slot = 0; slot < this.size() && remaining > 0; slot++) {
                    remaining -= super.insert((slot + lowestSlot) % this.size(), resource, amountPerSlot, transaction);
                }
                
                return amount - remaining;
            }
            
            return super.insert(resource, amount, transaction);
        }
        
        // specific slot insert in even mode forwards to non-specific slot variant
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            
            if (inventoryInputMode.equals(InventoryInputMode.FILL_EVENLY)) {
                return insert(resource, amount, transaction);
            }
            
            return super.insert(index, resource, amount, transaction);
        }
    }
    
    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) implements CustomPacketPayload {
        
        public static final Type<InventoryInputModeSelectorPacket> PACKET_ID = new Type<>(Oritech.id("input_mode"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
}
