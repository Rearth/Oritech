package rearth.oritech.api.attachment;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

public interface Attachment<A> {
    
    ResourceLocation identifier();
    
    Codec<A> persistenceCodec();
    
    Supplier<A> initializer();

}
