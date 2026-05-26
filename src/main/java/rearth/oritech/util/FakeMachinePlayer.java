package rearth.oritech.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.transfer.item.SimpleInventoryStorage;

public abstract class FakeMachinePlayer {

    public static ServerPlayer create(ServerLevel level, GameProfile profile, SimpleInventoryStorage inventory) {
        return OritechPlatform.INSTANCE.create(level, profile, inventory);
    }
}
