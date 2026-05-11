package rearth.oritech.client.ui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import rearth.oritech.Oritech;
import rearth.oritech.api.screen.data.DisplayDataSource;
import rearth.oritech.block.base.entity.UpgradableGeneratorBlockEntity;
import rearth.oritech.init.BlockContent;
import rearth.oritech.util.MachineAddonController;
import rearth.oritech.util.ScreenProvider;

import java.util.Objects;

public class UpgradableOritechScreenHandler extends OritechScreenHandler {
    
    public final Level worldAccess;
    public final MachineAddonController addonController;
    
    public UpgradableOritechScreenHandler(int syncId, Inventory inventory, FriendlyByteBuf buf) {
        this(syncId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(buf.readBlockPos())));
    }
    
    public UpgradableOritechScreenHandler(int syncId, Inventory playerInventory, BlockEntity blockEntity) {
        super(syncId, playerInventory, blockEntity);
        
        if (blockEntity instanceof MachineAddonController upgradableEntity) {
            addonController = upgradableEntity;
        } else {
            Oritech.LOGGER.debug("Creating Upgrade screen for non-upgradable block: {}", blockEntity);
            addonController = null;
        }
        
        this.worldAccess = playerInventory.player.level();
    }
    
    @Override
    public void addFluidDisplay() {
        super.addFluidDisplay();
        
        if (blockEntity instanceof UpgradableGeneratorBlockEntity generatorBlock && generatorBlock.isProducingSteam) {
            var in = DisplayDataSource.CreateFluid(generatorBlock.boilerStorage.getInputContainer(), new ScreenProvider.BarConfiguration(8, 24, 16, 54), generatorBlock);
            getDataDisplays().add(in);
            
            var out = DisplayDataSource.CreateFluid(generatorBlock.boilerStorage.getOutputContainer(), new ScreenProvider.BarConfiguration(8 + 19, 24, 16, 54), generatorBlock);
            getDataDisplays().add(out);
        }
        
    }
    
    @Override
    public boolean showRedstoneAddon() {
        return super.showRedstoneAddon() ||
            addonController.getConnectedAddons().stream()
                .anyMatch(addonPos -> this.worldAccess.getBlockState(addonPos).getBlock().equals(BlockContent.MACHINE_REDSTONE_ADDON));
    }
}
