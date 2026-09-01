package rearth.oritech.spaceage.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import rearth.oritech.spaceage.block.assembler.RocketAssemblerMenu;
import rearth.oritech.spaceage.simulation.ActiveRocketData;

public final class RocketAssemblerClientController {

    private RocketAssemblerClientController() {
    }

    public static void receivePreview(BlockPos position, @Nullable ActiveRocketData rocket) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RocketAssemblerMenu menu
                && menu.blockPos.equals(position)) {
            menu.setPreview(rocket);
        }
    }
}
