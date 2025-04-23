package rearth.oritech.api.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;

import java.util.function.Supplier;

public interface Attachment<A> {
    
    Identifier identifier();
    
    Codec<A> persistenceCodec();
    
    Supplier<A> initializer();

}
