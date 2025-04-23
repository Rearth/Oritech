package rearth.oritech.api.attachment.neoforge;

import net.minecraft.entity.LivingEntity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;

@SuppressWarnings({"unchecked"})
public class AttachmentApiImpl {
    
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Oritech.MOD_ID);
    
    public static <T> void register(Attachment<T> attachment) {
        
        ATTACHMENT_TYPES.register(attachment.identifier().getPath(), () ->
                                                                       AttachmentType
                                                                         .builder(attachment.initializer())
                                                                         .serialize(attachment.persistenceCodec())
                                                                         .copyOnDeath()
                                                                         .build());
        
    }
    
    public static <T> boolean hasAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Querying attachment that has not been registered: {}", attachment.identifier());
            return false;
        }
        return entity.hasData(type);
    }
    
    public static <T> T getAttachmentValue(LivingEntity entity, Attachment<T> attachment) {
        var type = (AttachmentType<T>) ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Getting attachment that has not been registered: {}", attachment.identifier());
            return null;
        }
        return entity.getData(type);
    }
    
    public static <T> void setAttachment(LivingEntity entity, Attachment<T> attachment, T value) {
        var type = (AttachmentType<T>) ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Setting attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.setData(type, value);
    }
    
    public static <T> void removeAttachment(LivingEntity entity, Attachment<T> attachment) {
        var type = ATTACHMENT_TYPES.getRegistry().get().get(attachment.identifier());
        if (type == null) {
            Oritech.LOGGER.warn("Removing attachment that has not been registered: {}", attachment.identifier());
            return;
        }
        entity.removeData(type);
    }
}
