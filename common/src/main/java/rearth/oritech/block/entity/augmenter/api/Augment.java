package rearth.oritech.block.entity.augmenter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.network.NetworkContent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

// all events / methods here are called just on the server (except for refreshClient()). However the augments are also present and loaded
// on the client with all their data and recipe.
public abstract class Augment {
    
    public static final Attachment<Map<Identifier, AugmentState>> ACTIVE_AUGMENTS_DATA = new Attachment<>() {
        @Override
        public Identifier identifier() {
            return Oritech.id("playeraugments");
        }
        
        @Override
        public Codec<Map<Identifier, AugmentState>> persistenceCodec() {
            return Codec.unboundedMap(Identifier.CODEC, AugmentState.CODEC);
        }
        
        @Override
        public Supplier<Map<Identifier, AugmentState>> initializer() {
            return HashMap::new;
        }
    };
    
    public static void registerAttachmentTypes() {
        AttachmentApi.register(ACTIVE_AUGMENTS_DATA);
        AttachmentApi.register(CustomAugmentsCollection.PORTAL_TARGET_TYPE);
    }
    
    public final Identifier id;
    public final boolean toggleable;
    
    protected Augment(Identifier id, boolean toggleable) {
        this.id = id;
        this.toggleable = toggleable;
    }
    
    public boolean isInstalled(PlayerEntity player) {
        var data = AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA);
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return !state.equals(AugmentState.NOT_INSTALLED);
    }
    
    public void installToPlayer(PlayerEntity player) {
        var data = new HashMap<>(AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA));
        data.put(id, AugmentState.ENABLED);
        AttachmentApi.setAttachment(player, ACTIVE_AUGMENTS_DATA, data);
        syncToClient(player, data);
        
        activate(player);
    }
    
    public void removeFromPlayer(PlayerEntity player) {
        var data = new HashMap<>(AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA));
        data.put(id, AugmentState.NOT_INSTALLED);
        AttachmentApi.setAttachment(player, ACTIVE_AUGMENTS_DATA, data);
        syncToClient(player, data);
        
        deactivate(player);
    }
    
    public boolean isEnabled(PlayerEntity player) {
        var data = AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA);
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return state.equals(AugmentState.ENABLED);
    }
    
    public void toggle(PlayerEntity player) {
        var data = new HashMap<>(AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA));
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        if (state.equals(AugmentState.ENABLED)) {
            state = AugmentState.DISABLED;
            deactivate(player);
        } else if (state.equals(AugmentState.DISABLED)) {
            state = AugmentState.ENABLED;
            activate(player);
        }
        data.put(id, state);
        AttachmentApi.setAttachment(player, ACTIVE_AUGMENTS_DATA, data);
        syncToClient(player, data);
    }
    
    // this is called once when the augment is installed / enabled
    public abstract void activate(PlayerEntity player);
    
    // this is called when the augment is removed / disabled
    public abstract void deactivate(PlayerEntity player);
    
    // this is called every N ticks while the augment is enabled
    public abstract void refreshServer(PlayerEntity player);
    
    public void refreshClient(PlayerEntity player) {
    }
    
    public void syncToClient(PlayerEntity player, Map<Identifier, AugmentState> data) {
        NetworkContent.MACHINE_CHANNEL.serverHandle(player).send(new NetworkContent.AugmentPlayerStatePacket(data));
    }
    
    public abstract int refreshInterval();
    
    public enum AugmentState {
        ENABLED, DISABLED, NOT_INSTALLED;
        
        public static final Codec<AugmentState> CODEC = Codec.INT.flatXmap(
          id -> DataResult.success(AugmentState.values()[id]),
          augmentState -> DataResult.success(augmentState.ordinal())
        );
    }
    
}
