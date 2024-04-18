package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.block.base.entity.MultiblockFrameInteractionEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Objects;

public class DestroyerBlockEntity extends MultiblockFrameInteractionEntity {
    
    public boolean hasCropFilterAddon;
    
    public DestroyerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.CROP_FILTER_ADDON)) {
            hasCropFilterAddon = true;
        }
        super.getAdditionalStatFromAddon(addonBlock);
    }
    
    @Override
    public void resetAddons() {
        super.resetAddons();
        hasCropFilterAddon = false;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasCropFilterAddon = nbt.getBoolean("cropAddon");
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var targetPosition = toolPosition.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        // skip not grown crops
        if (hasCropFilterAddon && targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
            return false;
        }
        
        return !targetState.getBlock().equals(Blocks.AIR);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        var targetHardness = targetState.getBlock().getHardness();
        if (targetHardness < 0) return;    // skip undestroyable blocks, such as bedrock
        
        // skip not grown crops
        if (hasCropFilterAddon && targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
            return;
        }
        
        if (!targetState.getBlock().equals(Blocks.AIR)) {
            
            var targetEntity = world.getBlockEntity(targetPosition);
            var dropped = Block.getDroppedStacks(targetState, (ServerWorld) world, targetPosition, targetEntity);
            
            // only proceed if all stacks fit
            for (var stack : dropped) {
                if (!this.inventory.canInsert(stack)) return;
            }
            
            for (var stack : dropped) {
                this.inventory.addStack(stack);
            }
            
            world.addBlockBreakParticles(targetPosition, world.getBlockState(targetPosition));
            world.playSound(null, targetPosition, targetState.getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1f, 1f);
            world.breakBlock(targetPosition, false);
            super.finishBlockWork(processed);
        }
    }
    
    @Override
    protected void doProgress(boolean moving) {
        super.doProgress(moving);
        if (!moving && hasWorkAvailable(getCurrentTarget()))
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 4);
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_DESTROYER_HEAD.getDefaultState();
    }
    
    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 50, 21),
          new GuiSlot(1, 70, 21),
          new GuiSlot(2, 90, 21));
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0,-2),
          new Vec3i(-1, 0,-1),
          new Vec3i(0, 0,2),
          new Vec3i(-1, 0,1)
        );
    }
    
    @Override
    public int getMoveTime() {
        return 20;
    }
    
    @Override
    public int getWorkTime() {
        return 40;
    }
    
    @Override
    public int getMoveEnergyUsage() {
        return 5;
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return 100;
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0,-1),
          new Vec3i(0, 0,1)
        );
    }
    
    @Override
    public void playSetupAnimation() {
    
    }
}
