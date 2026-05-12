package rearth.oritech.api.networking;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.Oritech;

public interface LevelPacketCodec<I, T> extends StreamCodec<I, T> {
    
    default T decode(I buf) {
        Oritech.LOGGER.warn("Using non-level variant of level packet codec for decode");
        return decode(buf, null);
    }
    default void encode(I buf, T value) {
        Oritech.LOGGER.warn("Using non-level variant of level packet codec for encode");
        encode(buf, value, null);
    }
    
    T decode(I buf, @Nullable Level level);
    void encode(I buf, T value, @Nullable Level level);
    
}
