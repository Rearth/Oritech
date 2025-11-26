package rearth.oritech.api.attachment;

import net.minecraft.world.entity.LivingEntity;
import rearth.oritech.OritechPlatform;

public interface AttachmentApi {
    
    /**
     * Registers an attachment type with the platform-specific APIs.
     * This should be called during startup.
     *
     * @param <T>       The type of the attachment.
     * @param attachment The attachment type to register.
     */
    static <T> void register(Attachment<T> attachment) {
        OritechPlatform.INSTANCE.register(attachment);
    }
    
    /**
     * Checks if the attachment type is registered and attached to the entity.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to check.
     * @param attachment The attachment to check for.
     * @return True if the attachment type is registered and attached to the entity, false otherwise.
     */
    static <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        return OritechPlatform.INSTANCE.hasAttachment(entity, attachment);
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
    static <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        return OritechPlatform.INSTANCE.getAttachmentValue(entity, attachment);
    }
    
    /**
     * Sets the attachment value for the entity. If it is not present already, it will be added.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to set the attachment value for.
     * @param attachment The attachment to set.
     * @param value     The value to set for the attachment.
     */
    static <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        OritechPlatform.INSTANCE.setAttachment(entity, attachment, value);
    }
    
    /**
     * Completely removes the attachment from the entity. Does nothing if the attachment is not present.
     * Throws an exception if the attachment type has not been registered first.
     *
     * @param <T>       The type of the attachment.
     * @param entity    The entity to remove the attachment from.
     * @param attachment The attachment to remove.
     */
    static <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        OritechPlatform.INSTANCE.removeAttachment(entity, attachment);
    }
}
