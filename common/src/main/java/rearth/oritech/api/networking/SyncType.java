package rearth.oritech.api.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public enum SyncType {
    
    INITIAL, TICK, SPARSE_TICK, GUI_TICK, GUI_OPEN;
    
    public static PacketCodec<RegistryByteBuf, SyncType> PACKET_CODEC = new PacketCodec<>() {
        @Override
        public SyncType decode(RegistryByteBuf buf) {
            return SyncType.values()[buf.readUnsignedShort()];
        }
        
        @Override
        public void encode(RegistryByteBuf buf, SyncType value) {
            buf.writeShort(value.ordinal());
        }
    };
    
}
