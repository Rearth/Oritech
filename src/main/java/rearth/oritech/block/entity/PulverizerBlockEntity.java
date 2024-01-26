package rearth.oritech.block.entity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.ui.PulverizerScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.*;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PulverizerBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory, GeoBlockEntity, EnergyProvider, ScreenProvider {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(5, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(5000, 100, 0) {
        @Override
        protected void onFinalCommit() {
            super.onFinalCommit();
            markNetDirty();
        }
    };
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private int progress;
    private OritechRecipe currentRecipe = OritechRecipe.DUMMY;
    private InventoryInputMode inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
    private boolean networkDirty = true;

    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY, pos, state);

        energyStorage.amount = 1000;
    }

    public void tick(World world, BlockPos pos, BlockState state) {

        if (world.isClient) return;

        var recipeCandidate = getRecipe();
        if (recipeCandidate.isEmpty()) currentRecipe = OritechRecipe.DUMMY;     // reset recipe when invalid or no input is given

        if (recipeCandidate.isPresent() && canOutputRecipe(recipeCandidate.get().value())) {
            // this is separate so that progress is not reset when out of energy
            if (hasEnoughEnergy(recipeCandidate.get().value())) {
                var activeRecipe = recipeCandidate.get().value();
                currentRecipe = activeRecipe;
                
                // check energy
                useEnergy(activeRecipe);
                
                // increase progress
                progress++;
                
                if (checkCraftingFinished(activeRecipe)) {
                    craftItem(activeRecipe, getOutputView(), getInputView());
                    resetProgress();
                }
                
                markNetDirty();
            }

        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress > 0) resetProgress();
        }

        if (networkDirty) {
            updateNetwork();
        }
    }

    private boolean hasEnoughEnergy(OritechRecipe recipe) {
        return energyStorage.amount > recipe.getEnergyPerTick();
    }

    private void useEnergy(OritechRecipe recipe) {
        var used = recipe.getEnergyPerTick();
        energyStorage.amount -= used;
    }

    private void updateNetwork() {

        if (!networkDirty) return;

        var updateFrequency = 5;

        // checks if a player has the inventory opened. In this case, update net every tick. In the screen we want to data to always be live, while otherwise it can be
        // a few ticks old (e.g. for rendering), as this does not matter as much.
        // Currently not perfect for multiplayer, as it doesn't track individual players. So all players that match the entity handle will receive the packets while
        // the screen is open
        if (isActivelyViewed()) updateFrequency = 1;

        if (Objects.requireNonNull(this.world).getTime() % updateFrequency != 0) return;

        sendNetworkEntry();
    }

    private boolean isActivelyViewed() {
        var closestPlayer = Objects.requireNonNull(world).getClosestPlayer(pos.getX(), pos.getY(), pos.getZ(), 5, false);
        return closestPlayer != null && closestPlayer.currentScreenHandler instanceof PulverizerScreenHandler handler && getPos().equals(handler.getBlockPos());
    }

    private void sendNetworkEntry() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.MachineSyncPacket(getPos(), energyStorage.amount, progress, currentRecipe, inventoryInputMode));
        networkDirty = false;
    }

    private void craftItem(OritechRecipe activeRecipe, List<ItemStack> outputInventory, List<ItemStack> inputInventory) {

        var results = activeRecipe.getResults();
        var inputs = activeRecipe.getInputs();
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

        for (int i = 0; i < inputs.size(); i++) {
            var taken = Inventories.splitStack(inputInventory, i, 1);  // amount is not configurable, because ingredient doesn't parse amount in recipe
        }

    }

    private boolean checkCraftingFinished(OritechRecipe activeRecipe) {
        return progress >= activeRecipe.getTime();
    }

    private void resetProgress() {
        progress = 0;
        markNetDirty();
    }

    private void markNetDirty() {
        networkDirty = true;
        markDirty();
    }

    // check if output slots are valid, meaning: each slot is either empty, or of the same type and can add the target amount without overfilling
    private boolean canOutputRecipe(OritechRecipe recipe) {

        var outInv = getOutputInventory();

        if (outInv.isEmpty()) return true;

        List<ItemStack> results = recipe.getResults();
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var outSlot = outInv.getStack(i);

            if (outSlot.isEmpty()) continue;

            if (!result.getItem().equals(outSlot.getItem())) return false;                      // type mismatches
            if (outSlot.getCount() + result.getCount() > outSlot.getMaxCount()) return false;   // count too high

        }

        return true;
    }

    private Optional<RecipeEntry<OritechRecipe>> getRecipe() {
        return Objects.requireNonNull(world).getRecipeManager().getFirstMatch(getOwnRecipeType(), getInputInventory(), world);
    }

    protected OritechRecipe.OritechRecipeType getOwnRecipeType() {
        return RecipeContent.PULVERIZER;
    }

    public InventorySlotAssignment getSlots() {
        return new InventorySlotAssignment(0, 4, 4, 1);
    }

    protected List<ItemStack> getInputView() {
        var slots = getSlots();
        return this.inventory.subList(slots.inputStart(), slots.inputStart() + slots.inputCount());
    }

    protected List<ItemStack> getOutputView() {
        var slots = getSlots();
        return this.inventory.subList(slots.outputStart(), slots.outputStart() + slots.outputCount());
    }

    protected Inventory getInputInventory() {
        return new SimpleInventory(getInputView().toArray(ItemStack[]::new));
    }

    protected Inventory getOutputInventory() {
        return new SimpleInventory(getOutputView().toArray(ItemStack[]::new));
    }

    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("oritech.machine_progress", progress);
        nbt.putLong("oritech.machine_energy", energyStorage.amount);
        nbt.putShort("oritech.machine_input_mode", (short) inventoryInputMode.ordinal());
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        progress = nbt.getInt("oritech.machine_progress");
        energyStorage.amount = nbt.getLong("oritech.machine_energy");
        inventoryInputMode = InventoryInputMode.values()[nbt.getShort("oritech.machine_input_mode")];
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction side) {

        // todo allow output to right side, disable input on right side
        if (side != Direction.DOWN) return false;

        var config = getSlots();
        return slot >= config.outputStart() && slot < config.outputStart() + config.outputCount();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction side) {
        var mode = inventoryInputMode;
        var config = getSlots();

        // insert from any side besides bottom
        if (side == Direction.DOWN) return false;

        var inv = this.getInputView();

        // fill equally
        // check all slots, find the one with the lowest (or empty) type
        return switch (mode) {
            case FILL_EVENLY -> {
                var target = findLowestMatchingSlot(stack, inv, true);
                yield target >= 0 && config.inputToRealSlot(target) == slot;
            }
            case FILL_LEFT_TO_RIGHT -> // fill left to right
                    true;
            case FILL_MATCHING_RECIPE -> {
                var recipeTargetSlot = slotRecipeSearch(stack, inv);
                yield recipeTargetSlot >= 0 && config.inputToRealSlot(recipeTargetSlot) == slot;
            }
        };

    }

    private int slotRecipeSearch(ItemStack stack, List<ItemStack> inv) {

        // find matching recipe
        // check if currently already using a recipe, if so use this one. This means that all slots are used, and we can just top the slots up
        if (currentRecipe.getEnergyPerTick() != -1) {
            return findLowestMatchingSlot(stack, inv, false);
        }

        // get all recipe types
        // filter which ones are available based on stack
        // filter remaining ones based on inventory
        // select first (if any) remaining
        // select lowest filled slot matching positions in recipe

        var availableRecipes = Objects.requireNonNull(world).getRecipeManager().listAllOfType(getOwnRecipeType());
        var matchingStackRecipes = new HashSet<OritechRecipe>(availableRecipes.size() / 2);

        for (var recipe : availableRecipes) {
            if (recipeInputsStack(recipe.value().getInputs(), stack))
                matchingStackRecipes.add(recipe.value());
        }

        OritechRecipe result = null;
        for (var recipe : matchingStackRecipes) {
            if (invCouldAllowRecipe(recipe, inv)) {
                // found valid recipe, use this one
                result = recipe;
                break;
            }
        }

        if (result == null) return -1;

        // find indices of slots matching stack in recipe
        // find lowest / first empty slot in those indices
        var searchTargets = new HashSet<Integer>();
        var inputs = result.getInputs();

        for (int i = 0; i < inputs.size(); i++) {
            var ingredient = inputs.get(i);
            if (ingredient.test(stack)) searchTargets.add(i);
        }

        var lowestCount = 64;
        var lowestIndex = -1;
        for (var slot : searchTargets) {
            var slotContent = inv.get(slot);
            System.out.println(slot);
            if (slotContent.isEmpty()) return slot;

            if (slotContent.getCount() < lowestCount) {
                lowestIndex = slot;
                lowestCount = slotContent.getCount();
            }
        }

        return lowestIndex;
    }

    private boolean invCouldAllowRecipe(OritechRecipe recipe, List<ItemStack> inv) {

        List<Ingredient> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            var ingredient = inputs.get(i);
            var slot = inv.get(i);
            if (!slot.isEmpty() && !ingredient.test(slot)) return false;
        }

        return true;
    }

    private static boolean recipeInputsStack(List<Ingredient> inputs, ItemStack stack) {
        for (var ingredient : inputs) {
            if (ingredient.test(stack)) {
                // found recipe containing item
                return true;
            }
        }

        return false;
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

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
        sendNetworkEntry();
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Pulverizer");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PulverizerScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public EnergyStorage getStorage() {
        return energyStorage;
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
                new GuiSlot(0, 80, 11),
                new GuiSlot(1, 100, 11),
                new GuiSlot(2, 120, 11),
                new GuiSlot(3, 140, 11),
                new GuiSlot(4, 80, 59));
    }

    @Override
    public float getProgress() {
        return (float) progress / currentRecipe.getTime();
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }

    public SimpleEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public OritechRecipe getCurrentRecipe() {
        return currentRecipe;
    }

    public void setCurrentRecipe(OritechRecipe currentRecipe) {
        this.currentRecipe = currentRecipe;
    }
    
    public void cycleInputMode() {
        switch (inventoryInputMode) {
            case FILL_LEFT_TO_RIGHT:
                inventoryInputMode = InventoryInputMode.FILL_EVENLY;
                break;
            case FILL_EVENLY:
                inventoryInputMode = InventoryInputMode.FILL_MATCHING_RECIPE;
                break;
            case FILL_MATCHING_RECIPE:
                inventoryInputMode = InventoryInputMode.FILL_LEFT_TO_RIGHT;
                break;
        }
        
        markNetDirty();
    }

    public void setInventoryInputMode(InventoryInputMode inventoryInputMode) {
        this.inventoryInputMode = inventoryInputMode;
    }
    
    @Override
    public InventoryInputMode getInventoryInputMode() {
        return inventoryInputMode;
    }
}
