package rearth.oritech.block.entity.augmenter;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.HashMap;
import java.util.Map;

public class PlayerAugments {
    
    public static final Map<Identifier, Augment> allAugments = new HashMap<>();
    
    // this is called after recipe manager init / recipe reload
    public static void loadAllAugments(RecipeManager manager) {
        allAugments.clear();
        manager.listAllOfType(RecipeContent.AUGMENT_DATA).forEach(recipe -> allAugments.put(recipe.id(), recipe.value().createAugment(recipe.id())));
    }
    
    // called when a client connects to a server / changes world
    public static void refreshPlayerAugments(PlayerEntity player) {
        NetworkManager.sendPlayerHandle(new AugmentPlayerStatePacket(AttachmentApi.getAttachmentValue(player, Augment.ACTIVE_AUGMENTS_DATA)), (ServerPlayerEntity) player);
        for (var augment : PlayerAugments.allAugments.values()) {
            if (augment.isEnabled(player))
                augment.refreshServer(player);
        }
    }
    
    public static void serverTickAugments(ServerPlayerEntity player) {
        
        if (player.getWorld().getTime() % 80 == 0)
            refreshPlayerAugments(player);
        
        for (var augment : allAugments.values()) {
            if (augment.isEnabled(player)) {
                if (player.getServerWorld().getTime() % augment.refreshInterval() == 0)
                    augment.refreshServer(player);
            }
        }
    }
    
    public static void receiveInstallTrigger(AugmentInstallTriggerPacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        var entity = player.getWorld().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            var operation = PlayerAugments.AugmentApplicatorOperation.values()[packet.operationId];
            switch (operation) {
                case RESEARCH -> {
                    modifierEntity.researchAugment(packet.id, player.isCreative(), player);
                }
                case ADD -> {
                    modifierEntity.installAugmentToPlayer(packet.id, player);
                }
                case REMOVE -> {
                    modifierEntity.removeAugmentFromPlayer(packet.id, player);
                }
            }
        }
    }
    
    public static void receivePlayerLoadMachine(LoadPlayerAugmentsToMachinePacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        var entity = player.getWorld().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            modifierEntity.loadResearchesFromPlayer(player);
        }
    }
    
    public static void receiveOpenAugmentScreen(OpenAugmentScreenPacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        var entity = player.getWorld().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity && player instanceof ServerPlayerEntity serverPlayer) {
            modifierEntity.screenInvOverride = true;
            MenuRegistry.openExtendedMenu(serverPlayer, modifierEntity);
        }
    }
    
    public static void receiveToggleAugment(AugmentPlayerTogglePacket packet, PlayerEntity player, DynamicRegistryManager dynamicRegistryManager) {
        AugmentApplicationEntity.toggleAugmentForPlayer(packet.id, player);
    }
    
    
    public static void receiveAugmentState(PlayerAugments.AugmentPlayerStatePacket packet, World world, DynamicRegistryManager dynamicRegistryManager) {
        if (world.isClient)
            PlayerAugmentsClient.receiveAugmentState(packet.data);
    }
    
    public enum AugmentApplicatorOperation {
        RESEARCH, ADD, REMOVE, NONE, NEEDS_INIT
    }
    
    public record AugmentInstallTriggerPacket(BlockPos position, Identifier id, int operationId) implements CustomPayload {
        
        public static final CustomPayload.Id<AugmentInstallTriggerPacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("aug_install"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
    public record LoadPlayerAugmentsToMachinePacket(BlockPos position) implements CustomPayload {
        
        public static final CustomPayload.Id<LoadPlayerAugmentsToMachinePacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("aug_loadtomachine"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
    public record OpenAugmentScreenPacket(BlockPos position) implements CustomPayload {
        
        public static final CustomPayload.Id<OpenAugmentScreenPacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("aug_openscreen"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
    public record AugmentPlayerTogglePacket(Identifier id) implements CustomPayload {
        
        public static final CustomPayload.Id<AugmentPlayerTogglePacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("aug_toggle"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
    
    public record AugmentPlayerStatePacket(Map<Identifier, Augment.AugmentState> data) implements CustomPayload {
        
        public static final CustomPayload.Id<AugmentPlayerStatePacket> PACKET_ID = new CustomPayload.Id<>(Oritech.id("aug_state"));
        
        @Override
        public Id<? extends CustomPayload> getId() {
            return PACKET_ID;
        }
    }
}
