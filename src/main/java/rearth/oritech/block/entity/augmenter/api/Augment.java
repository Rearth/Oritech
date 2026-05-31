package rearth.oritech.block.entity.augmenter.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import rearth.oritech.init.AttachmentContent;

import java.util.HashMap;
import java.util.Map;

// all events / methods here are called just on the server (except for refreshClient()). However the augments are also present and loaded
// on the client with all their data and recipe.
public abstract class Augment {

    public final Identifier id;
    public final boolean toggleable;

    protected Augment(Identifier id, boolean toggleable) {
        this.id = id;
        this.toggleable = toggleable;
    }

    public boolean isInstalled(Player player) {
        var data = player.getData(AttachmentContent.ACTIVE_AUGMENTS);
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return !state.equals(AugmentState.NOT_INSTALLED);
    }

    public void installToPlayer(Player player) {
        var data = new HashMap<>(player.getData(AttachmentContent.ACTIVE_AUGMENTS));
        data.put(id, AugmentState.ENABLED);
        player.setData(AttachmentContent.ACTIVE_AUGMENTS, data);

        activate(player);
    }

    public void removeFromPlayer(Player player) {
        var data = new HashMap<>(player.getData(AttachmentContent.ACTIVE_AUGMENTS));
        data.put(id, AugmentState.NOT_INSTALLED);
        player.setData(AttachmentContent.ACTIVE_AUGMENTS, data);

        deactivate(player);
    }

    public boolean isEnabled(Player player) {
        var data = player.getData(AttachmentContent.ACTIVE_AUGMENTS);
        return isEnabled(data);
    }

    public boolean isEnabled(Map<Identifier, AugmentState> playerData) {
        var state = playerData.getOrDefault(id, AugmentState.NOT_INSTALLED);
        return state.equals(AugmentState.ENABLED);
    }

    public void toggle(Player player) {
        var data = new HashMap<>(player.getData(AttachmentContent.ACTIVE_AUGMENTS));
        var state = data.getOrDefault(id, AugmentState.NOT_INSTALLED);
        if (state.equals(AugmentState.ENABLED)) {
            state = AugmentState.DISABLED;
            deactivate(player);
        } else if (state.equals(AugmentState.DISABLED)) {
            state = AugmentState.ENABLED;
            activate(player);
        }
        data.put(id, state);
        player.setData(AttachmentContent.ACTIVE_AUGMENTS, data);
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
