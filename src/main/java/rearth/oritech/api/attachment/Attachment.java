package rearth.oritech.api.attachment;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

public interface Attachment<A> {
    
    Identifier identifier();
    
    Codec<A> persistenceCodec();
    
    StreamCodec<ByteBuf, A> networkCodec();
    
    Supplier<A> initializer();

}
