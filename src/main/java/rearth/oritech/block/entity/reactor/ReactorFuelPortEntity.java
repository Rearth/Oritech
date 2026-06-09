package rearth.oritech.block.entity.reactor;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.item.InOutInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.init.SoundContent;
import rearth.oritech.init.recipes.OritechRecipeInput;
import rearth.oritech.init.recipes.RecipeContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;

import java.util.List;

public class ReactorFuelPortEntity extends BlockEntity implements MenuProvider, ScreenProvider, ItemProvider {

    public final InOutInventoryStorage inventory = new InOutInventoryStorage(1, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 0)) {
        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            var addedStack = resource.toStack();
            if (!(level instanceof ServerLevel serverLevel)) return 0;
            var craftingInv = new OritechRecipeInput(List.of(addedStack), FluidStack.EMPTY);
            var recipeCandidate = serverLevel.recipeAccess().getRecipeFor(RecipeContent.REACTOR.get(), craftingInv, level);
            if (recipeCandidate.isEmpty()) return 0;
            return super.insert(index, resource, amount, transaction);

        }
    };

    @SyncField(SyncType.GUI_TICK)
    public int availableFuel;
    @SyncField(SyncType.GUI_TICK)
    public int currentFuelOriginalCapacity;

    public ReactorFuelPortEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.REACTOR_FUEL_PORT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.putInt("available", availableFuel);
        output.putInt("capacity", currentFuelOriginalCapacity);

        inventory.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        availableFuel = input.getIntOr("available", 0);
        currentFuelOriginalCapacity = input.getIntOr("capacity", 0);

        inventory.deserialize(input);
    }

    // consumes remaining internal fuel when disabled, but will not consume new input items
    public boolean tryConsumeFuel(int amount, boolean disabled) {
        if (availableFuel >= amount) {
            availableFuel -= amount;
            return true;
        }

        if (disabled) return false;

        // try consume input
        var inputStack = inventory.getItem(0);
        if (inputStack.isEmpty()) return false;
        if (!(level instanceof ServerLevel serverLevel)) return false;

        var craftingInv = new OritechRecipeInput(List.of(inputStack), FluidStack.EMPTY);
        var recipeCandidate = serverLevel.recipeAccess().getRecipeFor(RecipeContent.REACTOR.get(), craftingInv, level);

        if (recipeCandidate.isEmpty()) return false;

        var capacity = recipeCandidate.get().value().time();
        currentFuelOriginalCapacity = capacity;
        availableFuel = capacity - amount;
        inputStack.shrink(1);
        playLoadingSound();
        return true;

    }

    private void playLoadingSound() {
        var variation = level.getRandom().nextFloat() * 0.6f - 0.2f;
        level.playSound(null, worldPosition, SoundContent.REACTOR_LOADING.value(), SoundSource.BLOCKS, 0.5f, 0.8f + variation);
    }

    public void updateNetwork() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, SyncType.GUI_TICK, usedBuf, level);
        if (fieldCount == 0) return;
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, ChunkPos.containing(worldPosition), new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), SyncType.GUI_TICK, usedBuf.array()));
    }

    @Override
    public Component getDisplayName() {
        return Component.nullToEmpty("");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OritechScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 55, 35));
    }

    @Override
    public boolean showEnergy() {
        return false;
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 0;
    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public boolean showProgress() {
        return false;
    }

    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }

    @Override
    public InOutInventoryStorage getDisplayedInventory() {
        return inventory;
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.FUEL_PORT_SCREEN.get();
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public boolean showExpansionPanel() {
        return false;
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(Direction direction) {
        return inventory;
    }
}
