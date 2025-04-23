package rearth.oritech.api.attachment.fabric;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "unchecked"})
public class AttachmentApiImpl {
    
    private static final Map<Identifier, AttachmentType<?>> registeredTypes = new HashMap<>();
    
    public static <T> void register(Attachment<T> attachment) {
        
        var created = AttachmentRegistry.<T>builder()
                        .copyOnDeath()
                        .initializer(attachment.initializer())
                        .persistent(attachment.persistenceCodec())
                        .buildAndRegister(attachment.identifier());
        
        System.out.println("Registered attachment fabric impl: " + attachment.identifier());
        
        registeredTypes.put(attachment.identifier(), created);
        
    }
    
    public static <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = registeredTypes.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Querying attachment that has not been registered: {}", attachment.identifier());
            return false;
        }
        return entity.hasAttached(type);
    }
    
    public static <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        AttachmentType<T> type = (AttachmentType<T>) registeredTypes.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Getting attachment that has not been registered: {}", attachment.identifier());
            return null;
        }
        return entity.getAttached(type);
    }
    
    public static <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        AttachmentType<T> type = (AttachmentType<T>) registeredTypes.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Setting attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.setAttached(type, value);
    }
    
    public static <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = registeredTypes.get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Removing attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.removeAttached(type);
    }
}
