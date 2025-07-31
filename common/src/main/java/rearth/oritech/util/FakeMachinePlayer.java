package rearth.oritech.util;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import rearth.oritech.OritechPlatform;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;

public abstract class FakeMachinePlayer {

    public static ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        return OritechPlatform.INSTANCE.create(world, profile, inventory);
    }
}
