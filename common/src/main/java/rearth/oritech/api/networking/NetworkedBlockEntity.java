package rearth.oritech.api.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;

// important: when implementing this class and the block has a GUI, make sure to call `this.sendUpdate(SyncType.GUI_OPEN);` in the `saveExtraData()` method.
// also ensure the `gui_tick` event type is sent from the screenhandler `sendContentUpdates` call.
public abstract class NetworkedBlockEntity extends BlockEntity implements BlockEntityTicker<NetworkedBlockEntity> {
    
    private boolean networkDirty = false;
    private boolean needsInitialUpdate = false;
    
    public NetworkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // this should never be used in child classes, always use serverTick / clientTick
    @Override
    public void tick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClient) {
            clientTick(world, pos, state, blockEntity);
            return;
        }
        
        serverTick(world, pos, state, blockEntity);
        
        if ((world.getTime() + this.pos.asLong()) % getSparseUpdateInterval() == 0)
            sendUpdate(SyncType.SPARSE_TICK);
        
        if (networkDirty) {
            networkDirty = false;
            sendUpdate(SyncType.TICK);
        }
        if (needsInitialUpdate) {
            needsInitialUpdate = false;
            sendUpdate(SyncType.INITIAL);
        }
    }
    
    public abstract void serverTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity);
    public void clientTick(World world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {};
    
    public int getSparseUpdateInterval() {return 100;}
    
    @Override
    public void markDirty() {
        markDirty(false);
    }
    
    public void markDirty(boolean updateComparator) {
        if (this.world != null) {
            markDirty(this.world, this.pos, this.getCachedState());
            if (updateComparator)
                world.updateComparators(pos, getCachedState().getBlock());
        }
        
        networkDirty = true;
    }
    
    public void preNetworkUpdate(SyncType type) {}
    
    public void sendUpdate(SyncType type) {
        if (world == null) {
            Oritech.LOGGER.warn("unable to send update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf);
        if (fieldCount == 0) return;
        NetworkManager.sendBlockHandle(this, new NetworkManager.MessagePayload(pos, Registries.BLOCK_ENTITY_TYPE.getId(getType()), type, usedBuf.array()));
    }
    
    public void sendUpdate(SyncType type, ServerPlayerEntity player) {
        if (world == null) {
            Oritech.LOGGER.warn("unable to send player update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf);
        if (fieldCount == 0) return;
        NetworkManager.sendPlayerHandle(new NetworkManager.MessagePayload(pos, Registries.BLOCK_ENTITY_TYPE.getId(getType()), type, usedBuf.array()), player);
    }
    
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        needsInitialUpdate = true;
        return super.toInitialChunkDataNbt(registryLookup);
    }
}
