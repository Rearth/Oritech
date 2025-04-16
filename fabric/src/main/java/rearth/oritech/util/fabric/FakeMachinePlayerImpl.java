package rearth.oritech.util.fabric;

import java.util.Map;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import rearth.oritech.util.item.containers.SimpleInventoryStorage;

// inspired by Fabric's FakePlayer and Neoforge's FakePlayerFactory

// the only difference between this and the Neoforge Impl is the platform's version of FakePlayer that is being extended.
public class FakeMachinePlayerImpl extends FakePlayer {
    private static final Map<FakeMachinePlayerKey, FakePlayer> fakeMachinePlayers = new MapMaker().weakValues().makeMap();
    private record FakeMachinePlayerKey (ServerWorld world, GameProfile profile) {}
    
    private final SimpleInventoryStorage inventory;

    private FakeMachinePlayerImpl(ServerWorld world, GameProfile profile, SimpleInventoryStorage inventory) {
        super(world, profile);
        this.inventory = inventory;
    }

    public static ServerPlayerEntity create(ServerWorld world, GameProfile profile, SimpleInventoryStorage inventory) {
        FakeMachinePlayerKey key = new FakeMachinePlayerKey(world, profile);
        return fakeMachinePlayers.computeIfAbsent(key, k -> new FakeMachinePlayerImpl(k.world(), k.profile(), inventory));
	}

    @Override
    public boolean giveItemStack(ItemStack itemStack) {
        this.inventory.insert(itemStack, false);
        return true;
    }
}
