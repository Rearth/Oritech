package rearth.oritech.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.PulverizerBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;

public class NetworkContent {

    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "machine_data"));

    public record MachineSyncPacket(BlockPos position, long energy, int progress, OritechRecipe activeRecipe) {}

    public static void registerChannels() {

        Oritech.LOGGER.info("Registering oritech channels");

        ReflectiveEndecBuilder.register(OritechRecipe.ORITECH_ENDEC, OritechRecipe.class);

        MACHINE_CHANNEL.registerClientbound(MachineSyncPacket.class, ((message, access) -> {

            var entity = access.player().clientWorld.getBlockEntity(message.position);

            if (entity instanceof PulverizerBlockEntity machine) {
                machine.setProgress(message.progress);
                machine.getEnergyStorage().amount = message.energy;
                machine.setCurrentRecipe(message.activeRecipe);
            }

        }));

    }

}
