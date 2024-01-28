package rearth.oritech.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.particles.ClientParticles;
import io.wispforest.owo.particles.systems.ParticleSystem;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.MachineBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.util.InventoryInputMode;

public class NetworkContent {

    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "machine_data"));
    public static final OwoNetChannel UI_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "ui_interactions"));

    // Server -> Client
    public record MachineSyncPacket(BlockPos position, long energy, int progress, OritechRecipe activeRecipe, InventoryInputMode inputMode) {}

    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) {}

    public static void registerChannels() {

        Oritech.LOGGER.info("Registering oritech channels");

        ReflectiveEndecBuilder.register(OritechRecipe.OritechRecipeType.ORI_RECIPE_ENDEC, OritechRecipe.class);

        MACHINE_CHANNEL.registerClientbound(MachineSyncPacket.class, ((message, access) -> {

            var entity = access.player().clientWorld.getBlockEntity(message.position);

            if (entity instanceof MachineBlockEntity machine) {
                machine.setProgress(message.progress);
                machine.getEnergyStorage().amount = message.energy;
                machine.setCurrentRecipe(message.activeRecipe);
                machine.setInventoryInputMode(message.inputMode);
            }

        }));

        UI_CHANNEL.registerServerbound(InventoryInputModeSelectorPacket.class, (message, access) -> {

            var entity = access.player().getWorld().getBlockEntity(message.position);

            if (entity instanceof MachineBlockEntity machine) {
                machine.cycleInputMode();
            }

        });

    }

}
