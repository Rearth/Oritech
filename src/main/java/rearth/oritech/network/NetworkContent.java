package rearth.oritech.network;

import io.wispforest.owo.network.OwoNetChannel;
import io.wispforest.owo.serialization.endec.ReflectiveEndecBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import rearth.oritech.Oritech;
import rearth.oritech.block.base.entity.MachineBlockEntity;
import rearth.oritech.block.entity.InventoryProxyAddonBlockEntity;
import rearth.oritech.init.recipes.OritechRecipe;
import rearth.oritech.init.recipes.OritechRecipeType;
import rearth.oritech.util.InventoryInputMode;

public class NetworkContent {

    public static final OwoNetChannel MACHINE_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "machine_data"));
    public static final OwoNetChannel UI_CHANNEL = OwoNetChannel.create(new Identifier(Oritech.MOD_ID, "ui_interactions"));

    // Server -> Client
    public record MachineSyncPacket(BlockPos position, long energy, long maxEnergy, long maxInsert, int progress, OritechRecipe activeRecipe, InventoryInputMode inputMode) {}

    // Client -> Server (e.g. from UI interactions
    public record InventoryInputModeSelectorPacket(BlockPos position) {}
    public record InventoryProxySlotSelectorPacket(BlockPos position, int slot) {}

    public static void registerChannels() {

        Oritech.LOGGER.info("Registering oritech channels");

        ReflectiveEndecBuilder.register(OritechRecipeType.ORI_RECIPE_ENDEC, OritechRecipe.class);

        MACHINE_CHANNEL.registerClientbound(MachineSyncPacket.class, ((message, access) -> {

            var entity = access.player().clientWorld.getBlockEntity(message.position);

            if (entity instanceof MachineBlockEntity machine) {
                machine.handleNetworkEntry(message);
            }

        }));

        UI_CHANNEL.registerServerbound(InventoryInputModeSelectorPacket.class, (message, access) -> {

            var entity = access.player().getWorld().getBlockEntity(message.position);

            if (entity instanceof MachineBlockEntity machine) {
                machine.cycleInputMode();
            }

        });
        
        UI_CHANNEL.registerServerbound(InventoryProxySlotSelectorPacket.class, (message, access) -> {
            
            var entity = access.player().getWorld().getBlockEntity(message.position);
            
            if (entity instanceof InventoryProxyAddonBlockEntity machine) {
                machine.setTargetSlot(message.slot);
                System.out.println(message.slot);
            }
            
        });

    }

}
