package rearth.oritech.util;

import com.mojang.authlib.GameProfile;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import rearth.oritech.util.item.containers.SimpleInventoryStorage;

public abstract class FakeMachinePlayer {
    @ExpectPlatform
    public static ServerPlayerEntity create(ServerWorld world, GameProfile profile) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static ServerPlayerEntity create(ServerWorld world, GameProfile profile, SimpleInventoryStorage inventory) {
        throw new AssertionError();
    }
}
