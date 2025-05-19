package rearth.oritech.api.attachment;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;

public abstract class AttachmentApi {
    
    /**
     * Registers an attachment type with the platform-specific APIs.
     * This should be called during startup.
     *
     * @param <T>       The type of the attachment.
     * @param attachment The attachment type to register.
     */
    @ExpectPlatform
    public static <T> void register(Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    /**
     * Checks if the attachment type is registered and attached to the entity.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to check.
     * @param attachment The attachment to check for.
     * @return True if the attachment type is registered and attached to the entity, false otherwise.
     */
    @ExpectPlatform
    public static <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    /**
     * Retrieves the attachment value for the entity. If the attachment is not present,
     * it will be initialized with the default value and then attached to the entity.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to retrieve the attachment value for.
     * @param attachment The attachment to retrieve.
     * @return The attachment value for the entity.
     */
    @ExpectPlatform
    public static <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
    
    /**
     * Sets the attachment value for the entity. If it is not present already, it will be added.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to set the attachment value for.
     * @param attachment The attachment to set.
     * @param value     The value to set for the attachment.
     */
    @ExpectPlatform
    public static <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        throw new AssertionError();
    }
    
    /**
     * Completely removes the attachment from the entity. Does nothing if the attachment is not present.
     * Throws an exception if the attachment type has not been registered first.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to remove the attachment from.
     * @param attachment The attachment to remove.
     */
    @ExpectPlatform
    public static <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        throw new AssertionError();
    }
}
