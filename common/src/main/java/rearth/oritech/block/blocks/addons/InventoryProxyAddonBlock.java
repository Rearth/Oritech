package rearth.oritech.block.blocks.addons;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.addons.InventoryProxyAddonBlockEntity;

public class InventoryProxyAddonBlock extends MachineAddonBlock {
    
    public InventoryProxyAddonBlock(Properties settings, AddonSettings addonSettings) {
        super(settings, addonSettings);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return InventoryProxyAddonBlockEntity.class;
    }
    
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        
        if (!world.isClientSide && state.getValue(ADDON_USED)) {
            var handler = (ExtendedMenuProvider) world.getBlockEntity(pos);
                MenuRegistry.openExtendedMenu((ServerPlayer) player, handler);
        }
        
        return InteractionResult.SUCCESS;
    }
    
}
