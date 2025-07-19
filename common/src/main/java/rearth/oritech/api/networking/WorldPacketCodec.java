package rearth.oritech.api.networking;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public interface WorldPacketCodec<I, T> extends PacketCodec<I, T> {
    
    default T decode(I buf) {
        System.out.println("warning: using non-world variant of world packet codec");
        return decode(buf, null);
    }
    default void encode(I buf, T value) {
        System.out.println("warning: using non-world variant of world packet codec");
        encode(buf, value, null);
    }
    
    T decode(I buf, @Nullable World world);
    void encode(I buf, T value, @Nullable World world);
    
}
