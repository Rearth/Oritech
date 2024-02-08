package rearth.oritech.block.custom;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import rearth.oritech.block.entity.CapacitorAddonBlockEntity;
import rearth.oritech.block.entity.InventoryProxyAddonBlockEntity;

public class InventoryProxyAddonBlock extends MachineAddonBlock {
    
    public InventoryProxyAddonBlock(Settings settings, boolean extender, float speedMultiplier, float efficiencyMultiplier) {
        super(settings, extender, speedMultiplier, efficiencyMultiplier);
    }
    
    @Override
    public @NotNull Class<? extends BlockEntity> getBlockEntityType() {
        return InventoryProxyAddonBlockEntity.class;
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        
        if (!world.isClient && state.get(ADDON_USED)) {
            var handler = (ExtendedScreenHandlerFactory) world.getBlockEntity(pos);
            player.openHandledScreen(handler);
        }
        
        return ActionResult.SUCCESS;
    }
    
}
