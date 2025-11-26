package rearth.oritech.util;

import rearth.oritech.api.energy.EnergyApi;
import rearth.oritech.api.item.ItemApi;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.processing.MachineCoreBlock;
import rearth.oritech.block.entity.MachineCoreEntity;
import rearth.oritech.client.init.ParticleContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface MultiblockMachineController {
    
    List<Vec3i> getCorePositions();
    
    Direction getFacingForMultiblock();
    
    BlockPos getPosForMultiblock();
    
    Level getWorldForMultiblock();
    
    ArrayList<BlockPos> getConnectedCores();
    
    void setCoreQuality(float quality);
    
    float getCoreQuality();
    
    ItemApi.InventoryStorage getInventoryForMultiblock();
    
    EnergyApi.EnergyStorage getEnergyStorageForMultiblock(Direction direction);
    
    default void addMultiblockToNbt(CompoundTag nbt) {
        
        var posList = new ListTag();
        for (var pos : getConnectedCores()) {
            var posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedCores", posList);
        nbt.putFloat("coreQuality", getCoreQuality());
    }
    
    default void loadMultiblockNbtData(CompoundTag nbt) {
        
        var posList = nbt.getList("connectedCores", Tag.TAG_COMPOUND);
        var coreBlocksConnected = getConnectedCores();
        
        for (var posTag : posList) {
            var posCompound = (CompoundTag) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            coreBlocksConnected.add(pos);
        }
        
        setCoreQuality(nbt.getFloat("coreQuality"));
    }
    
    default Boolean tryPlaceNextCore(Player player) {
        
        var heldStack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        var heldItem = heldStack.getItem();
        
        if (!(heldItem instanceof BlockItem blockItem)) return false;
        
        if (blockItem.getBlock() instanceof MachineCoreBlock) {
            var nextPosition = this.getNextMissingCore();
            if (nextPosition != null) {
                this.getWorldForMultiblock().setBlockAndUpdate(nextPosition, blockItem.getBlock().defaultBlockState());
                if (!player.isCreative()) {
                    heldStack.shrink(1);
                    if (heldStack.getCount() == 0)
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }
    
    default BlockPos getNextMissingCore() {
        
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        
        var ownFacing = getFacingForMultiblock();
        var targetMachinePositions = getCorePositions();
        
        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.offset(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            if (checkState.is(Blocks.AIR) || checkState.is(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("minecraft", "replaceable")))) {
                return checkPos;
            }
        }
        
        return null;
    }
    
    default boolean initMultiblock(BlockState state) {
        
        // check if multiblock is already created, if so cancel
        // call method the get a list of relative positions
        // check all positions if the blocks there extend MachineCoreBlock
        // if so, add them to list of used blocks
        // if not (e.g. block wrong type or air), draw a small particle to indicate the missing position
        // when all blocks are valid, multiblock is active
        // update all multiblocks state to USED=true, write controller position to block state
        
        if (state.getValue(MultiblockMachine.ASSEMBLED)) return true;
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        var coreBlocksConnected = getConnectedCores();
        
        var ownFacing = getFacingForMultiblock();
        
        var targetMachinePositions = getCorePositions();
        var coreBlocks = new ArrayList<MultiBlockElement>(targetMachinePositions.size());
        
        var sumCoreQuality = 0f;
        
        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.offset(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            var blockType = checkState.getBlock();
            if (blockType instanceof MachineCoreBlock coreBlock && !checkState.getValue(MachineCoreBlock.USED)) {
                coreBlocks.add(new MultiBlockElement(checkState, coreBlock, checkPos));
                sumCoreQuality += coreBlock.getCoreQuality();
            } else {
                highlightBlock(checkPos, world);
            }
        }
        
        if (targetMachinePositions.size() == coreBlocks.size()) {
            // valid
            for (var core : coreBlocks) {
                var newState = core.state.setValue(MachineCoreBlock.USED, true);
                var coreEntity = (MachineCoreEntity) world.getBlockEntity(core.pos());
                coreEntity.setControllerPos(pos);
                world.setBlockAndUpdate(core.pos, newState);
                coreBlocksConnected.add(core.pos);
            }
            
            var quality = sumCoreQuality / coreBlocks.size();
            setCoreQuality(quality);
            
            Objects.requireNonNull(world).setBlockAndUpdate(pos, state.setValue(MultiblockMachine.ASSEMBLED, true));
            return true;
        } else {
            // invalid
            return false;
        }
    }
    
    default void onCoreBroken(BlockPos corePos) {
        
        var world = getWorldForMultiblock();
        var pos = getPosForMultiblock();
        var coreBlocksConnected = getConnectedCores();
        
        Objects.requireNonNull(world).setBlockAndUpdate(pos, world.getBlockState(pos).setValue(MultiblockMachine.ASSEMBLED, false));
        
        for (var core : coreBlocksConnected) {
            if (core.equals(corePos)) continue;
            
            var state = world.getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockAndUpdate(core, state.setValue(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
    }
    
    default void onControllerBroken() {
        
        var world = getWorldForMultiblock();
        var coreBlocksConnected = getConnectedCores();
        
        for (var core : coreBlocksConnected) {
            var state = Objects.requireNonNull(world).getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockAndUpdate(core, state.setValue(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
    }
    
    private void highlightBlock(BlockPos block, Level world) {
        ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3.atLowerCornerOf(block), null);
    }
    
    // this should be called on the server
    void triggerSetupAnimation();
    
    record MultiBlockElement(BlockState state, MachineCoreBlock coreBlock, BlockPos pos) {
    }
    
}
