package rearth.oritech.spaceage.block.hologram;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.network.connection.ConnectionType;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.spaceage.block.mission_control.GroundStationBlockEntity;
import rearth.oritech.spaceage.init.SpaceAgeBlocks;
import rearth.oritech.spaceage.network.HoloNetworking;
import rearth.oritech.spaceage.network.HoloNetworking.Configure;
import rearth.oritech.spaceage.network.HoloNetworking.Projection;
import rearth.oritech.spaceage.simulation.SpaceSimulationSavedData;

import java.util.UUID;

public final class HoloGameTests {

    private HoloGameTests() {
    }

    public static void projector(GameTestHelper test) {

        var level = test.getLevel();
        var origin = test.absolutePos(new BlockPos(1, 2, 1));
        var controllerPos = origin.west();
        var stationPos = controllerPos.west();
        for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 3; z++) level.setBlockAndUpdate(origin.offset(x, 0, z), SpaceAgeBlocks.HOLO_SCREEN.get().defaultBlockState());
        }
        level.setBlockAndUpdate(controllerPos, SpaceAgeBlocks.HOLO_CONTROLLER.get().defaultBlockState());
        var controller = (HoloControllerBlockEntity) level.getBlockEntity(controllerPos);
        test.assertTrue(controller.buildSnapshot().status().equals("no_station"), "A screen alone does not create a second mission control");
        level.setBlockAndUpdate(stationPos, SpaceAgeBlocks.MISSION_CONTROL.get().defaultBlockState());
        var station = (GroundStationBlockEntity) level.getBlockEntity(stationPos);
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "HoloOwner"));
        player.setPos(controllerPos.getX() + .5, controllerPos.getY() + .5, controllerPos.getZ() + .5);
        station.owner = player.getUUID();
        var menu = new HoloControllerMenu(1, player.getInventory(), controllerPos);
        player.containerMenu = menu;
        var system = SpaceSimulationSavedData.getForPlayer(player);
        var before = system.createObjectData();
        var snapshot = controller.buildSnapshot();
        test.assertTrue(snapshot.status().equals("ready") && snapshot.surface().width() * snapshot.surface().depth() == 12,
                "Unpowered controller projects over its connected screen rectangle");
        test.assertTrue(snapshot.objects().equals(before), "Projection uses exactly the owner's discovered map data");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        try {
            var codec = NetworkManager.getAutoCodec(Projection.class);
            codec.encode(buffer, new Projection(controllerPos, snapshot));
            var decoded = (Projection) codec.decode(buffer);
            test.assertTrue(snapshot.equals(decoded.snapshot()), "Real projection packet round-trips through the registry codec");
        } finally {
            buffer.release();
        }
        test.assertTrue(HoloNetworking.configure(player, new Configure(controllerPos, HoloSettings.DEFAULT, true)), "Owner can configure the display");
        var tag = controller.saveWithFullMetadata(level.registryAccess());
        var restored = (HoloControllerBlockEntity) BlockEntity.loadStatic(controllerPos, controller.getBlockState(), tag, level.registryAccess());
        test.assertTrue(restored != null && restored.settings.equals(controller.settings), "Projector settings survive a block-entity save/reload");
        station.owner = UUID.randomUUID();
        test.assertTrue(!HoloNetworking.configure(player, new Configure(controllerPos, HoloSettings.DEFAULT, true)), "Another player's station cannot be reconfigured");
        station.owner = player.getUUID();
        level.setBlockAndUpdate(origin.offset(3, 0, 2), Blocks.AIR.defaultBlockState());
        test.assertTrue(controller.buildSnapshot().surface().width() * controller.buildSnapshot().surface().depth() == 9,
                "Removing panels recalculates the solid projection footprint");
        level.setBlockAndUpdate(stationPos, Blocks.AIR.defaultBlockState());
        test.assertTrue(controller.buildSnapshot().status().equals("no_station"), "Removing mission control stops the projection");
        test.assertTrue(system.createObjectData().equals(before), "Inspection never changes discovery or simulation state");
        player.containerMenu = player.inventoryMenu;
        test.succeed();
    }
}
