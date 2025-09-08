package rearth.oritech.block.entity.augmenter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.Attachment;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.PlayerAugments;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

// all events / methods here are called just on the server (except for refreshClient()). However the augments are also present and loaded
// on the client with all their data and recipe.
public abstract class Augment {
    
    public static final Attachment<Map<ResourceLocation, AugmentState>> ACTIVE_AUGMENTS_DATA = new Attachment<>() {
        @Override
        public ResourceLocation identifier() {
            return Oritech.id("playeraugments");
        }
        
        @Override
        public Codec<Map<ResourceLocation, AugmentState>> persistenceCodec() {
            return Codec.unboundedMap(ResourceLocation.CODEC, AugmentState.CODEC);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public StreamCodec<ByteBuf, Map<ResourceLocation, AugmentState>> networkCodec() {
            return ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, NetworkManager.getAutoCodec(AugmentState.class));
        }
        
        @Override
        public Supplier<Map<ResourceLocation, AugmentState>> initializer() {
            return HashMap::new;
        }
    };
    
    public static void registerAttachmentTypes() {
        AttachmentApi.register(ACTIVE_AUGMENTS_DATA);
        AttachmentApi.register(CustomAugmentsCollection.PORTAL_TARGET_TYPE);
    }
    
    public final ResourceLocation id;
    public final boolean toggleable;
    
    protected Augment(ResourceLocation id, boolean toggleable) {
        this.id = id;
        this.toggleable = toggleable;
    }
    
    public boolean isInstalled(Player player) {
        var data = AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA);
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return !state.equals(AugmentState.NOT_INSTALLED);
    }
    
    public void installToPlayer(Player player) {
        var data = new HashMap<>(AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA));
        data.put(id, AugmentState.ENABLED);
        AttachmentApi.setAttachment(player, ACTIVE_AUGMENTS_DATA, data);
        
        activate(player);
    }
    
    public void removeFromPlayer(Player player) {
        var data = new HashMap<>(AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA));
        data.put(id, AugmentState.NOT_INSTALLED);
        AttachmentApi.setAttachment(player, ACTIVE_AUGMENTS_DATA, data);
        
        deactivate(player);
    }
    
    public boolean isEnabled(Player player) {
        var data = AttachmentApi.getAttachmentValue(player, ACTIVE_AUGMENTS_DATA);
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return state.equals(AugmentState.ENABLED);
    }
    
    public void toggle(Player player) {
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
    }
    
    // this is called once when the augment is installed / enabled
    public abstract void activate(Player player);
    
    // this is called when the augment is removed / disabled
    public abstract void deactivate(Player player);
    
    // this is called every N ticks while the augment is enabled
    public abstract void refreshServer(Player player);
    
    public void refreshClient(Player player) {
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
