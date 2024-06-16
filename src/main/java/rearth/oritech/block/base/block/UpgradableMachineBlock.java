package rearth.oritech.block.base.block;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.util.MachineAddonController;

public abstract class UpgradableMachineBlock extends MachineBlock {
    
    public UpgradableMachineBlock(Settings settings) {
        super(settings);
    }
    
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        
        if (!world.isClient) {
            
            var entity = world.getBlockEntity(pos);
            if (!(entity instanceof MachineAddonController machineEntity)) {
                return ActionResult.SUCCESS;
            }
            
            machineEntity.initAddons();
            
        }
        
        return super.onUse(state, world, pos, player, hit);
    }
    
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {

        if (!world.isClient()) {

            var entity = world.getBlockEntity(pos);
            if (entity instanceof MachineAddonController machineEntity) {
                machineEntity.resetAddons();
            }
        }

        return super.onBreak(world, pos, state, player);
    }
}
