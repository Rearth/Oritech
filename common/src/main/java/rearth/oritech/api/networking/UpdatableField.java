package rearth.oritech.api.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;

// full update is send when GUI is opened, and when the block is loaded for the first time.
// R is expected to just update relevant info, and T is usually just returns its own instance.
public interface UpdatableField<T, R> {
    
    R getDeltaData();
    T getFullData();
    PacketCodec<? extends ByteBuf, R> getDeltaCodec();
    PacketCodec<? extends ByteBuf, T> getFullCodec();
    
    default boolean useDeltaOnly(SyncType type) {
        return type.equals(SyncType.TICK) || type.equals(SyncType.GUI_TICK) || type.equals(SyncType.SPARSE_TICK);
    }
    
    void handleFullUpdate(T updatedData);
    void handleDeltaUpdate(R updatedData);
    
}
