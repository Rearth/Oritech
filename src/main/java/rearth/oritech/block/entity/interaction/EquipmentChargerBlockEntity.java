package rearth.oritech.block.entity.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.api.transfer.fluid.SimpleFluidStorage;
import rearth.oritech.api.transfer.item.InOutInventoryStorage;
import rearth.oritech.api.transfer.item.ItemProvider;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.ui.OritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ContainerSlotAssignment;
import rearth.oritech.util.InventoryInputMode;
import rearth.oritech.util.ScreenProvider;
import rearth.oritech.client.init.ParticleContent;

import java.util.List;

public class EquipmentChargerBlockEntity extends NetworkedBlockEntity implements FluidProvider, EnergyProvider, ItemProvider,
        ScreenProvider, MenuProvider {

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    protected final DynamicEnergyStorage energyStorage = new DynamicEnergyStorage(OritechConfig.equipmentCharger.energyCapacity.get(), OritechConfig.equipmentCharger.maxEnergyInsertion.get(), OritechConfig.equipmentCharger.maxEnergyExtraction.get(), 0, this::setChanged, false);

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    private final SimpleFluidStorage fluidStorage = new SimpleFluidStorage(16 * 1000, this::setChanged);

    // 0 = bucket/item to be charged/filled, 1 = empty bucket/charged/fill item
    public final InOutInventoryStorage inventory = new InOutInventoryStorage(2, this::setChanged, new ContainerSlotAssignment(0, 1, 1, 1));

    private boolean padOccupied;

    public EquipmentChargerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.EQUIPMENT_CHARGER.get(), pos, state);
    }

    @Override
    public void serverTick(ServerLevel serverLevel, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (serverLevel.isClientSide()) return;

        var startEnergy = energyStorage.energy;
        var startFluid = fluidStorage.getAmount();

        if (!inventory.getItem(0).isEmpty() && inventory.getItem(0).getCount() == 1) {
            var isFull = chargeItems();
            if (!fillItems()) isFull = false;

            // move charged and/or filled item to right
            if (isFull && inventory.getItem(1).isEmpty()) {
                inventory.getStacks().set(1, inventory.getItem(0));
                inventory.getStacks().set(0, ItemStack.EMPTY);
                setChanged();
            }
        }

        if (fluidStorage.getAmount() != startFluid || energyStorage.energy != startEnergy) {
            var c = pos.getCenter().add(0.1, 0.1, 0);
            ParticleContent.sendParticles(serverLevel, ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, 1, 0.6, 0.6, 0.6, 0);
        }

        // The input slot and all players share the charger's per-tick transfer limit.
        chargePlayers(serverLevel, pos, Math.max(0, energyStorage.maxExtract - (startEnergy - energyStorage.energy)));
    }

    private void chargePlayers(ServerLevel level, BlockPos pos, long remainingEnergy) {
        // The charger is half a block tall. Only grounded players with feet on its surface count.
        var surfaceY = pos.getY() + 0.5;
        var players = level.getEntitiesOfClass(Player.class,
                new AABB(pos.getX(), surfaceY, pos.getZ(), pos.getX() + 1, surfaceY + 0.1, pos.getZ() + 1),
                player -> player.isAlive() && !player.isSpectator() && player.onGround()
                        && Math.abs(player.getY() - surfaceY) < 0.05);

        var occupied = !players.isEmpty();
        if (occupied != padOccupied) {
            level.playSound(null, pos, occupied ? SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON : SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
                    SoundSource.BLOCKS, 0.3F, occupied ? 0.8F : 0.7F);
            padOccupied = occupied;
        }

        for (var player : players) {
            var transferred = 0L;
            var playerInventory = PlayerInventoryWrapper.of(player);
            for (var slot = 0; slot < playerInventory.size(); slot++) {
                if (remainingEnergy <= 0 || energyStorage.energy <= 0) break;
                if (player.getInventory().getItem(slot).isEmpty()) continue;

                var itemEnergy = ItemAccess.forPlayerSlot(player, slot).getCapability(Capabilities.Energy.ITEM);
                if (itemEnergy == null) continue;

                var moved = transferEnergy(itemEnergy, remainingEnergy);
                remainingEnergy -= moved;
                transferred += moved;
            }

            if (transferred > 0 && level.getGameTime() % 4 == 0) {
                ParticleContent.sendParticles(level, ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), surfaceY + 0.3, player.getZ(), 4, 0.35, 0.2, 0.35, 0.02);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        fluidStorage.serialize(output);
        inventory.serialize(output);
        output.putLong("energy_stored", energyStorage.energy);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fluidStorage.deserialize(input);
        inventory.deserialize(input);
        energyStorage.energy = input.getLongOr("energy_stored", 0);
    }

    // return true if nothing is left to charge/fill
    private boolean chargeItems() {
        var heldStack = inventory.getStacks().get(0);
        var slotEnergyContainer = heldStack.getCapability(Capabilities.Energy.ITEM, ItemAccess.forHandlerIndexStrict(inventory, 0));
        if (slotEnergyContainer != null) {
            transferEnergy(slotEnergyContainer, energyStorage.maxExtract);
            return slotEnergyContainer.getAmountAsLong() >= slotEnergyContainer.getCapacityAsLong();
        } else {
            return true;
        }
    }

    private int transferEnergy(EnergyHandler target, long limit) {
        var available = (int) Math.min(Integer.MAX_VALUE, Math.min(limit, energyStorage.energy));
        if (available <= 0) return 0;

        try (var transaction = Transaction.openRoot()) {
            var moved = target.insert(available, transaction);
            if (moved > 0) {
                energyStorage.internalExtract(moved, transaction);
                transaction.commit();
            }
            return moved;
        }
    }

    // return true if nothing is left to fill
    private boolean fillItems() {

        var heldStack = inventory.getStacks().get(0);
        var slotFluidContainer = heldStack.getCapability(Capabilities.Fluid.ITEM, ItemAccess.forHandlerIndexStrict(inventory, 0));
        if (slotFluidContainer != null) {
            try (var transaction = Transaction.openRoot()) {    // todo use ResourceHandlerUtil for more things
                var moved = ResourceHandlerUtil.moveFirst(fluidStorage, slotFluidContainer, resource -> true, 100, transaction);
                if (moved != null && !moved.isEmpty()) transaction.commit();
                return fluidStorage.getAmount() > 0 && (moved == null || moved.isEmpty());
            }
        } else {
            return true;
        }

    }

    @Override
    public Component getDisplayName() {
        return Component.literal("");
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        this.sendUpdate(SyncType.GUI_OPEN);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new OritechScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public MenuType<?> getScreenHandlerType() {
        return ModScreens.EQUIPMENT_CHARGER_SCREEN.get();
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return energyStorage;
    }

    @Override
    public ResourceHandler<ItemResource> getItemLookup(@Nullable Direction direction) {
        return inventory.getExternalAccess();
    }

    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(new GuiSlot(0, 56, 38), new GuiSlot(1, 117, 38));
    }

    @Override
    public float getDisplayedEnergyUsage() {
        return 1024;
    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public InventoryInputMode getInventoryInputMode() {
        return InventoryInputMode.FILL_LEFT_TO_RIGHT;
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getDisplayedInventory() {
        return inventory;
    }

    @Override
    public boolean inputOptionsEnabled() {
        return false;
    }

    @Override
    public boolean showArmor() {
        return true;
    }

    @Override
    public boolean showExpansionPanel() {
        return false;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        return fluidStorage;
    }
}
