package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.ItemEnergyFrameInteractionBlockEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;

import java.util.List;
import java.util.Objects;

public class PlacerBlockEntity extends ItemEnergyFrameInteractionBlockEntity {
    
    public PlacerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.PLACER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return false;
        var block = Block.getBlockFromItem(firstBlock.getItem());
        if (block == null) return false;
        
        var targetPosition = toolPosition.down();
        return Objects.requireNonNull(world).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.getDefaultState().canPlaceAt(world, targetPosition);
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var firstBlock = getFirstInInventory();
        if (firstBlock == null) return;
        var block = Block.getBlockFromItem(firstBlock.getItem());
        if (block == null) return;
        
        var targetPosition = processed.down();
        if (Objects.requireNonNull(world).getBlockState(targetPosition).getBlock().equals(Blocks.AIR) && block.getDefaultState().canPlaceAt(world, targetPosition)) {
            world.setBlockState(targetPosition, block.getDefaultState());
            firstBlock.decrement(1);
            world.playSound(null, targetPosition, block.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS, 1f, 1f);
            super.finishBlockWork(processed);
        }
    }
    
    private ItemStack getFirstInInventory() {
        for (var stack : inventory.heldStacks) {
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return stack;
            }
        }
        
        return null;
    }
    
    // send inventory updates to client to correctly render the current item
    @Override
    public void updateNetwork() {
        super.updateNetwork();
        
        if (!isActivelyViewed())
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.InventorySyncPacket(pos, inventory.heldStacks));
    }
    
    @Override
    public float getCoreQuality() {
        return 3f;
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_PLACER_HEAD.getDefaultState();
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, -1, 0)
        );
    }
    
    @Override
    public List<GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 56, 38));
    }
    
    @Override
    public ItemStack getToolheadAdditionalRender() {
        return getFirstInInventory();
    }
    
    @Override
    public int getMoveTime() {
        return Oritech.CONFIG.placerConfig.moveDuration();
    }
    
    @Override
    public int getWorkTime() {
        return Oritech.CONFIG.placerConfig.workDuration();
    }
    
    
    @Override
    public int getMoveEnergyUsage() {
        return Oritech.CONFIG.placerConfig.moveEnergyUsage();
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return Oritech.CONFIG.placerConfig.workEnergyUsage();
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.PLACER_SCREEN;
    }
}
