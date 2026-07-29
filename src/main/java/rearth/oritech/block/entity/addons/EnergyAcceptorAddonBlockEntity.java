package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.energy.DelegatingEnergyStorage;
import rearth.oritech.api.transfer.energy.EnergyProvider;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class EnergyAcceptorAddonBlockEntity extends AddonBlockEntity implements EnergyProvider {

    private final EnergyHandler delegatedStorage = new DelegatingEnergyStorage(this::getMainStorage, this::isConnected);

    private MachineAddonController cachedController;

    public EnergyAcceptorAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.ENERGY_ACCEPTOR_ADDON.get(), pos, state);
    }

    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }

    private EnergyHandler getMainStorage() {

        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        if (!isUsed) return null;

        var controllerEntity = getCachedController();
        return controllerEntity.getStorageForAddon();
    }

    private MachineAddonController getCachedController() {

        if (cachedController != null)
            return cachedController;

        cachedController = (MachineAddonController) Objects.requireNonNull(level).getBlockEntity(getControllerPos());
        return cachedController;
    }

    @Override
    public EnergyHandler getEnergyLookup(@Nullable Direction direction) {
        return delegatedStorage;
    }
}
