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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.client.ui.PulverizerScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.EnergyProvider;
import rearth.oritech.util.ImplementedInventory;
import rearth.oritech.util.ScreenProvider;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PulverizerBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, ImplementedInventory, GeoBlockEntity, EnergyProvider, ScreenProvider {

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
    private final SimpleEnergyStorage energyStorage = new SimpleEnergyStorage(5000, 100, 0);
    private final AnimatableInstanceCache animatableInstanceCache = GeckoLibUtil.createInstanceCache(this);

    private int progress;
    private OritechRecipe currentRecipe = OritechRecipe.DUMMY;

    private boolean networkDirty = false;

    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PULVERIZER_ENTITY, pos, state);

        energyStorage.amount = 1000;
    }

    public void tick(World world, BlockPos pos, BlockState state) {

        if (world.isClient) return;

        var recipeCandidate = getRecipe();

        if (recipeCandidate.isPresent() && canOutput(recipeCandidate.get().value())) {
            var activeRecipe = recipeCandidate.get().value();
            currentRecipe = activeRecipe;

            // check energy

            // increase progress
            progress++;

            if (checkCraftingFinished(activeRecipe)) {
                craftItem(activeRecipe, getOutputView(), getInputView());
                resetProgress();
            }

            markNetDirty();

        } else {
            // this happens if either the input slot is empty, or the output slot is blocked
            if (progress < 0) resetProgress();
        }

        if (networkDirty) {
            updateNetwork();
        }
    }

    private void updateNetwork() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.MachineSyncPacket(getPos(), energyStorage.amount, progress, currentRecipe));
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
    }

    // check if output slots are valid, meaning: each slot is either empty, or of the same type and can add the target amount without overfilling
    private boolean canOutput(OritechRecipe recipe) {

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

    protected List<ItemStack> getInputView() {
        var start = 0;
        var count = 2;
        return this.inventory.subList(start, start + count);
    }

    protected List<ItemStack> getOutputView() {
        var start = 2;
        var count = 1;
        return this.inventory.subList(start, start + count);
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableInstanceCache;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(this.getPos());
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
    public List<GuiSlot> getActiveSlots() {
        return List.of(new GuiSlot(0, 80, 11), new GuiSlot(1, 95, 11), new GuiSlot(2, 80, 59));
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
}
