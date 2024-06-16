package rearth.oritech.block.entity.machines.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MultiblockFrameInteractionEntity;
import rearth.oritech.client.init.ModScreens;
import rearth.oritech.client.init.ParticleContent;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;
import rearth.oritech.util.ScreenProvider;

import java.util.List;
import java.util.Objects;

public class DestroyerBlockEntity extends MultiblockFrameInteractionEntity {
    
    public boolean hasCropFilterAddon;
    public int range = 1;
    
    // non-persistent
    public BlockPos quarryTarget = BlockPos.ORIGIN;
    public float targetHardness = 1f;
    
    public DestroyerBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.DESTROYER_BLOCK_ENTITY, pos, state);
    }
    
    @Override
    public void gatherAddonStats(List<AddonBlock> addons) {
        range = 1;
        super.gatherAddonStats(addons);
        
        System.out.println("range: " + range);
    }
    
    @Override
    public void getAdditionalStatFromAddon(AddonBlock addonBlock) {
        if (addonBlock.state().getBlock().equals(BlockContent.CROP_FILTER_ADDON))
            hasCropFilterAddon = true;
        
        if (addonBlock.state().getBlock().equals(BlockContent.QUARRY_ADDON)) {
            range *= 8;
        }
        
        
        super.getAdditionalStatFromAddon(addonBlock);
    }
    
    @Override
    public void resetAddons() {
        super.resetAddons();
        hasCropFilterAddon = false;
        range = 1;
    }
    
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("cropAddon", hasCropFilterAddon);
        nbt.putInt("range", range);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasCropFilterAddon = nbt.getBoolean("cropAddon");
        range = nbt.getInt("range");
    }
    
    @Override
    protected boolean hasWorkAvailable(BlockPos toolPosition) {
        
        if (range > 1) {
            return hasQuarryTarget(toolPosition);
        }
        
        var targetPosition = toolPosition.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        // skip not grown crops
        if (hasCropFilterAddon && targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
            return false;
        }
        
        return !targetState.getBlock().equals(Blocks.AIR);
    }
    
    private boolean hasQuarryTarget(BlockPos toolPosition) {
        return getQuarryDownwardState(toolPosition) != null;
    }
    
    private Pair<BlockPos, BlockState> getQuarryDownwardState(BlockPos toolPosition) {
        for (int i = 1; i <= range; i++) {
            var checkPos = toolPosition.down(i);
            var targetState = world.getBlockState(checkPos);
            if (!targetState.getBlock().equals(Blocks.AIR) && !targetState.isLiquid()) {  // pass through both air and liquid
                quarryTarget = checkPos;
                targetHardness = targetState.getHardness(world, checkPos);
                syncQuarryNetworkData();
                return new Pair<>(checkPos, targetState);
            }
        }
        
        quarryTarget = BlockPos.ORIGIN;
        return null;
    }
    
    @Override
    public void finishBlockWork(BlockPos processed) {
        
        var targetPosition = processed.down();
        var targetState = Objects.requireNonNull(world).getBlockState(targetPosition);
        
        if (range > 1) {
            var data = getQuarryDownwardState(processed);
            if (data == null) return;
            targetPosition = data.getLeft();
            targetState = data.getRight();
        }
        
        // remove fluids
        if (targetState.isLiquid()) {
            world.setBlockState(targetPosition, Blocks.AIR.getDefaultState());
        }
        
        var targetHardness = targetState.getBlock().getHardness();
        if (targetHardness < 0) return;    // skip undestroyable blocks, such as bedrock
        
        // skip not grown crops
        if (range == 1 && hasCropFilterAddon && targetState.getBlock() instanceof CropBlock cropBlock && !cropBlock.isMature(targetState)) {
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
        
        if (moving)
            return;
        
        if (range > 1 && quarryTarget != BlockPos.ORIGIN) {
            ParticleContent.QUARRY_DESTROY_EFFECT.spawn(world, Vec3d.ofCenter(quarryTarget).add(0, 0.5, 0), 3);
        } else if (hasWorkAvailable(getCurrentTarget())) {
            ParticleContent.BLOCK_DESTROY_EFFECT.spawn(world, Vec3d.of(getCurrentTarget().down()), 4);
        }
    }
    
    @Override
    public List<Pair<Text, Text>> getExtraExtensionLabels() {
        
        if (range == 1) return super.getExtraExtensionLabels();
        
        return List.of(new Pair<>(Text.literal(range + " Range"), Text.literal("Maximum digging depth")));
    }
    
    @Override
    public BlockState getMachineHead() {
        return BlockContent.BLOCK_DESTROYER_HEAD.getDefaultState();
    }
    
    @Override
    public List<ScreenProvider.GuiSlot> getGuiSlots() {
        return List.of(
          new GuiSlot(0, 117, 20),
          new GuiSlot(1, 117, 38),
          new GuiSlot(2, 117, 56));
    }
    
    @Override
    public int getInventorySize() {
        return 3;
    }
    
    @Override
    public List<Vec3i> getAddonSlots() {
        return List.of(
          new Vec3i(0, 0, -2),
          new Vec3i(-1, 0, -1),
          new Vec3i(0, 0, 2),
          new Vec3i(-1, 0, 1)
        );
    }
    
    @Override
    public int getMoveTime() {
        return Math.max((int) (Oritech.CONFIG.destroyerConfig.moveDuration() * this.getSpeedMultiplier()), 1);
    }
    
    @Override
    public int getWorkTime() {
        return (int) (Oritech.CONFIG.destroyerConfig.workDuration() * this.getSpeedMultiplier() * Math.pow(targetHardness,  0.3f));
    }
    
    @Override
    public int getMoveEnergyUsage() {
        return Oritech.CONFIG.destroyerConfig.moveEnergyUsage();
    }
    
    @Override
    public int getOperationEnergyUsage() {
        return Oritech.CONFIG.destroyerConfig.workEnergyUsage();
    }
    
    @Override
    public ScreenHandlerType<?> getScreenHandlerType() {
        return ModScreens.DESTROYER_SCREEN;
    }
    
    @Override
    public List<Vec3i> getCorePositions() {
        return List.of(
          new Vec3i(0, 0, -1),
          new Vec3i(0, 0, 1)
        );
    }
    
    @Override
    public void updateNetwork() {
        super.updateNetwork();
        syncQuarryNetworkData();
    }
    
    private void syncQuarryNetworkData() {
        NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.QuarryTargetPacket(pos, quarryTarget, range, getBaseAddonData().speed()));
    }
    
    @Override
    public void playSetupAnimation() {
    
    }
}
