package rearth.oritech.block.blocks.addons;

import net.minecraft.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.SteamBoilerAddonBlockEntity;

public class SteamBoilerAddonBlock extends MachineAddonBlock {
    
    public SteamBoilerAddonBlock(Settings settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return SteamBoilerAddonBlockEntity.class;
    }
    
}
