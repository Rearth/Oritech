package rearth.oritech.block.entity.addons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;
import rearth.oritech.block.blocks.addons.MachineAddonBlock;
import rearth.oritech.block.entity.interaction.ShrinkerBlockEntity;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.MachineAddonController;

public class CombiAddonEntity extends AddonBlockEntity {
    
    public ShrinkerBlockEntity.ShrunkAddonData storedData;
    
    public CombiAddonEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.COMBI_ADDON_ENTITY, pos, state);
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.saveAdditional(nbt, registryLookup);
        
        if (this.storedData != null) {
            ShrinkerBlockEntity.ShrunkAddonData.CODEC.encodeStart(registryLookup.createSerializationContext(NbtOps.INSTANCE), this.storedData)
              .resultOrPartial(error -> Oritech.LOGGER.error("Failed to encode stored_data: {}", error))
              .ifPresent(tag -> nbt.put("data", tag));
        }
    }
    
    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        super.loadAdditional(nbt, registryLookup);
        
        if (nbt.contains("data", Tag.TAG_COMPOUND)) {
            var dataTag = nbt.get("data");
            this.storedData = ShrinkerBlockEntity.ShrunkAddonData.CODEC.parse(registryLookup.createSerializationContext(NbtOps.INSTANCE), dataTag)
                                .resultOrPartial(error -> Oritech.LOGGER.error("Failed to decode stored_data: {}", error))
                                .orElse(null);
        }
        
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
