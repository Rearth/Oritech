package rearth.oritech.api.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum SyncType {
    
    INITIAL, TICK, SPARSE_TICK, GUI_TICK, GUI_OPEN;
    
    public static StreamCodec<RegistryFriendlyByteBuf, SyncType> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public SyncType decode(RegistryFriendlyByteBuf buf) {
            return SyncType.values()[buf.readUnsignedShort()];
        }
        
        @Override
        public void encode(RegistryFriendlyByteBuf buf, SyncType value) {
            buf.writeShort(value.ordinal());
        }
    };
    
}
