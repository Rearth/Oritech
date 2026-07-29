package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.interaction.AddonSplicerBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

public class HeartOfTheMachineAddonEntity extends AddonBlockEntity {

    public AddonSplicerBlockEntity.ShrunkAddonData storedData;

    public HeartOfTheMachineAddonEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.HEART_OF_THE_MACHINE_ADDON.get(), pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        if (this.storedData != null) {
            output.store("data", AddonSplicerBlockEntity.ShrunkAddonData.CODEC, this.storedData);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedData = input.read("data", AddonSplicerBlockEntity.ShrunkAddonData.CODEC).orElse(null);

    }

    public MachineAddonController.BaseAddonData getBaseData() {
        if (storedData == null) return MachineAddonController.BaseAddonData.DEFAULT_ADDON_DATA;
        return storedData.data();
    }

    public MachineAddonBlock.AddonSettings getAsSettings() {
        if (storedData == null) return MachineAddonBlock.AddonSettings.getDefaultSettings();

        var base = storedData.data();

        return MachineAddonBlock.AddonSettings.getDefaultSettings()
                .withAddedCapacity(base.energyBonusCapacity())
                .withAddedInsert(base.energyBonusTransfer())
                .withSpeedMultiplier(base.speed())
                .withEfficiencyMultiplier(base.efficiency())
                .withChambers(base.extraChambers())
                .withBurstTicks(base.maxBurstTicks());

    }

    public int getQuarryCount() {
        if (storedData == null) return 0;
        return storedData.quarryCount();
    }

    public int getHunterCount() {
        if (storedData == null) return 0;
        return storedData.hunterCount();
    }

    public int getYieldCount() {
        if (storedData == null) return 0;
        return storedData.yieldCount();
    }

    public boolean hasFluid() {
        if (storedData == null) return false;
        return storedData.fluid();
    }

    public boolean hasCropFilter() {
        if (storedData == null) return false;
        return storedData.cropFilter();
    }

    public boolean hasSilk() {
        if (storedData == null) return false;
        return storedData.silk();
    }
}
