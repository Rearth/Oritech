package rearth.oritech.block.entity.augmenter;

import io.wispforest.owo.network.ClientAccess;
import net.minecraft.util.Identifier;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.block.entity.augmenter.api.Augment;

import java.util.Map;

// for stupid reasons that I don't understand moving this to another second method helps somehow, otherwise we get crashes on server environments because it's loading
// a ClientPlayerEntity class?
public class PlayerAugmentsClient {
    
    public static void setPlayerAugment(ClientAccess access, Map<Identifier, Augment.AugmentState> state) {
        AttachmentApi.setAttachment(access.player(), Augment.ACTIVE_AUGMENTS_DATA, state);
    }
    
}
