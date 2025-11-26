package rearth.oritech.block.entity.augmenter;

import io.wispforest.owo.network.ClientAccess;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.api.Augment;

import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

// for stupid reasons that I don't understand moving this to another second method helps somehow, otherwise we get crashes on server environments because it's loading
// a ClientPlayerEntity class?
public class PlayerAugmentsClient {
    
    public static void receiveAugmentState(Map<ResourceLocation, Augment.AugmentState> data) {
        AttachmentApi.setAttachment(Minecraft.getInstance().player, Augment.ACTIVE_AUGMENTS_DATA, data);
    }
}
