package rearth.oritech.block.entity.machines.accelerator;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.init.BlockContent;
import rearth.oritech.init.BlockEntitiesContent;
import rearth.oritech.network.NetworkContent;

public class BlackHoleBlockEntity extends BlockEntity implements BlockEntityTicker<BlackHoleBlockEntity> {
    public BlockPos currentlyPullingFrom;
    public BlockState currentlyPulling;
    public long pullingStartedAt;
    public long pullTime;
    
    // if nothing is in influence, don't search so often
    private int waitTicks;
    
    public BlackHoleBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntitiesContent.BLACK_HOLE_ENTITY, pos, state);
    }
    
    @Override
    public void tick(World world, BlockPos pos, BlockState state, BlackHoleBlockEntity blockEntity) {
        if (world.isClient || waitTicks-- > 0) return;
        
        if (currentlyPullingFrom != null && pullingStartedAt + pullTime < world.getTime()) {
            currentlyPullingFrom = null;
        }
        
        if (currentlyPullingFrom != null) return;

        int pullRange = Oritech.CONFIG.pullRange();

        for (var candidate : BlockPos.iterateOutwards(pos, pullRange, pullRange, pullRange)) {
            var candidateState = world.getBlockState(candidate);
            if (candidate.equals(pos) || candidateState.isAir() || candidateState.isLiquid() || candidateState.getBlock().equals(BlockContent.BLACK_HOLE_BLOCK)) continue;
            
            currentlyPullingFrom = candidate;
            currentlyPulling = candidateState;
            pullingStartedAt = world.getTime();
            pullTime = (long) candidate.getManhattanDistance(pos) * Oritech.CONFIG.pullTimeMultiplier();
            NetworkContent.MACHINE_CHANNEL.serverHandle(this).send(new NetworkContent.BlackHoleSuckPacket(pos, currentlyPullingFrom, pullingStartedAt, pullTime));
            world.setBlockState(candidate, Blocks.AIR.getDefaultState());
            return;
        }
        
        if (currentlyPullingFrom == null) {
            waitTicks = Oritech.CONFIG.idleWaitTicks();
        }
    }
    
    public void onClientPullEvent(NetworkContent.BlackHoleSuckPacket packet) {
        this.currentlyPullingFrom = packet.from();
        this.pullTime = packet.duration();
        this.pullingStartedAt = world.getTime();
        this.currentlyPulling = world.getBlockState(packet.from());
    }
    
}
