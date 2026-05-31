package rearth.oritech.block.base.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.StacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.networking.NetworkedBlockEntity;
import rearth.oritech.api.networking.SyncField;
import rearth.oritech.api.networking.SyncType;
import rearth.oritech.api.transfer.energy.DynamicEnergyStorage;
import rearth.oritech.client.ui.UpgradableOritechScreenHandler;
import rearth.oritech.config.OritechConfig;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.ArrayList;
import java.util.List;

public abstract class UpgradableMachineBlockEntity extends MachineBlockEntity implements MachineAddonController {

    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> connectedAddons = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private final List<BlockPos> openSlots = new ArrayList<>();
    @SyncField(SyncType.GUI_OPEN)
    private BaseAddonData addonData = BaseAddonData.DEFAULT_ADDON_DATA;

    @SyncField({SyncType.GUI_TICK, SyncType.GUI_OPEN})
    public int remainingBurstTicks = 0;

    public UpgradableMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int energyPerTick) {
        super(type, pos, state, energyPerTick);
    }

    @Override
    protected void onProgressed() {
        super.onProgressed();
        consumeBurstTicks();
    }

    public void consumeBurstTicks() {
        // consume burst tick with each tick that we progress (which uses energy once)
        var wasThrottled = isBurstThrottled();
        remainingBurstTicks -= 2;
        remainingBurstTicks = Math.max(remainingBurstTicks, -addonData.maxBurstTicks());
        if (!wasThrottled && isBurstThrottled()) {
            spawnOverheatSmoke();
        }
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        super.serverTick(level, pos, state, blockEntity);
        addBurstTicks();
    }

    public void addBurstTicks() {
        remainingBurstTicks++;
        remainingBurstTicks = Math.min(remainingBurstTicks, addonData.maxBurstTicks());
    }

    @Override
    protected boolean finishCrafting(Transaction transaction) {
        var initialSuccess = super.finishCrafting(transaction);

        if (!initialSuccess) return false;

        var chamberCount = addonData.extraChambers();
        if (chamberCount >= 1) craftChamberResults(chamberCount, transaction);

        // extra chamber crafts are attempted with a sub-transaction for each, but they don't all have to work
        return true;

    }

    protected void craftChamberResults(int chambers, Transaction transaction) {

        for (int i = 0; i < chambers; i++) {
            // try to craft the current recipe N times, with a nested transaction for each
            try (var nested = Transaction.open(transaction)) {

                var success = super.finishCrafting(nested);

                if (!success) break;    // dont commit transaction, dont try again
                nested.commit();    // succeed nested transaction
            }
        }

    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        serializeAddonData(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        deserializeAddonData(input);
        updateEnergyContainer();
    }

    @Override
    public List<BlockPos> getConnectedAddons() {
        return connectedAddons;
    }

    @Override
    public List<BlockPos> getOpenAddonSlots() {
        return openSlots;
    }

    @Override
    public Direction getFacingForAddon() {
        return super.getFacing();
    }

    @Override
    public DynamicEnergyStorage getStorageForAddon() {
        return super.getEnergyStorage();
    }

    @Override
    public BaseAddonData getBaseAddonData() {
        return addonData;
    }


    @Override
    public BlockPos getPosForAddon() {
        return getBlockPos();
    }

    @Override
    public Level getWorldForAddon() {
        return getLevel();
    }

    @Override
    public void setBaseAddonData(BaseAddonData data) {
        this.addonData = data;
        this.setChanged();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new UpgradableOritechScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public StacksResourceHandler<ItemStack, ItemResource> getInventoryForAddon() {
        return inventory;
    }

    @Override
    public ScreenProvider getScreenProvider() {
        return this;
    }

    public boolean isBurstAvailable() {
        return remainingBurstTicks > 0;
    }

    public boolean isBurstThrottled() {
        return remainingBurstTicks < 0;
    }

    private void spawnOverheatSmoke() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        var smokePos = worldPosition.getCenter().add(0, 0.35, 0);
        serverLevel.sendParticles(ParticleTypes.SMOKE, smokePos.x, smokePos.y, smokePos.z, 5, 0.15, 0.1, 0.15, 0.01);
    }

    public float getBurstBonus() {
        if (isBurstAvailable()) {
            return 1 / OritechConfig.addonConfig.burstAddonSpeedMultiplier.get().floatValue();
        } else if (isBurstThrottled()) {
            return OritechConfig.addonConfig.burstAddonThrottleMultiplier.get().floatValue();
        } else {
            return 1f;
        }
    }

    // values smaller than 1 are faster, higher than 1 are slower
    @Override
    public float getSpeedMultiplier() {
        return getBaseAddonData().speed() * getBurstBonus();
    }

    // same here
    @Override
    public float getEfficiencyMultiplier() {
        return getBaseAddonData().efficiency() * getBurstBonus();
    }

    @Override
    public int receivedRedstoneSignal() {
        if (disabledViaRedstone) return 15;
        return 0;
    }

    @Override
    public String currentRedstoneEffect() {
        if (disabledViaRedstone) return "tooltip.oritech.redstone_disabled";
        return "tooltip.oritech.redstone_enabled";
    }
}
