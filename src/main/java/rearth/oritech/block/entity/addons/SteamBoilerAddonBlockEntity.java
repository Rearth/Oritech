package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.api.transfer.fluid.FluidProvider;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

import java.util.Objects;

public class SteamBoilerAddonBlockEntity extends AddonBlockEntity implements FluidProvider {

    private UpgradableGeneratorBlockEntity cachedController;

    public SteamBoilerAddonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.STEAM_BOILER_ADDON.get(), pos, state);
    }

    private boolean isConnected() {
        var isUsed = this.getBlockState().getValue(MachineAddonBlock.ADDON_USED);
        return isUsed && getCachedController() != null;
    }

    private MachineAddonController getCachedController() {

        if (cachedController != null)
            return cachedController;

        var candidate = Objects.requireNonNull(level).getBlockEntity(getControllerPos());
        if (candidate instanceof UpgradableGeneratorBlockEntity generator) {
            cachedController = generator;
        }
        return cachedController;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidLookup(@Nullable Direction direction) {
        if (!isConnected()) return null;
        return cachedController.boilerStorage.getExternalAccess();
    }
}
