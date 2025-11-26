package rearth.oritech.block.entity.augmenter;

import dev.architectury.registry.menu.MenuRegistry;
import rearth.oritech.Oritech;
import rearth.oritech.api.attachment.AttachmentApi;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.init.recipes.RecipeContent;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PlayerAugments {
    
    public static final Map<ResourceLocation, Augment> allAugments = new HashMap<>();
    
    // this is called after recipe manager init / recipe reload
    public static void loadAllAugments(RecipeManager manager) {
        allAugments.clear();
        manager.getAllRecipesFor(RecipeContent.AUGMENT_DATA).forEach(recipe -> allAugments.put(recipe.id(), recipe.value().createAugment(recipe.id())));
    }
    
    public static void serverTickAugments(ServerPlayer player) {
        
        for (var augment : allAugments.values()) {
            var data = AttachmentApi.getAttachmentValue(player, Augment.ACTIVE_AUGMENTS_DATA);
            if (augment.isEnabled(data)) {
                if (player.serverLevel().getGameTime() % augment.refreshInterval() == 0)
                    augment.refreshServer(player);
            }
        }
    }
    
    public static void receiveInstallTrigger(AugmentInstallTriggerPacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        var entity = player.level().getBlockEntity(packet.position);
        
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
    
    public static void receivePlayerLoadMachine(LoadPlayerAugmentsToMachinePacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            modifierEntity.loadResearchesFromPlayer(player);
        }
    }
    
    public static void receiveOpenAugmentScreen(OpenAugmentScreenPacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity && player instanceof ServerPlayer serverPlayer) {
            modifierEntity.screenInvOverride = true;
            MenuRegistry.openExtendedMenu(serverPlayer, modifierEntity);
        }
    }
    
    public static void receiveToggleAugment(AugmentPlayerTogglePacket packet, Player player, RegistryAccess dynamicRegistryManager) {
        AugmentApplicationEntity.toggleAugmentForPlayer(packet.id, player);
    }
    
    public enum AugmentApplicatorOperation {
        RESEARCH, ADD, REMOVE, NONE, NEEDS_INIT
    }
    
    public record AugmentInstallTriggerPacket(BlockPos position, ResourceLocation id, int operationId) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<AugmentInstallTriggerPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("aug_install"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record LoadPlayerAugmentsToMachinePacket(BlockPos position) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<LoadPlayerAugmentsToMachinePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("aug_loadtomachine"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record OpenAugmentScreenPacket(BlockPos position) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<OpenAugmentScreenPacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("aug_openscreen"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record AugmentPlayerTogglePacket(ResourceLocation id) implements CustomPacketPayload {
        
        public static final CustomPacketPayload.Type<AugmentPlayerTogglePacket> PACKET_ID = new CustomPacketPayload.Type<>(Oritech.id("aug_toggle"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
