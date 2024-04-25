package rearth.oritech.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import rearth.oritech.block.base.block.MultiblockMachine;
import rearth.oritech.block.blocks.MachineCoreBlock;
import rearth.oritech.block.entity.machines.MachineCoreEntity;
import rearth.oritech.client.init.ParticleContent;
import team.reborn.energy.api.EnergyStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public interface MultiblockMachineController {
    
    List<Vec3i> getCorePositions();
    Direction getFacingForMultiblock();
    BlockPos getMachinePos();
    World getMachineWorld();
    ArrayList<BlockPos> getConnectedCores();
    void setCoreQuality(float quality);
    float getCoreQuality();
    InventoryProvider getInventoryForLink();
    EnergyStorage getEnergyStorageForLink();
    
    default void addMultiblockToNbt(NbtCompound nbt) {
        
        var posList = new NbtList();
        for (var pos : getConnectedCores()) {
            var posTag = new NbtCompound();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            posList.add(posTag);
        }
        nbt.put("connectedCores", posList);
        nbt.putFloat("coreQuality", getCoreQuality());
    }
    
    default void loadMultiblockNbtData(NbtCompound nbt) {
        
        var posList = nbt.getList("connectedCores", NbtElement.COMPOUND_TYPE);
        var coreBlocksConnected = getConnectedCores();
        
        for (var posTag : posList) {
            var posCompound = (NbtCompound) posTag;
            var x = posCompound.getInt("x");
            var y = posCompound.getInt("y");
            var z = posCompound.getInt("z");
            var pos = new BlockPos(x, y, z);
            coreBlocksConnected.add(pos);
        }
        
        setCoreQuality(nbt.getFloat("coreQuality"));
    }
    
    default Boolean tryPlaceNextCore(PlayerEntity player) {
        
        var heldStack = player.getEquippedStack(EquipmentSlot.MAINHAND);
        var heldItem = heldStack.getItem();
        
        if (!(heldItem instanceof BlockItem blockItem)) return false;
        
        if (blockItem.getBlock() instanceof MachineCoreBlock) {
            var nextPosition = this.getNextMissingCore();
            if (nextPosition != null) {
                this.getMachineWorld().setBlockState(nextPosition, blockItem.getBlock().getDefaultState());
                if (!player.isCreative()) {
                    heldStack.decrement(1);
                    if (heldStack.getCount() == 0)
                        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
                return true;
            }
        }
        return false;
    }
    
    default BlockPos getNextMissingCore() {
        
        var world = getMachineWorld();
        var pos = getMachinePos();
        
        var ownFacing = getFacingForMultiblock();
        var targetMachinePositions = getCorePositions();
        
        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.add(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            if (checkState.isOf(Blocks.AIR) || checkState.isIn(TagKey.of(RegistryKeys.BLOCK, new Identifier("minecraft", "replaceable")))) {
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
        
        if (state.get(MultiblockMachine.ASSEMBLED)) return true;
        var world = getMachineWorld();
        var pos = getMachinePos();
        var coreBlocksConnected = getConnectedCores();
        
        var ownFacing = getFacingForMultiblock();
        
        var targetMachinePositions = getCorePositions();
        var coreBlocks = new ArrayList<MultiBlockElement>(targetMachinePositions.size());
        
        var sumCoreQuality = 0f;
        
        for (var targetMachinePosition : targetMachinePositions) {
            var rotatedPos = Geometry.rotatePosition(targetMachinePosition, ownFacing);
            var checkPos = pos.add(rotatedPos);
            var checkState = Objects.requireNonNull(world).getBlockState(checkPos);
            
            var blockType = checkState.getBlock();
            if (blockType instanceof MachineCoreBlock coreBlock && !checkState.get(MachineCoreBlock.USED)) {
                coreBlocks.add(new MultiBlockElement(checkState, coreBlock, checkPos));
                sumCoreQuality += coreBlock.getCoreQuality();
            } else {
                highlightBlock(checkPos, world);
            }
        }
        
        if (targetMachinePositions.size() == coreBlocks.size()) {
            // valid
            for (var core : coreBlocks) {
                var newState = core.state.with(MachineCoreBlock.USED, true);
                var coreEntity = (MachineCoreEntity) world.getBlockEntity(core.pos());
                coreEntity.setControllerPos(pos);
                world.setBlockState(core.pos, newState);
                coreBlocksConnected.add(core.pos);
            }
            
            var quality = sumCoreQuality / coreBlocks.size();
            setCoreQuality(quality);
            
            Objects.requireNonNull(world).setBlockState(pos, state.with(MultiblockMachine.ASSEMBLED, true));
            return true;
        } else {
            // invalid
            return false;
        }
    }
    
    default void onCoreBroken(BlockPos corePos) {
        
        var world = getMachineWorld();
        var pos = getMachinePos();
        var coreBlocksConnected = getConnectedCores();
        
        Objects.requireNonNull(world).setBlockState(pos, world.getBlockState(pos).with(MultiblockMachine.ASSEMBLED, false));
        
        for (var core : coreBlocksConnected) {
            if (core.equals(corePos)) continue;
            
            var state = world.getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockState(core, state.with(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
    }
    
    default void onControllerBroken() {
        
        var world = getMachineWorld();
        var coreBlocksConnected = getConnectedCores();
        
        for (var core : coreBlocksConnected) {
            var state = Objects.requireNonNull(world).getBlockState(core);
            if (state.getBlock() instanceof MachineCoreBlock) {
                world.setBlockState(core, state.with(MachineCoreBlock.USED, false));
            }
        }
        
        coreBlocksConnected.clear();
    }
    
    private void highlightBlock(BlockPos block, World world) {
        ParticleContent.HIGHLIGHT_BLOCK.spawn(world, Vec3d.of(block), null);
    }
    
    void playSetupAnimation();
    
    record MultiBlockElement(BlockState state, MachineCoreBlock coreBlock, BlockPos pos) {}
    
}
