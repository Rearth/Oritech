package rearth.oritech.api.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import rearth.oritech.Oritech;

// important: when implementing this class and the block has a GUI, make sure to call `this.sendUpdate(SyncType.GUI_OPEN);` in the `saveExtraData()` method.
// also ensure the `gui_tick` event type is sent from the screenhandler `sendContentUpdates` call, e.g. `blockEntity.sendUpdate(SyncType.GUI_TICK);`
public abstract class NetworkedBlockEntity extends BlockEntity implements BlockEntityTicker<NetworkedBlockEntity> {
    
    private boolean networkDirty = false;
    private boolean needsInitialUpdate = false;
    
    public NetworkedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // this should never be used in child classes, always use serverTick / clientTick
    @Override
    public void tick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {
        if (world.isClientSide) {
            clientTick(world, pos, state, blockEntity);
            return;
        }
        
        serverTick(world, pos, state, blockEntity);
        
        if ((world.getGameTime() + this.worldPosition.asLong()) % getSparseUpdateInterval() == 0)
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
    
    public abstract void serverTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity);
    public void clientTick(Level world, BlockPos pos, BlockState state, NetworkedBlockEntity blockEntity) {};
    
    public int getSparseUpdateInterval() {return 100;}
    
    @Override
    public void setChanged() {
        markDirty(false);
    }
    
    public void markDirty(boolean updateComparator) {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.getBlockState());
            if (updateComparator)
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        
        networkDirty = true;
    }
    
    public void preNetworkUpdate(SyncType type) {}
    
    public void sendUpdate(SyncType type) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        
        NetworkManager.sendBlockHandle(this, new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()));
    }
    
    public void sendUpdate(SyncType type, ServerPlayer player) {
        if (level == null) {
            Oritech.LOGGER.warn("unable to send player update: World is null.");
            return;
        }
        
        preNetworkUpdate(type);
        
        var usedBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        var fieldCount = NetworkManager.encodeFields(this, type, usedBuf, level);
        if (fieldCount == 0) return;
        
        NetworkManager.sendPlayerHandle(new NetworkManager.MessagePayload(worldPosition, BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(getType()), type, usedBuf.array()), player);
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        needsInitialUpdate = true;
        return super.getUpdateTag(registryLookup);
    }
}
