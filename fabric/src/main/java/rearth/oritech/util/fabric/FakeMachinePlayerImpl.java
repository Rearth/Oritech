package rearth.oritech.util.fabric;

import java.util.Map;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import rearth.oritech.api.item.containers.SimpleInventoryStorage;

// inspired by Fabric's FakePlayer and Neoforge's FakePlayerFactory

// the only difference between this and the Neoforge Impl is the platform's version of FakePlayer that is being extended.
public class FakeMachinePlayerImpl extends FakePlayer {
    private static final Map<FakeMachinePlayerKey, FakePlayer> FAKE_MACHINE_PLAYERS = new MapMaker().weakValues().makeMap();
    private record FakeMachinePlayerKey (ServerLevel world, GameProfile profile) {}
    
    private final SimpleInventoryStorage inventory;

    private FakeMachinePlayerImpl(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        super(world, profile);
        this.inventory = inventory;
    }

    public static ServerPlayer create(ServerLevel world, GameProfile profile, SimpleInventoryStorage inventory) {
        FakeMachinePlayerKey key = new FakeMachinePlayerKey(world, profile);
        return FAKE_MACHINE_PLAYERS.computeIfAbsent(key, k -> new FakeMachinePlayerImpl(k.world(), k.profile(), inventory));
	}

    @Override
    public boolean addItem(ItemStack itemStack) {
        this.inventory.insert(itemStack, false);
        return true;
    }
}
