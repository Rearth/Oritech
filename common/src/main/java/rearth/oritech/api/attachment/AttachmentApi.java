package rearth.oritech.api.attachment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;

public abstract class AttachmentApi {
    
    @ExpectPlatform
    public static <T> void register(Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        throw new AssertionError();
    }
    
    @ExpectPlatform
    public static <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
}
