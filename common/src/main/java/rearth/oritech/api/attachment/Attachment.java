package rearth.oritech.api.attachment;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public interface Attachment<A> {
    
    ResourceLocation identifier();
    
    Codec<A> persistenceCodec();
    
    StreamCodec<ByteBuf, A> networkCodec();
    
    Supplier<A> initializer();

}
