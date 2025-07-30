package rearth.oritech.util;

import com.mojang.authlib.GameProfile;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;

public abstract class FakeMachinePlayer {

    @ExpectPlatform
    public static ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        throw new AssertionError();
    }
}
