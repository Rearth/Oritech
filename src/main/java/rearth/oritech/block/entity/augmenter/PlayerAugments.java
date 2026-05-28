package rearth.oritech.block.entity.augmenter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import rearth.oritech.Oritech;
import rearth.oritech.block.entity.augmenter.api.Augment;
import rearth.oritech.init.AttachmentContent;
import rearth.oritech.init.datapack.AugmentContent;
import rearth.oritech.init.datapack.AugmentData;

import java.util.Map;
import java.util.WeakHashMap;

public class PlayerAugments {
    
    private static final Map<Identifier, Augment> AUGMENT_CACHE = new WeakHashMap<>();
    
    public static Map<Identifier, Augment> getAllAugments(RegistryAccess registryAccess) {
        
        if (AUGMENT_CACHE.isEmpty()) {
            var registry = registryAccess.lookupOrThrow(AugmentContent.AUGMENT_REGISTRY_KEY);
            registry.listElements().forEach((holder) -> AUGMENT_CACHE.put(holder.key().identifier(), holder.value().createAugment(holder.key().identifier())));
        }
        
        return AUGMENT_CACHE;
    }
    
    public static Augment getAugment(RegistryAccess registryAccess, Identifier id) {
        return getAllAugments(registryAccess).get(id);
    }
    
    public static AugmentData getAugmentData(RegistryAccess registryAccess, Identifier id) {
        var registry = registryAccess.lookupOrThrow(AugmentContent.AUGMENT_REGISTRY_KEY);
        return registry.getValue(id);
    }
    
    public static void serverTickAugments(ServerPlayer player) {
        var data = player.getData(AttachmentContent.ACTIVE_AUGMENTS);
        
        for (var augment : getAllAugments(player.registryAccess()).values()) {
            if (augment.isEnabled(data)) {
                if (player.level().getGameTime() % augment.refreshInterval() == 0)
                    augment.refreshServer(player);
            }
        }
    }
    
    // todo check if this needs to be called or can be removed?
    public static void refreshActiveAugments(ServerPlayer player) {
        var data = player.getData(AttachmentContent.ACTIVE_AUGMENTS);
        
        for (var augment : getAllAugments(player.registryAccess()).values()) {
            if (augment.isEnabled(data)) {
                augment.refreshServer(player);
            }
        }
    }
    
    public static void receiveInstallTrigger(AugmentInstallTriggerPacket packet, IPayloadContext context) {
        var player = context.player();
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            var operation = AugmentApplicatorOperation.values()[packet.operationId];
            switch (operation) {
                case RESEARCH -> modifierEntity.researchAugment(packet.id, player.isCreative(), player);
                case ADD -> modifierEntity.installAugmentToPlayer(packet.id, player.isCreative(), player);
                case REMOVE -> modifierEntity.removeAugmentFromPlayer(packet.id, player);
            }
        }
    }
    
    public static void receivePlayerLoadMachine(LoadPlayerAugmentsToMachinePacket packet, IPayloadContext context) {
        var player = context.player();
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity) {
            modifierEntity.loadResearchesFromPlayer(player);
        }
    }
    
    public static void receiveOpenAugmentScreen(OpenAugmentScreenPacket packet, IPayloadContext context) {
        var player = context.player();
        var entity = player.level().getBlockEntity(packet.position);
        
        if (entity instanceof AugmentApplicationEntity modifierEntity && player instanceof ServerPlayer serverPlayer) {
            modifierEntity.screenInvOverride = true;
            serverPlayer.openMenu(modifierEntity, modifierEntity.getPosForMultiblock());
        }
    }
    
    public static void receiveToggleAugment(AugmentPlayerTogglePacket packet, IPayloadContext context) {
        AugmentApplicationEntity.toggleAugmentForPlayer(packet.id, context.player());
    }
    
    public enum AugmentApplicatorOperation {
        RESEARCH, ADD, REMOVE, NONE, NEEDS_INIT
    }
    
    public record AugmentInstallTriggerPacket(BlockPos position, Identifier id,
                                              int operationId) implements CustomPacketPayload {
        
        public static final Type<AugmentInstallTriggerPacket> PACKET_ID = new Type<>(Oritech.id("aug_install"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record LoadPlayerAugmentsToMachinePacket(BlockPos position) implements CustomPacketPayload {
        
        public static final Type<LoadPlayerAugmentsToMachinePacket> PACKET_ID = new Type<>(Oritech.id("aug_loadtomachine"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record OpenAugmentScreenPacket(BlockPos position) implements CustomPacketPayload {
        
        public static final Type<OpenAugmentScreenPacket> PACKET_ID = new Type<>(Oritech.id("aug_openscreen"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
    
    public record AugmentPlayerTogglePacket(Identifier id) implements CustomPacketPayload {
        
        public static final Type<AugmentPlayerTogglePacket> PACKET_ID = new Type<>(Oritech.id("aug_toggle"));
        
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return PACKET_ID;
        }
    }
}
